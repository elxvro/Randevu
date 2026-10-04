<?php

declare(strict_types=1);

final class Auth
{
    public static function extractBearer(?string $header): ?string
    {
        if ($header === null || !preg_match('/^Bearer\s+(.+)$/i', trim($header), $match)) {
            return null;
        }
        $token = trim($match[1]);
        return $token === '' ? null : $token;
    }

    public static function requireBusiness(PDO $pdo): array
    {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null;
        $token = self::extractBearer(is_string($header) ? $header : null);
        if ($token === null) {
            throw new RuntimeException('unauthorized');
        }
        $stmt = $pdo->prepare(
            'SELECT u.id AS user_id, u.business_id, u.name, u.phone FROM api_tokens t JOIN users u ON u.id=t.user_id WHERE t.token_hash=:hash AND t.expires_at>UTC_TIMESTAMP() AND u.business_id IS NOT NULL LIMIT 1'
        );
        $stmt->execute(['hash' => hash('sha256', $token)]);
        $row = $stmt->fetch();
        if (!$row) {
            throw new RuntimeException('unauthorized');
        }
        return $row;
    }

    public static function bootstrap(PDO $pdo, array $config, array $payload): array
    {
        $configured = (string) ($config['app_setup_key'] ?? '');
        $supplied = (string) ($payload['setup_key'] ?? '');
        if ($configured === '' || !hash_equals($configured, $supplied)) {
            throw new RuntimeException('invalid_setup_key');
        }
        $businessName = trim((string) ($payload['business_name'] ?? ''));
        $ownerName = trim((string) ($payload['owner_name'] ?? ''));
        $phone = Phone::normalize((string) ($payload['phone'] ?? ''));
        $timezone = trim((string) ($payload['timezone'] ?? 'Europe/Istanbul'));
        if ($businessName === '' || $ownerName === '' || $phone === null) {
            throw new RuntimeException('invalid_bootstrap_payload');
        }
        try { new DateTimeZone($timezone); } catch (Throwable) { throw new RuntimeException('invalid_timezone'); }

        $pdo->beginTransaction();
        try {
            $slug = Slug::unique($pdo, $businessName);
            $stmt = $pdo->prepare('INSERT INTO businesses(name,slug,phone,address,timezone) VALUES(:name,:slug,:phone,:address,:timezone)');
            $stmt->execute(['name'=>$businessName,'slug'=>$slug,'phone'=>$phone,'address'=>trim((string)($payload['address'] ?? '')),'timezone'=>$timezone]);
            $businessId = (int) $pdo->lastInsertId();
            $stmt = $pdo->prepare("INSERT INTO users(business_id,role,name,phone) VALUES(:business,'business',:name,:phone)");
            $stmt->execute(['business'=>$businessId,'name'=>$ownerName,'phone'=>$phone]);
            $userId = (int) $pdo->lastInsertId();
            $plain = bin2hex(random_bytes(32));
            $ttl = max(1, (int) ($config['token_ttl_hours'] ?? 720));
            $expires = (new DateTimeImmutable('now', new DateTimeZone('UTC')))->modify("+{$ttl} hours")->format('Y-m-d H:i:s');
            $stmt = $pdo->prepare('INSERT INTO api_tokens(user_id,token_hash,expires_at) VALUES(:user,:hash,:expires)');
            $stmt->execute(['user'=>$userId,'hash'=>hash('sha256',$plain),'expires'=>$expires]);
            $stmt = $pdo->prepare('INSERT INTO business_settings(business_id) VALUES(:business)');
            $stmt->execute(['business'=>$businessId]);
            $pdo->commit();
            return ['business_id'=>$businessId,'user_id'=>$userId,'token'=>$plain,'expires_at'=>$expires];
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            throw $e;
        }
    }
}
