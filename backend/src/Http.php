<?php

declare(strict_types=1);

final class Http
{
    public static function jsonBody(): array
    {
        $raw = file_get_contents('php://input') ?: '';
        if ($raw === '') return [];
        $decoded = json_decode($raw, true);
        if (!is_array($decoded)) throw new RuntimeException('invalid_json');
        return $decoded;
    }

    public static function respond(array $data, int $status = 200): never
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function error(string $error, int $status = 400, string $message = ''): never
    {
        self::respond(['ok'=>false,'error'=>$error,'message'=>$message !== '' ? $message : $error], $status);
    }
}
