<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/AvailabilityService.php')) { fwrite(STDERR, "FAIL: AvailabilityService.php missing\n"); exit(1); }
require_once __DIR__.'/../src/AvailabilityService.php';

function avail_expect(array $expected,array $actual,string $case): void {
    if ($expected!==$actual) { fwrite(STDERR,"FAIL {$case}: ".json_encode($actual)."\n"); exit(1); }
}
$business=['timezone'=>'Europe/Istanbul','opening_time'=>'09:00:00','closing_time'=>'10:00:00','active'=>1];
$service=['duration_minutes'=>30,'active'=>1];
$staff=['id'=>9,'active'=>1,'public_booking'=>1];
avail_expect(['09:00','09:15','09:30'], AvailabilityService::slots($business,$service,$staff,[],[],'2026-10-05'), 'base slots');

$leaves=[['staff_id'=>9,'start_date'=>'2026-10-05','end_date'=>'2026-10-05','start_time'=>'09:15:00','end_time'=>'09:45:00']];
avail_expect([], AvailabilityService::slots($business,$service,$staff,$leaves,[],'2026-10-05'), 'partial leave');

$appointments=[['staff_id'=>9,'starts_at'=>'2026-10-05 09:15:00','ends_at'=>'2026-10-05 09:45:00','status'=>'confirmed']];
avail_expect([], AvailabilityService::slots($business,$service,$staff,[],$appointments,'2026-10-05'), 'confirmed overlap');

$appointments[0]['status']='cancelled';
avail_expect(['09:00','09:15','09:30'], AvailabilityService::slots($business,$service,$staff,[],$appointments,'2026-10-05'), 'cancelled ignored');

$bad=$business; $bad['closing_time']='08:00:00';
avail_expect([], AvailabilityService::slots($bad,$service,$staff,[],[],'2026-10-05'), 'invalid overnight');
$inactive=$staff; $inactive['active']=0;
avail_expect([], AvailabilityService::slots($business,$service,$inactive,[],[],'2026-10-05'), 'inactive staff');

echo "v2_availability_test: OK\n";
