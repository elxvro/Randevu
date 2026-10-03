<?php

declare(strict_types=1);

require_once __DIR__ . '/ReminderQueueRepository.php';

final class SenderSummary
{
    public function __construct(public int $sent = 0, public int $retried = 0, public int $failed = 0) {}
}

final class ReminderSender
{
    public function __construct(private ReminderQueueStore $queue, private MetaWhatsAppClientContract $client) {}

    public function run(int $batchSize = 50): SenderSummary
    {
        $summary = new SenderSummary();
        foreach ($this->queue->claimDue($batchSize) as $row) {
            $id = (int)($row['id'] ?? 0);
            if ($id <= 0) continue;
            $parameters = $row['parameters'] ?? json_decode((string)($row['parameters_json'] ?? '[]'), true) ?: [];
            $result = $this->client->sendTemplate(
                (string)($row['recipient_phone'] ?? ''),
                (string)($row['template_name'] ?? 'appointment_reminder'),
                (string)($row['template_language'] ?? 'tr'),
                is_array($parameters) ? $parameters : []
            );
            if ($result->ok) {
                $this->queue->markSent($id, $result->messageId);
                $summary->sent++;
            } elseif ($result->transient) {
                $this->queue->markRetry($id, self::sanitize($result->error));
                $summary->retried++;
            } else {
                $this->queue->markFailed($id, self::sanitize($result->error));
                $summary->failed++;
            }
        }
        return $summary;
    }

    private static function sanitize(string $error): string
    {
        $clean = preg_replace('/Bearer\s+[A-Za-z0-9._\-]+/i', 'Bearer [redacted]', $error) ?? $error;
        $clean = str_replace('SECRET_TOKEN', '[redacted]', $clean);
        return substr($clean, 0, 500);
    }
}
