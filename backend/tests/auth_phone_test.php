<?php

declare(strict_types=1);

require_once __DIR__ . '/../src/Phone.php';
require_once __DIR__ . '/../src/Auth.php';

function same($expected, $actual, string $message): void {
    if ($expected !== $actual) {
        fwrite(STDERR, "FAIL: {$message}; expected=" . var_export($expected, true) . " actual=" . var_export($actual, true) . "\n");
        exit(1);
    }
}

same('905551112233', Phone::normalize('+90 555 111 22 33'), 'plus 90');
same('905551112233', Phone::normalize('00905551112233'), '0090');
same('905551112233', Phone::normalize('0555 111 22 33'), 'turkish local');
same('447700900123', Phone::normalize('+44 7700 900123'), 'international');
same(null, Phone::normalize('1234'), 'too short');
same(null, Phone::normalize('not-a-phone'), 'malformed');
same('abc123', Auth::extractBearer('Bearer abc123'), 'bearer extraction');
same(null, Auth::extractBearer('Basic abc123'), 'wrong scheme');
same(null, Auth::extractBearer('Bearer '), 'empty bearer');

echo "auth_phone_test: OK\n";
