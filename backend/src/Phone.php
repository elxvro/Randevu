<?php

declare(strict_types=1);

final class Phone
{
    public static function normalize(string $raw, string $defaultCountryCode = '90'): ?string
    {
        $digits = preg_replace('/\D+/', '', $raw) ?? '';
        if (str_starts_with($digits, '00')) {
            $digits = substr($digits, 2);
        } elseif (str_starts_with($digits, '0') && strlen($digits) === 11) {
            $digits = $defaultCountryCode . substr($digits, 1);
        }
        if (strlen($digits) < 10 || strlen($digits) > 15 || str_starts_with($digits, '0')) {
            return null;
        }
        return $digits;
    }
}
