<?php
declare(strict_types=1);

function auth_fail(string $message): never { fwrite(STDERR, "FAIL: {$message}\n"); exit(1); }
foreach ([__DIR__.'/../src/V2Auth.php', __DIR__.'/../src/RateLimiter.php'] as $file) {
    if (!file_exists($file)) auth_fail(basename($file) . ' missing');
}
require_once __DIR__.'/../src/V2Types.php';
require_once __DIR__.'/../src/V2Auth.php';
require_once __DIR__.'/../src/RateLimiter.php';

if (V2Auth::normalizeEmail('  TEST@Example.COM ') !== 'test@example.com') auth_fail('email normalization');
$valid = V2Auth::validateRegistration([
    'email'=>'owner@example.com','password'=>'12345678','business_name'=>'  Salon  ','owner_name'=>'  Emre  ',
    'timezone'=>'Europe/Istanbul'
]);
if ($valid['business_name'] !== 'Salon' || $valid['owner_name'] !== 'Emre') auth_fail('registration trimming');

$hash = V2Auth::hashPassword('12345678');
if ($hash === '12345678' || !password_verify('12345678', $hash)) auth_fail('password hashing');

$now = new DateTimeImmutable('2026-10-04 12:00:00', new DateTimeZone('UTC'));
$token = V2Auth::tokenMaterial($now);
if (!preg_match('/^[a-f0-9]{64}$/', $token['token'])) auth_fail('token format');
if ($token['token_hash'] !== hash('sha256', $token['token'])) auth_fail('token hash');
if ($token['expires_at'] !== '2026-11-03 12:00:00') auth_fail('30 day expiry');

foreach ([
    ['email'=>'bad','password'=>'12345678','business_name'=>'Salon','owner_name'=>'Emre'],
    ['email'=>'a@b.com','password'=>'123','business_name'=>'Salon','owner_name'=>'Emre'],
] as $payload) {
    try { V2Auth::validateRegistration($payload); auth_fail('invalid registration accepted'); }
    catch (RuntimeException $e) { if ($e->getMessage() !== V2ApiError::VALIDATION_ERROR) auth_fail('wrong validation code'); }
}

$source = file_get_contents(__DIR__.'/../src/V2Auth.php');
foreach (['password_hash(', 'password_verify(', 'random_bytes(32)', "hash('sha256'"] as $needle) {
    if (!str_contains($source, $needle)) auth_fail("auth source missing {$needle}");
}

echo "v2_auth_test: OK\n";
