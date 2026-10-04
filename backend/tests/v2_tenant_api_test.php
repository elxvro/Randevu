<?php
declare(strict_types=1);

foreach (['V2TenantService.php','V2BusinessRepository.php','V2CatalogRepository.php','V2StaffRepository.php'] as $name) {
    if (!file_exists(__DIR__.'/../src/'.$name)) { fwrite(STDERR, "FAIL: {$name} missing\n"); exit(1); }
}
require_once __DIR__.'/../src/V2Types.php';
require_once __DIR__.'/../src/V2TenantService.php';

final class TenantBusinessFake implements V2BusinessStore {
    public int $seenBusiness=0;
    public function get(int $businessId): array { $this->seenBusiness=$businessId; return ['id'=>$businessId,'version'=>2]; }
    public function update(int $businessId, array $input, int $expectedVersion): array {
        $this->seenBusiness=$businessId;
        if (array_key_exists('business_id',$input)) { fwrite(STDERR, "FAIL: payload business_id leaked\n"); exit(1); }
        return ['id'=>$businessId,'version'=>$expectedVersion+1,'name'=>$input['business_name'] ?? ''];
    }
}
final class TenantCatalogFake implements V2CatalogStore {
    public int $seenBusiness=0;
    public function listServices(int $businessId): array { $this->seenBusiness=$businessId; return []; }
    public function createService(int $businessId,array $input): array { $this->seenBusiness=$businessId; return ['external_id'=>$input['external_id'],'version'=>1]; }
    public function updateService(int $businessId,string $externalId,array $input,int $expectedVersion): array { $this->seenBusiness=$businessId; return ['external_id'=>$externalId,'version'=>$expectedVersion+1]; }
    public function deleteService(int $businessId,string $externalId,int $expectedVersion): void { $this->seenBusiness=$businessId; }
}
final class TenantStaffFake implements V2StaffStore {
    public int $seenBusiness=0;
    public function listStaff(int $businessId): array { $this->seenBusiness=$businessId; return []; }
    public function createStaff(int $businessId,array $input): array { $this->seenBusiness=$businessId; return ['external_id'=>$input['external_id'],'version'=>1]; }
    public function updateStaff(int $businessId,string $externalId,array $input,int $expectedVersion): array { $this->seenBusiness=$businessId; return ['external_id'=>$externalId,'version'=>$expectedVersion+1]; }
    public function deleteStaff(int $businessId,string $externalId,int $expectedVersion): void { $this->seenBusiness=$businessId; }
    public function listLeaves(int $businessId): array { $this->seenBusiness=$businessId; return []; }
    public function createLeave(int $businessId,array $input): array { $this->seenBusiness=$businessId; return ['external_id'=>$input['external_id'],'version'=>1]; }
    public function updateLeave(int $businessId,string $externalId,array $input,int $expectedVersion): array { $this->seenBusiness=$businessId; return ['external_id'=>$externalId,'version'=>$expectedVersion+1]; }
    public function deleteLeave(int $businessId,string $externalId,int $expectedVersion): void { $this->seenBusiness=$businessId; }
}
$b=new TenantBusinessFake(); $c=new TenantCatalogFake(); $s=new TenantStaffFake();
$svc=new V2TenantService($b,$c,$s);
$updated=$svc->updateBusiness(7,['business_id'=>999,'business_name'=>'X'],2);
if ($b->seenBusiness!==7 || $updated['id']!==7) { fwrite(STDERR,"FAIL: tenant override\n"); exit(1); }
$svc->listServices(7); $svc->listStaff(7); $svc->listLeaves(7);
if ($c->seenBusiness!==7 || $s->seenBusiness!==7) { fwrite(STDERR,"FAIL: tenant scope not forwarded\n"); exit(1); }

echo "v2_tenant_api_test: OK\n";
