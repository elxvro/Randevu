<?php

declare(strict_types=1);

interface ReminderQueueStore
{
    public function cancelBusiness(int $businessId): void
    {
        $stmt = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='cancelled',updated_at=CURRENT_TIMESTAMP WHERE business_id=:business AND status IN ('pending','processing')");
        $stmt->execute(['business'=>$businessId]);
    }

    public function claimDue(int $batchSize): array;
    public function markSent(int $id, string $messageId): void;
    public function markRetry(int $id, string $error): void;
    public function markFailed(int $id, string $error): void;
}

final class ReminderQueueRepository implements ReminderQueueStore
{
    public function __construct(private PDO $pdo, private int $maxAttempts = 4) {}

    public function replaceAppointment(int $businessId, array $appointment, array $settings): array
    {
        $rows = ReminderSchedule::build($appointment, $settings, new DateTimeImmutable('now', new DateTimeZone('UTC')));
        $externalId = trim((string)($appointment['id'] ?? ''));
        if ($externalId === '') throw new RuntimeException('missing_appointment_id');
        $this->pdo->beginTransaction();
        try {
            $cancel = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='cancelled',updated_at=CURRENT_TIMESTAMP WHERE business_id=:business AND appointment_external_id=:appointment AND status IN ('pending','processing')");
            $cancel->execute(['business'=>$businessId,'appointment'=>$externalId]);
            foreach ($rows as $row) {
                $sql = "INSERT INTO whatsapp_reminder_queue(business_id,appointment_external_id,offset_minutes,recipient_phone,scheduled_at_utc,next_attempt_at,status,attempt_count,last_error,meta_message_id,template_name,template_language,parameters_json) VALUES(:business,:appointment,:offset,:phone,:scheduled,:next_attempt,'pending',0,NULL,NULL,:template,:language,:parameters) ON DUPLICATE KEY UPDATE business_id=VALUES(business_id),recipient_phone=VALUES(recipient_phone),scheduled_at_utc=VALUES(scheduled_at_utc),next_attempt_at=VALUES(next_attempt_at),status='pending',attempt_count=0,last_error=NULL,meta_message_id=NULL,template_name=VALUES(template_name),template_language=VALUES(template_language),parameters_json=VALUES(parameters_json)";
                $stmt = $this->pdo->prepare($sql);
                $stmt->execute([
                    'business'=>$businessId,'appointment'=>$externalId,'offset'=>$row['offset_minutes'],'phone'=>$row['recipient_phone'],
                    'scheduled'=>$row['scheduled_at_utc'],'next_attempt'=>$row['next_attempt_at'],'template'=>$row['template_name'],
                    'language'=>$row['template_language'],'parameters'=>json_encode($row['parameters'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
                ]);
            }
            $this->pdo->commit();
            return $rows;
        } catch (Throwable $e) {
            if ($this->pdo->inTransaction()) $this->pdo->rollBack();
            throw $e;
        }
    }

    public function cancelAppointment(int $businessId, string $externalId): void
    {
        $stmt = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='cancelled' WHERE business_id=:business AND appointment_external_id=:appointment AND status IN ('pending','processing')");
        $stmt->execute(['business'=>$businessId,'appointment'=>$externalId]);
    }

    public function claimDue(int $batchSize): array
    {
        $limit = max(1, min(100, $batchSize));
        $this->pdo->beginTransaction();
        try {
            $stmt = $this->pdo->query("SELECT * FROM whatsapp_reminder_queue WHERE status='pending' AND scheduled_at_utc<=UTC_TIMESTAMP() AND next_attempt_at<=UTC_TIMESTAMP() ORDER BY scheduled_at_utc,id LIMIT {$limit} FOR UPDATE");
            $rows = $stmt->fetchAll() ?: [];
            if ($rows) {
                $ids = array_map(fn(array $row): int => (int)$row['id'], $rows);
                $placeholders = implode(',', array_fill(0, count($ids), '?'));
                $update = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='processing',attempt_count=attempt_count+1 WHERE id IN ({$placeholders}) AND status='pending'");
                $update->execute($ids);
                foreach ($rows as &$row) {
                    $row['status'] = 'processing';
                    $row['attempt_count'] = ((int)$row['attempt_count']) + 1;
                    $row['parameters'] = json_decode((string)$row['parameters_json'], true) ?: [];
                }
            }
            $this->pdo->commit();
            return $rows;
        } catch (Throwable $e) {
            if ($this->pdo->inTransaction()) $this->pdo->rollBack();
            throw $e;
        }
    }

    public function markSent(int $id, string $messageId): void
    {
        $stmt = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='sent',meta_message_id=:message,last_error=NULL WHERE id=:id");
        $stmt->execute(['message'=>$messageId,'id'=>$id]);
    }

    public function markRetry(int $id, string $error): void
    {
        $stmt = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status=CASE WHEN attempt_count>=:max_attempts THEN 'failed' ELSE 'pending' END,last_error=:error,next_attempt_at=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 10 MINUTE) WHERE id=:id");
        $stmt->execute(['max_attempts'=>$this->maxAttempts,'error'=>substr($error,0,500),'id'=>$id]);
    }

    public function markFailed(int $id, string $error): void
    {
        $stmt = $this->pdo->prepare("UPDATE whatsapp_reminder_queue SET status='failed',last_error=:error WHERE id=:id");
        $stmt->execute(['error'=>substr($error,0,500),'id'=>$id]);
    }
}
