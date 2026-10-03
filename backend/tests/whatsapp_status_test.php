<?php

declare(strict_types=1);

require_once __DIR__ . '/../src/WhatsAppStatusService.php';

function state_is(string $expected, array $actual, string $message): void {
    if (($actual['state'] ?? null) !== $expected) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
    if (array_key_exists('access_token', $actual) || str_contains(json_encode($actual), 'SECRET_TOKEN')) {
        fwrite(STDERR, "FAIL: secret leaked\n");
        exit(1);
    }
}

$base = ['enabled' => true, 'template_name' => 'appointment_reminder', 'template_language' => 'tr'];
$config = ['meta_access_token' => 'SECRET_TOKEN', 'meta_phone_number_id' => '123', 'meta_graph_version' => 'v23.0'];
state_is('disabled', WhatsAppStatusService::project($base + ['enabled' => false], $config, null), 'disabled');
state_is('incomplete', WhatsAppStatusService::project($base, $config + ['meta_phone_number_id' => ''], null), 'missing phone id');
state_is('incomplete', WhatsAppStatusService::project($base, $config + ['meta_access_token' => ''], null), 'missing token');
state_is('unreachable', WhatsAppStatusService::project($base, $config, 'timeout'), 'probe failure');
state_is('connected', WhatsAppStatusService::project($base, $config, null), 'connected');

echo "whatsapp_status_test: OK\n";
