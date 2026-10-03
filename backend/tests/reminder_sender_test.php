<?php

declare(strict_types=1);

require_once __DIR__ . '/../src/MetaWhatsAppClient.php';
require_once __DIR__ . '/../src/ReminderSender.php';

final class FakeQueue implements ReminderQueueStore {
    public array $rows;
    public array $sent = [];
    public array $retried = [];
    public array $failed = [];
    public function __construct(array $rows) { $this->rows = $rows; }
    public function claimDue(int $batchSize): array { return array_slice($this->rows, 0, $batchSize); }
    public function markSent(int $id, string $messageId): void { $this->sent[$id] = $messageId; }
    public function markRetry(int $id, string $error): void { $this->retried[$id] = $error; }
    public function markFailed(int $id, string $error): void { $this->failed[$id] = $error; }
}

final class FakeMeta implements MetaWhatsAppClientContract {
    public function __construct(private SendResult $result) {}
    public function sendTemplate(string $recipient, string $template, string $language, array $parameters): SendResult { return $this->result; }
}

function assert_ok(bool $value, string $message): void { if (!$value) { fwrite(STDERR, "FAIL: {$message}\n"); exit(1); } }

$row = ['id'=>1,'recipient_phone'=>'905551112233','template_name'=>'appointment_reminder','template_language'=>'tr','parameters'=>['Ayşe','Studio','05.10.2026','10:00','Bakım','Deniz'],'attempt_count'=>0,'status'=>'processing'];
$q1 = new FakeQueue([$row]);
$s1 = new ReminderSender($q1, new FakeMeta(SendResult::success('wamid.1')));
$summary1 = $s1->run();
assert_ok(($q1->sent[1] ?? '') === 'wamid.1' && $summary1->sent === 1, 'success marks sent');

$q2 = new FakeQueue([$row]);
$s2 = new ReminderSender($q2, new FakeMeta(SendResult::transientFailure('timeout SECRET_TOKEN')));
$summary2 = $s2->run();
assert_ok(isset($q2->retried[1]) && !str_contains($q2->retried[1], 'SECRET_TOKEN') && $summary2->retried === 1, 'transient retries and redacts');

$q3 = new FakeQueue([$row]);
$s3 = new ReminderSender($q3, new FakeMeta(SendResult::permanentFailure('configuration invalid SECRET_TOKEN')));
$summary3 = $s3->run();
assert_ok(isset($q3->failed[1]) && !str_contains($q3->failed[1], 'SECRET_TOKEN') && $summary3->failed === 1, 'permanent failure marked failed and redacted');

$q4 = new FakeQueue([]);
$s4 = new ReminderSender($q4, new FakeMeta(SendResult::success('wamid.never')));
$summary4 = $s4->run();
assert_ok($summary4->sent === 0, 'already-sent rows are not claimed/resend');

echo "reminder_sender_test: OK\n";
