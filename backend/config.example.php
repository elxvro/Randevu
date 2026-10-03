<?php

declare(strict_types=1);

return [
    'db' => [
        'host' => getenv('RANDEVU_DB_HOST') ?: 'localhost',
        'port' => (int) (getenv('RANDEVU_DB_PORT') ?: 3306),
        'name' => getenv('RANDEVU_DB_NAME') ?: 'randevu',
        'user' => getenv('RANDEVU_DB_USER') ?: 'randevu_user',
        'pass' => getenv('RANDEVU_DB_PASS') ?: '',
        'charset' => 'utf8mb4',
    ],
    'token_ttl_hours' => (int) (getenv('RANDEVU_TOKEN_TTL_HOURS') ?: 720),
    'app_setup_key' => getenv('RANDEVU_APP_SETUP_KEY') ?: '',
    'cron_key' => getenv('RANDEVU_CRON_KEY') ?: '',
    'meta_access_token' => getenv('META_ACCESS_TOKEN') ?: '',
    'meta_phone_number_id' => getenv('META_PHONE_NUMBER_ID') ?: '',
    'meta_graph_version' => getenv('META_GRAPH_VERSION') ?: 'v23.0',
];
