<?php

declare(strict_types=1);

$slugPath = __DIR__ . '/../src/Slug.php';

function slug_expect_same(string $expected, string $actual, string $case): void {
    if ($expected !== $actual) {
        fwrite(STDERR, "FAIL {$case}: expected {$expected}, got {$actual}\n");
        exit(1);
    }
}

if (!file_exists($slugPath)) {
    fwrite(STDERR, "FAIL: Slug.php missing\n");
    exit(1);
}
require_once $slugPath;

slug_expect_same('guzellik-atolyesi', Slug::base('Güzellik Atölyesi'), 'turkish');
slug_expect_same('emre-kuafor', Slug::base('  Emre   Kuaför!!  '), 'spaces-punctuation');
slug_expect_same('isletme', Slug::base('***'), 'empty-normalization');
slug_expect_same('cagdas-spa', Slug::base('ÇAĞDAŞ ŞPA'), 'uppercase-turkish');

echo "v2_slug_test: OK\n";
