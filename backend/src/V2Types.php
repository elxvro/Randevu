<?php

declare(strict_types=1);

final class V2ApiError
{
    public const VALIDATION_ERROR = 'validation_error';
    public const UNAUTHORIZED = 'unauthorized';
    public const EMAIL_IN_USE = 'email_in_use';
    public const INVALID_CREDENTIALS = 'invalid_credentials';
    public const TENANT_MISMATCH = 'tenant_mismatch';
    public const NOT_FOUND = 'not_found';
    public const VERSION_CONFLICT = 'version_conflict';
    public const SLOT_UNAVAILABLE = 'slot_unavailable';
    public const RATE_LIMITED = 'rate_limited';
    public const SERVER_ERROR = 'server_error';

    public static function all(): array
    {
        return [
            self::VALIDATION_ERROR,
            self::UNAUTHORIZED,
            self::EMAIL_IN_USE,
            self::INVALID_CREDENTIALS,
            self::TENANT_MISMATCH,
            self::NOT_FOUND,
            self::VERSION_CONFLICT,
            self::SLOT_UNAVAILABLE,
            self::RATE_LIMITED,
            self::SERVER_ERROR,
        ];
    }
}

final class V2Id
{
    public static function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);
        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12)
        );
    }

    public static function external(?string $value = null): string
    {
        $value = trim((string)$value);
        if ($value === '') return self::uuid();
        if (strlen($value) > 36 || !preg_match('/^[A-Za-z0-9._:-]+$/', $value)) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        return $value;
    }
}

final class V2VersionConflict extends RuntimeException
{
    public function __construct(public array $current)
    {
        parent::__construct(V2ApiError::VERSION_CONFLICT);
    }
}

final class V2Version
{
    public static function guard(int $expected, int $current, array $record): void
    {
        if ($expected !== $current) throw new V2VersionConflict($record);
    }
}
