<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/PublicBookingService.php')) { fwrite(STDERR,"FAIL: PublicBookingService.php missing\n"); exit(1); }
require_once __DIR__.'/../src/V2Types.php';
require_once __DIR__.'/../src/Phone.php';
require_once __DIR__.'/../src/PublicBookingService.php';

$clean=PublicBookingService::validateBookingPayload([
    'slug'=>' salon ','service'=>'11111111-1111-1111-1111-111111111111','staff'=>'22222222-2222-2222-2222-222222222222',
    'date'=>'2026-10-05','time'=>'09:15','customer_name'=>' Ayşe ','customer_phone'=>'0555 111 22 33'
]);
if ($clean['slug']!=='salon' || $clean['customer_name']!=='Ayşe' || $clean['customer_phone']!=='905551112233') { fwrite(STDERR,"FAIL: booking normalization\n"); exit(1); }
try { PublicBookingService::validateBookingPayload(['slug'=>'x']); fwrite(STDERR,"FAIL: malformed booking accepted\n"); exit(1); }
catch (RuntimeException $e) { if ($e->getMessage()!==V2ApiError::VALIDATION_ERROR) { fwrite(STDERR,"FAIL: wrong validation error\n"); exit(1); } }

echo "v2_public_booking_test: OK\n";
