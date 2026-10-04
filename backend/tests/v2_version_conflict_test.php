<?php
declare(strict_types=1);

if (!file_exists(__DIR__.'/../src/V2TenantService.php')) { fwrite(STDERR, "FAIL: V2TenantService.php missing\n"); exit(1); }
require_once __DIR__.'/../src/V2Types.php';
require_once __DIR__.'/../src/V2TenantService.php';

$current=['external_id'=>'x','version'=>4,'name'=>'Server'];
try {
    V2Version::guard(3,4,$current);
    fwrite(STDERR,"FAIL: stale version accepted\n"); exit(1);
} catch (V2VersionConflict $e) {
    if ($e->current !== $current || $e->getMessage() !== V2ApiError::VERSION_CONFLICT) { fwrite(STDERR,"FAIL: conflict payload\n"); exit(1); }
}
V2Version::guard(4,4,$current);

echo "v2_version_conflict_test: OK\n";
