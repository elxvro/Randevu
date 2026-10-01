<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET' && (str_ends_with($path, '/health') || $path === '/health')) {
    http_response_code(200);
    echo json_encode([
        'ok' => true,
        'service' => 'Randevu API',
        'version' => '0.5.0',
        'php' => PHP_VERSION,
        'time' => gmdate('c'),
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

if ($method === 'GET' && (str_ends_with($path, '/version') || $path === '/version')) {
    http_response_code(200);
    echo json_encode([
        'service' => 'Randevu API',
        'version' => '0.5.0',
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

http_response_code(404);
echo json_encode([
    'ok' => false,
    'error' => 'endpoint_not_found',
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
