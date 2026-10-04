<?php

declare(strict_types=1);

final class Slug
{
    public static function base(string $name): string
    {
        $value = strtr(trim($name), [
            'Ç'=>'C','Ğ'=>'G','İ'=>'I','I'=>'I','Ö'=>'O','Ş'=>'S','Ü'=>'U',
            'ç'=>'c','ğ'=>'g','ı'=>'i','i'=>'i','ö'=>'o','ş'=>'s','ü'=>'u',
        ]);
        $value = strtolower($value);
        $value = preg_replace('/[^a-z0-9]+/', '-', $value) ?? '';
        $value = trim($value, '-');
        return $value !== '' ? $value : 'isletme';
    }

    public static function unique(PDO $pdo, string $name): string
    {
        $base = self::base($name);
        $candidate = $base;
        $suffix = 2;
        $stmt = $pdo->prepare('SELECT 1 FROM businesses WHERE slug = :slug LIMIT 1');
        while (true) {
            $stmt->execute(['slug' => $candidate]);
            if ($stmt->fetchColumn() === false) {
                return $candidate;
            }
            $candidate = $base . '-' . $suffix++;
        }
    }
}
