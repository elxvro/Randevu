<?php
declare(strict_types=1);

foreach (['V2AppointmentService.php','V2AppointmentRepository.php'] as $name) {
    if (!file_exists(__DIR__.'/../src/'.$name)) { fwrite(STDERR, "FAIL: {$name} missing\n"); exit(1); }
}
require_once __DIR__.'/../src/V2Types.php';
require_once __DIR__.'/../src/V2AppointmentService.php';

final class AppointmentStoreFake implements V2AppointmentStore {
    public array $created=[];
    public function list(int $businessId,string $from,string $to): array { return []; }
    public function create(int $businessId,array $payload,string $source): array {
        $this->created=[$businessId,$payload,$source];
        return ['external_id'=>$payload['external_id'],'version'=>1,'status'=>'pending'];
    }
    public function update(int $businessId,string $externalId,array $payload,int $expectedVersion): array {
        V2Version::guard($expectedVersion,2,['external_id'=>$externalId,'version'=>2]);
        return ['external_id'=>$externalId,'version'=>3];
    }
    public function cancel(int $businessId,string $externalId,int $expectedVersion): array {
        V2Version::guard($expectedVersion,2,['external_id'=>$externalId,'version'=>2,'status'=>'pending']);
        return ['external_id'=>$externalId,'version'=>3,'status'=>'cancelled'];
    }
}
final class ReminderFake implements V2ReminderScheduler {
    public bool $fail=false;
    public function schedule(int $businessId,array $appointment): string {
        if ($this->fail) throw new RuntimeException('queue_failed');
        return 'scheduled';
    }
    public function cancel(int $businessId,string $externalId): string { return 'cancelled'; }
}
$store=new AppointmentStoreFake(); $rem=new ReminderFake(); $svc=new V2AppointmentService($store,$rem);
$r=$svc->create(5,['external_id'=>'a-1'],'public_web');
if ($store->created[0]!==5 || $store->created[2]!=='public_web' || $r['reminder_state']!=='scheduled') { fwrite(STDERR,"FAIL: create/source/reminder\n"); exit(1); }
$rem->fail=true; $r=$svc->create(5,['external_id'=>'a-2'],'android');
if ($r['reminder_state']!=='needs_attention') { fwrite(STDERR,"FAIL: reminder failure must not rollback appointment\n"); exit(1); }
try { $svc->update(5,'a-1',[],1); fwrite(STDERR,"FAIL: stale update accepted\n"); exit(1); }
catch (V2VersionConflict $e) {}

echo "v2_appointment_service_test: OK\n";
