<?php

declare(strict_types=1);

final class AvailabilityService
{
    public static function slots(
        array $business,
        array $service,
        array $staff,
        array $leaves,
        array $appointments,
        string $date
    ): array {
        if (!(bool)($business['active'] ?? true) || !(bool)($service['active'] ?? false) || !(bool)($staff['active'] ?? false)) return [];
        if (array_key_exists('public_booking', $staff) && !(bool)$staff['public_booking']) return [];

        $duration = (int)($service['duration_minutes'] ?? 0);
        if ($duration <= 0) return [];
        $opening = self::time((string)($business['opening_time'] ?? ''));
        $closing = self::time((string)($business['closing_time'] ?? ''));
        if ($opening === null || $closing === null || $closing <= $opening) return [];
        $day = DateTimeImmutable::createFromFormat('!Y-m-d', $date);
        if (!$day || $day->format('Y-m-d') !== $date) return [];

        $staffId = $staff['id'] ?? null;
        $staffExternal = $staff['external_id'] ?? null;
        $blocked = [];

        foreach ($leaves as $leave) {
            if (!self::sameStaff($leave, $staffId, $staffExternal)) continue;
            $from = (string)($leave['start_date'] ?? '');
            $to = (string)($leave['end_date'] ?? '');
            if ($from === '' || $to === '' || $date < $from || $date > $to) continue;

            $startTime = trim((string)($leave['start_time'] ?? ''));
            $endTime = trim((string)($leave['end_time'] ?? ''));
            if ($startTime === '' || $endTime === '') {
                $blocked[] = [0, 24 * 60];
                continue;
            }
            $start = self::time($startTime);
            $end = self::time($endTime);
            if ($start !== null && $end !== null && $end > $start) $blocked[] = [$start, $end];
        }

        foreach ($appointments as $appointment) {
            if (!self::sameStaff($appointment, $staffId, $staffExternal)) continue;
            $status = strtolower((string)($appointment['status'] ?? ''));
            if (!in_array($status, ['pending','confirmed'], true)) continue;
            $starts = trim((string)($appointment['starts_at'] ?? ''));
            $ends = trim((string)($appointment['ends_at'] ?? ''));
            if (!str_starts_with($starts, $date . ' ') || !str_starts_with($ends, $date . ' ')) continue;
            $start = self::time(substr($starts, 11));
            $end = self::time(substr($ends, 11));
            if ($start !== null && $end !== null && $end > $start) $blocked[] = [$start, $end];
        }

        $result = [];
        for ($start = $opening; $start + $duration <= $closing; $start += 15) {
            $end = $start + $duration;
            $overlap = false;
            foreach ($blocked as [$blockedStart, $blockedEnd]) {
                if ($start < $blockedEnd && $end > $blockedStart) {
                    $overlap = true;
                    break;
                }
            }
            if (!$overlap) $result[] = sprintf('%02d:%02d', intdiv($start, 60), $start % 60);
        }
        return $result;
    }

    private static function time(string $value): ?int
    {
        $value = trim($value);
        if (!preg_match('/^(\d{2}):(\d{2})(?::\d{2})?$/', $value, $match)) return null;
        $hour = (int)$match[1];
        $minute = (int)$match[2];
        if ($hour > 23 || $minute > 59) return null;
        return $hour * 60 + $minute;
    }

    private static function sameStaff(array $row, mixed $staffId, mixed $staffExternal): bool
    {
        if ($staffId !== null && array_key_exists('staff_id', $row)) {
            return (string)$row['staff_id'] === (string)$staffId;
        }
        if ($staffExternal !== null && array_key_exists('staff_external_id', $row)) {
            return (string)$row['staff_external_id'] === (string)$staffExternal;
        }
        return true;
    }
}
