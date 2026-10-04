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
