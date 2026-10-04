<?php

declare(strict_types=1);

interface V2SyncStore
{
    public function business(int $businessId): array;
    public function services(int $businessId): array;
    public function staff(int $businessId): array;
    public function leaves(int $businessId, string $from, string $to): array;
    public function appointments(int $businessId, string $from, string $to): array;
}

final class V2SyncService
{
    public function __construct(private V2SyncStore $store) {}

    public function bootstrap(int $businessId, DateTimeImmutable $now, string $timezone = 'UTC'): array
    {
        try {
            $zone = new DateTimeZone($timezone);
        } catch (Throwable) {
            $zone = new DateTimeZone('UTC');
        }
        $local = $now->setTimezone($zone);
        $from = $local->modify('-90 days')->format('Y-m-d');
        $to = $local->modify('+365 days')->format('Y-m-d');

        return [
            'business'=>$this->store->business($businessId),
            'services'=>$this->store->services($businessId),
            'staff'=>$this->store->staff($businessId),
            'staff_leaves'=>$this->store->leaves($businessId,$from,$to),
            'appointments'=>$this->store->appointments($businessId,$from,$to),
            'server_time'=>$now->setTimezone(new DateTimeZone('UTC'))->format(DateTimeInterface::ATOM),
        ];
    }
}

final class V2PdoSyncStore implements V2SyncStore
{
    public function __construct(
        private PDO $pdo,
        private V2BusinessRepository $businesses,
        private V2CatalogRepository $catalog,
        private V2StaffRepository $staffRepo,
        private V2AppointmentRepository $appointmentsRepo
    ) {}

    public function business(int $businessId): array
    {
        return $this->businesses->get($businessId);
    }

    public function services(int $businessId): array
    {
        return $this->catalog->listServices($businessId);
    }

    public function staff(int $businessId): array
    {
        return $this->staffRepo->listStaff($businessId);
    }

    public function leaves(int $businessId, string $from, string $to): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT l.external_id,s.external_id AS staff_external_id,l.start_date,l.end_date,
                    l.start_time,l.end_time,l.reason,l.version
             FROM staff_leaves l
             JOIN staff s ON s.id=l.staff_id AND s.business_id=l.business_id
             WHERE l.business_id=:business AND l.end_date>=:from_date AND l.start_date<=:to_date
             ORDER BY l.start_date,l.id'
        );
        $stmt->execute(['business'=>$businessId,'from_date'=>$from,'to_date'=>$to]);
        return array_map(static fn(array $row): array => [
            'external_id'=>(string)$row['external_id'],
            'staff_external_id'=>(string)$row['staff_external_id'],
            'start_date'=>(string)$row['start_date'],
            'end_date'=>(string)$row['end_date'],
            'start_time'=>$row['start_time']===null?null:substr((string)$row['start_time'],0,5),
            'end_time'=>$row['end_time']===null?null:substr((string)$row['end_time'],0,5),
            'reason'=>$row['reason']===null?'':(string)$row['reason'],
            'version'=>(int)$row['version'],
        ],$stmt->fetchAll() ?: []);
    }

    public function appointments(int $businessId, string $from, string $to): array
    {
        return $this->appointmentsRepo->list($businessId,$from,$to);
    }
}
