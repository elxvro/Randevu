<?php

return [
    'db' => [
        'host' => getenv('RANDEVU_DB_HOST') ?: 'localhost',
        'port' => (int) (getenv('RANDEVU_DB_PORT') ?: 3306),
        'name' => getenv('RANDEVU_DB_NAME') ?: 'randevu',
        'user' => getenv('RANDEVU_DB_USER') ?: 'randevu_user',
        'pass' => getenv('RANDEVU_DB_PASS') ?: '',
        'charset' => 'utf8mb4',
    ],
    'token_ttl_hours' => 720,
];
