<?php

declare(strict_types=1);

final class Config
{
    public static function load(): array
    {
        $root = dirname(__DIR__);
        $file = $root . '/config.php';
        $example = $root . '/config.example.php';
        $config = require is_file($file) ? $file : $example;
        if (!is_array($config)) {
            throw new RuntimeException('Backend configuration must return an array.');
        }
        return $config;
    }
}
