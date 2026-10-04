<?php
declare(strict_types=1);

$path=__DIR__.'/../src/V2AppointmentRepository.php';
if (!file_exists($path)) { fwrite(STDERR,"FAIL: V2AppointmentRepository.php missing\n"); exit(1); }
$src=file_get_contents($path);
foreach (['beginTransaction(', 'FOR UPDATE', 'starts_at < :ends', 'ends_at > :starts', 'rollBack(', 'slot_unavailable'] as $needle) {
    if (!str_contains($src,$needle)) { fwrite(STDERR,"FAIL: reservation contract missing {$needle}\n"); exit(1); }
}
echo "v2_booking_lock_contract_test: OK\n";
