<?php

declare(strict_types=1);

interface V2AppointmentStore
{
    public function list(int $businessId, string $from, string $to): array;
    public function create(int $businessId, array $payload, string $source): array;
    public function update(int $businessId, string $externalId, array $payload, int $expectedVersion): array;
    public function cancel(int $businessId, string $externalId, int $expectedVersion): array;
}

interface V2ReminderScheduler
{
    public function schedule(int $businessId, array $appointment): string;
    public function cancel(int $businessId, string $externalId): string;
}

final class V2AppointmentService
{
    public function __construct(
        private V2AppointmentStore $appointments,
        private V2ReminderScheduler $reminders
    ) {}

    public function list(int $businessId, string $from, string $to): array
    {
        self::validateWindow($from, $to);
        return $this->appointments->list($businessId, $from, $to);
    }

    public function create(int $businessId, array $payload, string $source): array
    {
        if (!in_array($source, ['android','public_web','system'], true)) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        $record = $this->appointments->create($businessId, $payload, $source);
        return $record + ['reminder_state'=>$this->safeSchedule($businessId, $record)];
    }

    public function update(int $businessId, string $externalId, array $payload, int $expectedVersion): array
    {
        $record = $this->appointments->update($businessId, $externalId, $payload, $expectedVersion);
        $status = strtolower((string)($record['status'] ?? ''));
        $reminder = in_array($status, ['cancelled','completed'], true)
            ? $this->safeCancel($businessId, $externalId)
            : $this->safeSchedule($businessId, $record);
        return $record + ['reminder_state'=>$reminder];
    }

    public function cancel(int $businessId, string $externalId, int $expectedVersion): array
    {
        $record = $this->appointments->cancel($businessId, $externalId, $expectedVersion);
        return $record + ['reminder_state'=>$this->safeCancel($businessId, $externalId)];
    }

    public static function validateWindow(string $from, string $to): void
    {
        $start = DateTimeImmutable::createFromFormat('!Y-m-d', $from);
        $end = DateTimeImmutable::createFromFormat('!Y-m-d', $to);
        if (!$start || !$end || $start->format('Y-m-d') !== $from || $end->format('Y-m-d') !== $to || $end < $start) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        if ((int)$start->diff($end)->format('%a') > 180) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
    }

    private function safeSchedule(int $businessId, array $record): string
    {
        try {
            return $this->reminders->schedule($businessId, $record);
        } catch (Throwable) {
            return 'needs_attention';
        }
    }

    private function safeCancel(int $businessId, string $externalId): string
    {
        try {
            return $this->reminders->cancel($businessId, $externalId);
        } catch (Throwable) {
            return 'needs_attention';
        }
    }
}

final class V2ReminderQueueScheduler implements V2ReminderScheduler
{
    public function __construct(
        private BusinessSettingsRepository $settings,
        private ReminderQueueRepository $queue
    ) {}

    public function schedule(int $businessId, array $appointment): string
    {
        $settings = $this->settings->get($businessId);
        if (!(bool)$settings['whatsapp_enabled']) {
            $this->queue->cancelAppointment($businessId, (string)$appointment['external_id']);
            return 'disabled';
        }

        $payload = [
            'id'=>(string)$appointment['external_id'],
            'customer_name'=>(string)($appointment['customer_name'] ?? ''),
            'customer_phone'=>(string)($appointment['customer_phone'] ?? ''),
            'date'=>(string)($appointment['date'] ?? ''),
            'time'=>(string)($appointment['time'] ?? ''),
            'service'=>(string)($appointment['service'] ?? ''),
            'staff'=>(string)($appointment['staff'] ?? ''),
            'status'=>(string)($appointment['status'] ?? 'pending'),
        ];
        $rows = $this->queue->replaceAppointment($businessId, $payload, [
            'enabled'=>(bool)$settings['whatsapp_enabled'],
            'reminder_24h'=>(bool)$settings['reminder_24h'],
            'reminder_2h'=>(bool)$settings['reminder_2h'],
            'timezone'=>(string)$settings['timezone'],
            'business_name'=>(string)$settings['business_name'],
            'template_name'=>(string)$settings['template_name'],
            'template_language'=>(string)$settings['template_language'],
        ]);
        return count($rows) > 0 ? 'scheduled' : 'not_due';
    }

    public function cancel(int $businessId, string $externalId): string
    {
        $this->queue->cancelAppointment($businessId, $externalId);
        return 'cancelled';
    }
}
