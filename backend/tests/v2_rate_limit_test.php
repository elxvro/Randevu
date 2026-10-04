<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/RateLimiter.php')) { fwrite(STDERR, "FAIL: RateLimiter.php missing\n"); exit(1); }
require_once __DIR__.'/../src/RateLimiter.php';

function rate_expect(bool $value, string $message): void { if (!$value) { fwrite(STDERR, "FAIL: {$message}\n"); exit(1); } }
$key1 = RateLimiter::key('login', '1.2.3.4', ' TEST@Example.COM ');
$key2 = RateLimiter::key('login', '1.2.3.4', 'test@example.com');
rate_expect($key1 === $key2 && preg_match('/^[a-f0-9]{64}$/', $key1) === 1, 'normalized deterministic key');
rate_expect(!RateLimiter::blockedCount(9, 10), 'nine attempts allowed');
rate_expect(RateLimiter::blockedCount(10, 10), 'ten attempts blocked');
rate_expect(RateLimiter::windowStart(new DateTimeImmutable('2026-10-04 12:00:00', new DateTimeZone('UTC')), 15)->format('Y-m-d H:i:s') === '2026-10-04 11:45:00', '15 minute window');

echo "v2_rate_limit_test: OK\n";
