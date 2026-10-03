<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/src/bootstrap.php';

try {
    $config = Config::load();
    if (PHP_SAPI !== 'cli') {
        $configured = (string)($config['cron_key'] ?? '');
        $supplied = (string)($_GET['key'] ?? '');
        if ($configured === '' || !hash_equals($configured, $supplied)) {
            http_response_code(403);
            echo "forbidden\n";
            exit(1);
        }
        header('Content-Type: application/json; charset=utf-8');
    }
    $pdo = Database::pdo($config);
    $queue = new ReminderQueueRepository($pdo);
    $client = new MetaWhatsAppClient($config);
    $summary = (new ReminderSender($queue, $client))->run(50);
    $payload = ['ok'=>true,'sent'=>$summary->sent,'retried'=>$summary->retried,'failed'=>$summary->failed,'time'=>gmdate('c')];
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . PHP_EOL;
} catch (Throwable $e) {
    error_log('Randevu reminder cron failed: ' . get_class($e));
    if (PHP_SAPI !== 'cli') http_response_code(500);
    echo json_encode(['ok'=>false,'error'=>'worker_failed']) . PHP_EOL;
    exit(1);
}
