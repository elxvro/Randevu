<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/PublicBookingService.php')) { fwrite(STDERR,"FAIL: PublicBookingService.php missing\n"); exit(1); }
require_once __DIR__.'/../src/PublicBookingService.php';

$catalog=PublicBookingService::redactCatalog([
  'business'=>['id'=>7,'name'=>'Salon','slug'=>'salon','phone'=>'123','address'=>'A','timezone'=>'Europe/Istanbul','password_hash'=>'x'],
  'services'=>[
    ['id'=>1,'external_id'=>'s1','name'=>'Kesim','duration_minutes'=>30,'price'=>'100.00','active'=>1,'version'=>4],
    ['id'=>2,'external_id'=>'s2','name'=>'Kapalı','duration_minutes'=>30,'active'=>0],
  ],
  'staff'=>[
    ['id'=>3,'external_id'=>'p1','name'=>'Deniz','phone'=>'secret','active'=>1,'public_booking'=>1,'version'=>2],
    ['id'=>4,'external_id'=>'p2','name'=>'Gizli','phone'=>'secret','active'=>1,'public_booking'=>0],
  ],
]);
$json=json_encode($catalog);
foreach (['password_hash','"phone":"secret"','"id":7','"version"'] as $forbidden) {
    if (str_contains($json,$forbidden)) { fwrite(STDERR,"FAIL: public leak {$forbidden}\n"); exit(1); }
}
if (count($catalog['services'])!==1 || count($catalog['staff'])!==1) { fwrite(STDERR,"FAIL: inactive/private resources exposed\n"); exit(1); }

echo "v2_public_redaction_test: OK\n";
