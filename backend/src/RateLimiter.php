<?php

declare(strict_types=1);

final class RateLimiter
{
    public function __construct(private PDO $pdo) {}

    public static function normalizeIdentifier(string $identifier): string
    {
        return strtolower(trim($identifier));
    }

    public static function key(string $action, string $ip, string $identifier): string
    {
        return hash('sha256', trim($action) . '|' . trim($ip) . '|' . self::normalizeIdentifier($identifier));
    }

    public static function blockedCount(int $count, int $limit): bool
    {
        return $count >= max(1, $limit);
    }

    public static function windowStart(DateTimeImmutable $nowUtc, int $minutes): DateTimeImmutable
    {
        return $nowUtc->modify('-' . max(1, $minutes) . ' minutes');
    }

    public function assertAllowed(string $action, string $ip, string $identifier, int $limit, int $windowMinutes): void
    {
        $now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
        $stmt = $this->pdo->prepare(
            'SELECT COUNT(*) FROM rate_limit_events WHERE action=:action AND key_hash=:key AND occurred_at>=:since'
        );
        $stmt->execute([
            'action' => $action,
            'key' => self::key($action, $ip, $identifier),
            'since' => self::windowStart($now, $windowMinutes)->format('Y-m-d H:i:s'),
        ]);
        if (self::blockedCount((int)$stmt->fetchColumn(), $limit)) {
            throw new RuntimeException(V2ApiError::RATE_LIMITED);
        }
    }

    public function record(string $action, string $ip, string $identifier): void
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO rate_limit_events(action,key_hash,occurred_at) VALUES(:action,:key,UTC_TIMESTAMP())'
        );
        $stmt->execute([
            'action' => $action,
            'key' => self::key($action, $ip, $identifier),
        ]);
    }

    public function consume(string $action, string $ip, string $identifier, int $limit, int $windowMinutes): void
    {
        $this->assertAllowed($action, $ip, $identifier, $limit, $windowMinutes);
        $this->record($action, $ip, $identifier);
    }
}
