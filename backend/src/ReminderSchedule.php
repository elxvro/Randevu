<?php

declare(strict_types=1);

final class ReminderSchedule
{
    public static function build(array $appointment, array $settings, DateTimeImmutable $nowUtc): array
    {
        $status = strtolower(trim((string)($appointment['status'] ?? '')));
        if (in_array($status, ['cancelled','completed'], true) || !(bool)($settings['enabled'] ?? $settings['whatsapp_enabled'] ?? false)) return [];
        $phone = Phone::normalize((string)($appointment['customer_phone'] ?? ''));
        if ($phone === null) return [];
        try {
            $zone = new DateTimeZone((string)($settings['timezone'] ?? 'UTC'));
            $startsLocal = new DateTimeImmutable(trim((string)($appointment['date'] ?? '')) . ' ' . trim((string)($appointment['time'] ?? '')), $zone);
        } catch (Throwable) {
            return [];
        }
        $startsUtc = $startsLocal->setTimezone(new DateTimeZone('UTC'));
        $offsets = [];
        if ((bool)($settings['reminder_24h'] ?? true)) $offsets[] = 1440;
        if ((bool)($settings['reminder_2h'] ?? true)) $offsets[] = 120;
        $rows = [];
        foreach ($offsets as $offset) {
            $scheduled = $startsUtc->modify("-{$offset} minutes");
            if ($scheduled <= $nowUtc) continue;
            $id = trim((string)($appointment['id'] ?? ''));
            if ($id === '') continue;
            $rows[] = [
                'unique_key' => $id . ':' . $offset,
                'appointment_external_id' => $id,
                'offset_minutes' => $offset,
                'recipient_phone' => $phone,
                'scheduled_at_utc' => $scheduled->format('Y-m-d H:i:s'),
                'next_attempt_at' => $scheduled->format('Y-m-d H:i:s'),
                'template_name' => trim((string)($settings['template_name'] ?? 'appointment_reminder')),
                'template_language' => trim((string)($settings['template_language'] ?? 'tr')),
                'parameters' => [
                    trim((string)($appointment['customer_name'] ?? 'Müşteri')) ?: 'Müşteri',
                    trim((string)($settings['business_name'] ?? 'İşletme')) ?: 'İşletme',
                    $startsLocal->format('d.m.Y'),
                    $startsLocal->format('H:i'),
                    trim((string)($appointment['service'] ?? 'Randevu')) ?: 'Randevu',
                    trim((string)($appointment['staff'] ?? '')) ?: 'Belirtilmedi',
                ],
            ];
        }
        return $rows;
    }
}
