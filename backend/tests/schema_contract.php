<?php

declare(strict_types=1);

$schema = file_get_contents(__DIR__ . '/../schema.sql');
$config = file_get_contents(__DIR__ . '/../config.example.php');

function expect_true(bool $value, string $message): void {
    if (!$value) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
}

foreach (['business_settings', 'whatsapp_reminder_queue', 'appointment_external_id', 'offset_minutes', 'scheduled_at_utc', 'meta_message_id', 'next_attempt_at'] as $needle) {
    expect_true(str_contains($schema, $needle), "schema missing {$needle}");
}
expect_true(str_contains($schema, 'UNIQUE KEY') && str_contains($schema, 'appointment_external_id') && str_contains($schema, 'offset_minutes'), 'queue unique key missing');
foreach (['META_ACCESS_TOKEN', 'META_PHONE_NUMBER_ID', 'META_GRAPH_VERSION'] as $needle) {
    expect_true(str_contains($config, $needle), "config missing {$needle}");
}
expect_true(!preg_match('/EAA[A-Za-z0-9]{30,}/', $config), 'config must not contain a production-looking Meta token');

echo "schema_contract: OK\n";
