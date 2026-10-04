<?php

declare(strict_types=1);

final class V2BusinessRepository implements V2BusinessStore
{
    public function __construct(private PDO $pdo) {}

    public function get(int $businessId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT id,name AS business_name,slug,phone,address,timezone,opening_time,closing_time,active,version
             FROM businesses WHERE id=:business LIMIT 1'
        );
        $stmt->execute(['business'=>$businessId]);
        $row = $stmt->fetch();
        if (!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $this->project($row);
    }

    public function update(int $businessId, array $input, int $expectedVersion): array
    {
        $current = $this->get($businessId);
        V2Version::guard($expectedVersion, (int)$current['version'], $current);

        $name = trim((string)($input['business_name'] ?? $current['business_name']));
        $timezone = trim((string)($input['timezone'] ?? $current['timezone']));
        $opening = trim((string)($input['opening_time'] ?? $current['opening_time']));
        $closing = trim((string)($input['closing_time'] ?? $current['closing_time']));
        if (strlen($name) < 2 || strlen($name) > 160) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        try { new DateTimeZone($timezone); } catch (Throwable) { throw new RuntimeException(V2ApiError::VALIDATION_ERROR); }

        $open = DateTimeImmutable::createFromFormat('!H:i:s', strlen($opening) === 5 ? $opening.':00' : $opening);
        $close = DateTimeImmutable::createFromFormat('!H:i:s', strlen($closing) === 5 ? $closing.':00' : $closing);
        if (!$open || !$close || $open >= $close) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);

        $rawPhone = trim((string)($input['phone'] ?? $current['phone'] ?? ''));
        $phone = $rawPhone === '' ? null : Phone::normalize($rawPhone);
        if ($rawPhone !== '' && $phone === null) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);

        $stmt = $this->pdo->prepare(
            'UPDATE businesses SET name=:name,phone=:phone,address=:address,timezone=:timezone,
             opening_time=:opening,closing_time=:closing,active=:active,version=version+1
             WHERE id=:business AND version=:expected'
        );
        $stmt->execute([
            'name'=>$name,'phone'=>$phone,'address'=>trim((string)($input['address'] ?? $current['address'] ?? '')),
            'timezone'=>$timezone,'opening'=>$open->format('H:i:s'),'closing'=>$close->format('H:i:s'),
            'active'=>(int)(bool)($input['active'] ?? $current['active']),
            'business'=>$businessId,'expected'=>$expectedVersion,
        ]);
        if ($stmt->rowCount() !== 1) {
            $latest = $this->get($businessId);
            throw new V2VersionConflict($latest);
        }
        return $this->get($businessId);
    }

    private function project(array $row): array
    {
        return [
            'business_name'=>(string)$row['business_name'],
            'slug'=>(string)$row['slug'],
            'phone'=>$row['phone'] === null ? '' : (string)$row['phone'],
            'address'=>$row['address'] === null ? '' : (string)$row['address'],
            'timezone'=>(string)$row['timezone'],
            'opening_time'=>substr((string)$row['opening_time'],0,5),
            'closing_time'=>substr((string)$row['closing_time'],0,5),
            'active'=>(bool)$row['active'],
            'version'=>(int)$row['version'],
        ];
    }
}
