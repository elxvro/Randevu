<?php

declare(strict_types=1);

final class PublicBookingService
{
    public function __construct(
        private PDO $pdo,
        private V2AppointmentService $appointments,
        private RateLimiter $limits
    ) {}

    public static function validateBookingPayload(array $input): array
    {
        $slug = trim((string)($input['slug'] ?? ''));
        $service = trim((string)($input['service'] ?? $input['service_external_id'] ?? ''));
        $staff = trim((string)($input['staff'] ?? $input['staff_external_id'] ?? ''));
        $date = trim((string)($input['date'] ?? ''));
        $time = trim((string)($input['time'] ?? ''));
        $name = trim((string)($input['customer_name'] ?? ''));
        $phone = Phone::normalize((string)($input['customer_phone'] ?? ''));

        $validExternal = static fn(string $value): bool =>
            $value !== '' && strlen($value) <= 36 && preg_match('/^[A-Za-z0-9._:-]+$/', $value) === 1;
        $dateObject = DateTimeImmutable::createFromFormat('!Y-m-d', $date);

        if (
            $slug === '' || strlen($slug) > 96 || preg_match('/^[a-z0-9-]+$/', $slug) !== 1 ||
            !$validExternal($service) || !$validExternal($staff) ||
            !$dateObject || $dateObject->format('Y-m-d') !== $date ||
            preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/', $time) !== 1 ||
            strlen($name) < 2 || strlen($name) > 160 || $phone === null
        ) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }

        return [
            'slug'=>$slug,
            'service'=>$service,
            'staff'=>$staff,
            'date'=>$date,
            'time'=>$time,
            'customer_name'=>$name,
            'customer_phone'=>$phone,
            'note'=>trim((string)($input['note'] ?? '')),
        ];
    }

    public static function redactCatalog(array $raw): array
    {
        $business = $raw['business'] ?? [];
        $services = array_values(array_map(
            static fn(array $row): array => [
                'external_id'=>(string)$row['external_id'],
                'name'=>(string)$row['name'],
                'duration_minutes'=>(int)$row['duration_minutes'],
                'price'=>(string)($row['price'] ?? '0.00'),
            ],
            array_filter($raw['services'] ?? [], static fn(array $row): bool => (bool)($row['active'] ?? false))
        ));
        $staff = array_values(array_map(
            static fn(array $row): array => [
                'external_id'=>(string)$row['external_id'],
                'name'=>(string)$row['name'],
                'title'=>(string)($row['title'] ?? ''),
            ],
            array_filter(
                $raw['staff'] ?? [],
                static fn(array $row): bool => (bool)($row['active'] ?? false) && (bool)($row['public_booking'] ?? false)
            )
        ));
        return [
            'business'=>[
                'name'=>(string)($business['name'] ?? $business['business_name'] ?? ''),
                'slug'=>(string)($business['slug'] ?? ''),
                'phone'=>(string)($business['phone'] ?? ''),
                'address'=>(string)($business['address'] ?? ''),
                'timezone'=>(string)($business['timezone'] ?? ''),
            ],
            'services'=>$services,
            'staff'=>$staff,
        ];
    }

    public function catalog(string $slug): array
    {
        $business = $this->businessBySlug($slug);

        $services = $this->pdo->prepare(
            'SELECT external_id,name,duration_minutes,price,active FROM services
             WHERE business_id=:business ORDER BY name,id'
        );
        $services->execute(['business'=>(int)$business['id']]);

        $staff = $this->pdo->prepare(
            'SELECT external_id,name,title,phone,active,public_booking FROM staff
             WHERE business_id=:business ORDER BY name,id'
        );
        $staff->execute(['business'=>(int)$business['id']]);

        return self::redactCatalog([
            'business'=>$business,
            'services'=>$services->fetchAll() ?: [],
            'staff'=>$staff->fetchAll() ?: [],
        ]);
    }

    public function availability(string $slug, string $serviceId, string $staffId, string $date): array
    {
        $business = $this->businessBySlug($slug);
        $businessId = (int)$business['id'];

        $serviceStmt = $this->pdo->prepare(
            'SELECT id,external_id,name,duration_minutes,active FROM services
             WHERE business_id=:business AND external_id=:external AND active=1 LIMIT 1'
        );
        $serviceStmt->execute(['business'=>$businessId,'external'=>$serviceId]);
        $service = $serviceStmt->fetch();
        if (!$service) throw new RuntimeException(V2ApiError::NOT_FOUND);

        $staffStmt = $this->pdo->prepare(
            'SELECT id,external_id,name,active,public_booking FROM staff
             WHERE business_id=:business AND external_id=:external AND active=1 AND public_booking=1 LIMIT 1'
        );
        $staffStmt->execute(['business'=>$businessId,'external'=>$staffId]);
        $staff = $staffStmt->fetch();
        if (!$staff) throw new RuntimeException(V2ApiError::NOT_FOUND);

        $dateObject = DateTimeImmutable::createFromFormat('!Y-m-d', $date);
        if (!$dateObject || $dateObject->format('Y-m-d') !== $date) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);

        $leaveStmt = $this->pdo->prepare(
            'SELECT staff_id,start_date,end_date,start_time,end_time FROM staff_leaves
             WHERE business_id=:business AND staff_id=:staff AND start_date<=:date AND end_date>=:date'
        );
        $leaveStmt->execute(['business'=>$businessId,'staff'=>(int)$staff['id'],'date'=>$date]);

        $zone = new DateTimeZone((string)$business['timezone']);
        $fromUtc = (new DateTimeImmutable($date.' 00:00:00',$zone))->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s');
        $toUtc = (new DateTimeImmutable($date.' 23:59:59',$zone))->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s');
        $appointmentStmt = $this->pdo->prepare(
            "SELECT staff_id,starts_at,ends_at,status FROM appointments
             WHERE business_id=:business AND staff_id=:staff AND starts_at<=:to_utc AND ends_at>=:from_utc
               AND status IN ('pending','confirmed')"
        );
        $appointmentStmt->execute([
            'business'=>$businessId,'staff'=>(int)$staff['id'],'from_utc'=>$fromUtc,'to_utc'=>$toUtc
        ]);
        $appointmentRows = [];
        foreach ($appointmentStmt->fetchAll() ?: [] as $row) {
            $start = (new DateTimeImmutable((string)$row['starts_at'],new DateTimeZone('UTC')))->setTimezone($zone);
            $end = (new DateTimeImmutable((string)$row['ends_at'],new DateTimeZone('UTC')))->setTimezone($zone);
            $appointmentRows[] = [
                'staff_id'=>$row['staff_id'],
                'starts_at'=>$start->format('Y-m-d H:i:s'),
                'ends_at'=>$end->format('Y-m-d H:i:s'),
                'status'=>$row['status'],
            ];
        }

        return AvailabilityService::slots(
            $business,
            $service,
            $staff,
            $leaveStmt->fetchAll() ?: [],
            $appointmentRows,
            $date
        );
    }

    public function book(array $input, string $ip): array
    {
        $data = self::validateBookingPayload($input);
        $this->limits->consume('public_booking', $ip, $data['slug'], 20, 15);
        $business = $this->businessBySlug($data['slug']);
        $result = $this->appointments->create((int)$business['id'], [
            'external_id'=>V2Id::uuid(),
            'service_external_id'=>$data['service'],
            'staff_external_id'=>$data['staff'],
            'date'=>$data['date'],
            'time'=>$data['time'],
            'customer_name'=>$data['customer_name'],
            'customer_phone'=>$data['customer_phone'],
            'note'=>$data['note'],
            'status'=>'pending',
        ], 'public_web');

        return [
            'appointment'=>[
                'external_id'=>$result['external_id'],
                'date'=>$result['date'],
                'time'=>$result['time'],
                'service'=>$result['service'],
                'staff'=>$result['staff'],
                'status'=>$result['status'],
                'version'=>$result['version'],
            ],
            'reminder_state'=>$result['reminder_state'],
        ];
    }

    private function businessBySlug(string $slug): array
    {
        $slug=trim($slug);
        if($slug==='' || strlen($slug)>96 || preg_match('/^[a-z0-9-]+$/',$slug)!==1) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        $stmt=$this->pdo->prepare(
            'SELECT id,name,slug,phone,address,timezone,opening_time,closing_time,active FROM businesses
             WHERE slug=:slug AND active=1 LIMIT 1'
        );
        $stmt->execute(['slug'=>$slug]);
        $row=$stmt->fetch();
        if(!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $row;
    }
}
