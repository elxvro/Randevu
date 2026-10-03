<?php

declare(strict_types=1);

require_once __DIR__ . '/../src/Phone.php';
require_once __DIR__ . '/../src/ReminderSchedule.php';

function check(bool $ok, string $message): void {
    if (!$ok) { fwrite(STDERR, "FAIL: {$message}\n"); exit(1); }
}

$appointment = [
    'id' => 'apt-1',
    'customer_name' => 'Ayşe',
    'customer_phone' => '+90 555 111 22 33',
    'date' => '2026-10-05',
    'time' => '10:00',
    'service' => 'Bakım',
    'staff' => 'Deniz',
    'status' => 'confirmed',
];
$settings = [
    'enabled' => true,
    'reminder_24h' => true,
    'reminder_2h' => true,
    'timezone' => 'Europe/Istanbul',
    'business_name' => 'Studio',
    'template_name' => 'appointment_reminder',
    'template_language' => 'tr',
];

$rows = ReminderSchedule::build($appointment, $settings, new DateTimeImmutable('2026-10-04 06:00:00', new DateTimeZone('UTC')));
check(count($rows) === 2, 'both offsets expected');
check(array_column($rows, 'offset_minutes') === [1440, 120], 'offset order');
check($rows[0]['unique_key'] === 'apt-1:1440' && $rows[1]['unique_key'] === 'apt-1:120', 'deterministic keys');
check($rows[0]['recipient_phone'] === '905551112233', 'phone normalization');

$later = ReminderSchedule::build($appointment, $settings, new DateTimeImmutable('2026-10-04 08:00:00', new DateTimeZone('UTC')));
check(count($later) === 1 && $later[0]['offset_minutes'] === 120, 'past 24h trigger skipped independently');
check(ReminderSchedule::build(array_merge($appointment, ['status' => 'cancelled']), $settings, new DateTimeImmutable('2026-10-04 06:00:00', new DateTimeZone('UTC'))) === [], 'cancelled yields none');
check(ReminderSchedule::build(array_merge($appointment, ['status' => 'completed']), $settings, new DateTimeImmutable('2026-10-04 06:00:00', new DateTimeZone('UTC'))) === [], 'completed yields none');
check(ReminderSchedule::build(array_merge($appointment, ['customer_phone' => 'bad']), $settings, new DateTimeImmutable('2026-10-04 06:00:00', new DateTimeZone('UTC'))) === [], 'bad phone yields none');
check(ReminderSchedule::build($appointment, array_merge($settings, ['timezone' => 'Bad/Zone']), new DateTimeImmutable('2026-10-04 06:00:00', new DateTimeZone('UTC'))) === [], 'bad timezone yields none');

echo "reminder_schedule_test: OK\n";
