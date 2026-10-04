<?php

declare(strict_types=1);

$schemaPath = __DIR__ . '/../schema.sql';
$migrationPath = __DIR__ . '/../migrations/2026_10_04_v2_core.sql';
$typesPath = __DIR__ . '/../src/V2Types.php';
$slugPath = __DIR__ . '/../src/Slug.php';

function v2_expect(bool $condition, string $message): void {
    if (!$condition) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
}

v2_expect(file_exists($migrationPath), 'v2 migration file missing');
v2_expect(file_exists($typesPath), 'V2Types.php missing');
v2_expect(file_exists($slugPath), 'Slug.php missing');

$schema = file_get_contents($schemaPath);
$migration = file_get_contents($migrationPath);

foreach ([
    'slug', 'opening_time', 'closing_time', 'active',
    'email', "'owner'", 'external_id', 'version', 'staff_leaves',
    "ENUM('android','public_web','system')", 'rate_limit_events'
] as $needle) {
    v2_expect(str_contains($schema, $needle), "schema missing {$needle}");
    v2_expect(str_contains($migration, $needle), "migration missing {$needle}");
}

v2_expect(
    preg_match('/UNIQUE\s+KEY[^\n]*slug/i', $schema) === 1,
    'business slug must be unique'
);
v2_expect(
    preg_match('/UNIQUE\s+KEY[^\n]*email/i', $schema) === 1,
    'owner email must be unique'
);

require_once $typesPath;
foreach ([
    'validation_error',
    'unauthorized',
    'email_in_use',
    'invalid_credentials',
    'tenant_mismatch',
    'not_found',
    'version_conflict',
    'slot_unavailable',
    'rate_limited',
    'server_error'
] as $code) {
    v2_expect(in_array($code, V2ApiError::all(), true), "missing error code {$code}");
}

echo "v2_schema_contract_test: OK\n";
