<?php

declare(strict_types=1);

final class V2Auth
{
    public static function normalizeEmail(string $email): string
    {
        return strtolower(trim($email));
    }

    public static function validateRegistration(array $input): array
    {
        $email = self::normalizeEmail((string)($input['email'] ?? ''));
        $password = (string)($input['password'] ?? '');
        $businessName = trim((string)($input['business_name'] ?? ''));
        $ownerName = trim((string)($input['owner_name'] ?? ''));
        $timezone = trim((string)($input['timezone'] ?? 'Europe/Istanbul')) ?: 'Europe/Istanbul';
        if (
            filter_var($email, FILTER_VALIDATE_EMAIL) === false ||
            strlen($password) < 8 ||
            strlen($businessName) < 2 || strlen($businessName) > 160 ||
            strlen($ownerName) < 2 || strlen($ownerName) > 160
        ) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        try {
            new DateTimeZone($timezone);
        } catch (Throwable) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }

        $rawPhone = trim((string)($input['phone'] ?? ''));
        $phone = null;
        if ($rawPhone !== '') {
            $phone = Phone::normalize($rawPhone);
            if ($phone === null) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }

        return [
            'email' => $email,
            'password' => $password,
            'business_name' => $businessName,
            'owner_name' => $ownerName,
            'phone' => $phone,
            'address' => trim((string)($input['address'] ?? '')),
            'timezone' => $timezone,
        ];
    }

    public static function hashPassword(string $password): string
    {
        $hash = password_hash($password, PASSWORD_DEFAULT);
        if (!is_string($hash) || $hash === '') throw new RuntimeException(V2ApiError::SERVER_ERROR);
        return $hash;
    }

    public static function tokenMaterial(?DateTimeImmutable $nowUtc = null): array
    {
        $nowUtc ??= new DateTimeImmutable('now', new DateTimeZone('UTC'));
        $plain = bin2hex(random_bytes(32));
        return [
            'token' => $plain,
            'token_hash' => hash('sha256', $plain),
            'expires_at' => $nowUtc->modify('+30 days')->format('Y-m-d H:i:s'),
        ];
    }

    public static function registerOwner(PDO $pdo, RateLimiter $limits, array $input, string $ip): array
    {
        $data = self::validateRegistration($input);
        $limits->consume('register_owner', $ip, $data['email'], 10, 15);

        $check = $pdo->prepare('SELECT 1 FROM users WHERE email=:email LIMIT 1');
        $check->execute(['email'=>$data['email']]);
        if ($check->fetchColumn() !== false) throw new RuntimeException(V2ApiError::EMAIL_IN_USE);

        $pdo->beginTransaction();
        try {
            $slug = Slug::unique($pdo, $data['business_name']);
            $stmt = $pdo->prepare(
                'INSERT INTO businesses(name,slug,phone,address,timezone) VALUES(:name,:slug,:phone,:address,:timezone)'
            );
            $stmt->execute([
                'name'=>$data['business_name'],'slug'=>$slug,'phone'=>$data['phone'],
                'address'=>$data['address'],'timezone'=>$data['timezone'],
            ]);
            $businessId = (int)$pdo->lastInsertId();

            $stmt = $pdo->prepare(
                "INSERT INTO users(business_id,role,name,email,phone,password_hash) VALUES(:business,'owner',:name,:email,:phone,:password)"
            );
            $stmt->execute([
                'business'=>$businessId,'name'=>$data['owner_name'],'email'=>$data['email'],
                'phone'=>$data['phone'],'password'=>self::hashPassword($data['password']),
            ]);
            $userId = (int)$pdo->lastInsertId();

            $stmt = $pdo->prepare('INSERT INTO business_settings(business_id) VALUES(:business)');
            $stmt->execute(['business'=>$businessId]);

            $token = self::tokenMaterial();
            self::insertToken($pdo, $userId, $token);
            $pdo->commit();
            return [
                'business_id'=>$businessId,
                'user_id'=>$userId,
                'owner_name'=>$data['owner_name'],
                'email'=>$data['email'],
                'business_slug'=>$slug,
                'token'=>$token['token'],
                'expires_at'=>$token['expires_at'],
            ];
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            if ((string)$e->getCode() === '23000') throw new RuntimeException(V2ApiError::EMAIL_IN_USE);
            throw $e;
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            throw $e;
        }
    }

    public static function login(PDO $pdo, RateLimiter $limits, array $input, string $ip): array
    {
        $email = self::normalizeEmail((string)($input['email'] ?? ''));
        $password = (string)($input['password'] ?? '');
        $limits->assertAllowed('login', $ip, $email, 10, 15);

        $stmt = $pdo->prepare(
            "SELECT u.id AS user_id,u.business_id,u.name,u.email,u.password_hash,b.slug AS business_slug
             FROM users u JOIN businesses b ON b.id=u.business_id
             WHERE u.email=:email AND u.role='owner' AND b.active=1 LIMIT 1"
        );
        $stmt->execute(['email'=>$email]);
        $row = $stmt->fetch();
        if (!$row || !is_string($row['password_hash'] ?? null) || !password_verify($password, (string)$row['password_hash'])) {
            $limits->record('login', $ip, $email);
            throw new RuntimeException(V2ApiError::INVALID_CREDENTIALS);
        }

        $token = self::tokenMaterial();
        self::insertToken($pdo, (int)$row['user_id'], $token);
        return [
            'business_id'=>(int)$row['business_id'],
            'user_id'=>(int)$row['user_id'],
            'owner_name'=>(string)$row['name'],
            'email'=>(string)$row['email'],
            'business_slug'=>(string)$row['business_slug'],
            'token'=>$token['token'],
            'expires_at'=>$token['expires_at'],
        ];
    }

    public static function logout(PDO $pdo, string $rawToken): void
    {
        if ($rawToken === '') return;
        $stmt = $pdo->prepare('DELETE FROM api_tokens WHERE token_hash=:hash');
        $stmt->execute(['hash'=>hash('sha256', $rawToken)]);
    }

    public static function requireOwner(PDO $pdo): array
    {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null;
        $raw = Auth::extractBearer(is_string($header) ? $header : null);
        if ($raw === null) throw new RuntimeException(V2ApiError::UNAUTHORIZED);

        $hash = hash('sha256', $raw);
        $stmt = $pdo->prepare(
            "SELECT u.id AS user_id,u.business_id,u.name,u.email,u.role,b.slug AS business_slug,t.token_hash
             FROM api_tokens t JOIN users u ON u.id=t.user_id JOIN businesses b ON b.id=u.business_id
             WHERE t.token_hash=:hash AND t.expires_at>UTC_TIMESTAMP() AND u.role='owner' AND b.active=1 LIMIT 1"
        );
        $stmt->execute(['hash'=>$hash]);
        $row = $stmt->fetch();
        if (!$row) throw new RuntimeException(V2ApiError::UNAUTHORIZED);
        return $row;
    }

    private static function insertToken(PDO $pdo, int $userId, array $token): void
    {
        $stmt = $pdo->prepare('INSERT INTO api_tokens(user_id,token_hash,expires_at) VALUES(:user,:hash,:expires)');
        $stmt->execute(['user'=>$userId,'hash'=>$token['token_hash'],'expires'=>$token['expires_at']]);
    }
}
