<?php

declare(strict_types=1);

final class Database
{
    public static function pdo(array $config): PDO
    {
        $db = $config['db'] ?? [];
        $host = (string) ($db['host'] ?? 'localhost');
        $port = (int) ($db['port'] ?? 3306);
        $name = (string) ($db['name'] ?? 'randevu');
        $charset = (string) ($db['charset'] ?? 'utf8mb4');
        $dsn = "mysql:host={$host};port={$port};dbname={$name};charset={$charset}";
        return new PDO($dsn, (string) ($db['user'] ?? ''), (string) ($db['pass'] ?? ''), [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
    }
}
