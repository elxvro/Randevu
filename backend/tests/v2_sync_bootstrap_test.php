<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/V2SyncService.php')) { fwrite(STDERR,"FAIL: V2SyncService.php missing\n"); exit(1); }
require_once __DIR__.'/../src/V2SyncService.php';

final class SyncStoreFake implements V2SyncStore {
    public array $calls=[];
    public function business(int $businessId): array { $this->calls[]=['business',$businessId]; return ['id'=>$businessId,'version'=>1]; }
    public function services(int $businessId): array { $this->calls[]=['services',$businessId]; return [['external_id'=>'s','active'=>0,'version'=>2]]; }
    public function staff(int $businessId): array { $this->calls[]=['staff',$businessId]; return [['external_id'=>'p','active'=>0,'version'=>2]]; }
    public function leaves(int $businessId,string $from,string $to): array { $this->calls[]=['leaves',$businessId,$from,$to]; return []; }
    public function appointments(int $businessId,string $from,string $to): array { $this->calls[]=['appointments',$businessId,$from,$to]; return []; }
}
$store=new SyncStoreFake(); $svc=new V2SyncService($store);
$now=new DateTimeImmutable('2026-10-04 12:00:00', new DateTimeZone('UTC'));
$out=$svc->bootstrap(8,$now,'Europe/Istanbul');
if ($out['services'][0]['active']!==0 || $out['staff'][0]['active']!==0) { fwrite(STDERR,"FAIL: inactive cache rows omitted\n"); exit(1); }
if ($out['server_time']!=='2026-10-04T12:00:00+00:00') { fwrite(STDERR,"FAIL: server time\n"); exit(1); }
foreach ($store->calls as $call) if ($call[1]!==8) { fwrite(STDERR,"FAIL: tenant scope\n"); exit(1); }
$appt=array_values(array_filter($store->calls,fn($c)=>$c[0]==='appointments'))[0];
if ($appt[2]!=='2026-07-06' || $appt[3]!=='2027-10-04') { fwrite(STDERR,"FAIL: bootstrap window ".json_encode($appt)."\n"); exit(1); }

echo "v2_sync_bootstrap_test: OK\n";
