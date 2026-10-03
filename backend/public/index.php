<?php

declare(strict_types=1);

require_once dirname(__DIR__) . '/src/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

function route_ends_with(string $path, string $route): bool {
    return $path === $route || str_ends_with($path, $route);
}

try {
    if ($method === 'GET' && route_ends_with($path, '/health')) {
        Http::respond(['ok'=>true,'service'=>'Randevu API','version'=>'1.3.1','php'=>PHP_VERSION,'time'=>gmdate('c')]);
    }
    if ($method === 'GET' && route_ends_with($path, '/version')) {
        Http::respond(['ok'=>true,'service'=>'Randevu API','version'=>'1.3.1']);
    }

    $config = Config::load();
    $pdo = Database::pdo($config);

    if ($method === 'POST' && route_ends_with($path, '/auth/bootstrap')) {
        $result = Auth::bootstrap($pdo, $config, Http::jsonBody());
        Http::respond(['ok'=>true] + $result, 201);
    }

    $auth = Auth::requireBusiness($pdo);
    $businessId = (int)$auth['business_id'];
    $settingsRepo = new BusinessSettingsRepository($pdo);
    $settings = $settingsRepo->get($businessId);

    if ($method === 'GET' && route_ends_with($path, '/whatsapp/status')) {
        Http::respond(['ok'=>true,'whatsapp'=>WhatsAppStatusService::project($settings, $config, null)]);
    }

    if ($method === 'POST' && route_ends_with($path, '/whatsapp/test-connection')) {
        $last = trim((string)($settings['last_connection_test_at'] ?? ''));
        if ($last !== '') {
            $lastAt = new DateTimeImmutable($last, new DateTimeZone('UTC'));
            if ($lastAt > new DateTimeImmutable('-60 seconds', new DateTimeZone('UTC'))) {
                Http::error('rate_limited', 429, 'Bağlantı testi en fazla dakikada bir çalıştırılabilir.');
            }
        }
        $phoneId = trim((string)($settings['meta_phone_number_id'] ?? ''));
        $client = new MetaWhatsAppClient($config, $phoneId !== '' ? $phoneId : null);
        $probeError = $client->probe();
        $settingsRepo->touchConnectionTest($businessId);
        Http::respond(['ok'=>$probeError === null,'whatsapp'=>WhatsAppStatusService::project($settings, $config, $probeError)], $probeError === null ? 200 : 503);
    }

    if ($method === 'PUT' && route_ends_with($path, '/business/profile')) {
        $updated = $settingsRepo->updateProfile($businessId, Http::jsonBody());
        Http::respond(['ok'=>true,'business'=>[
            'business_name'=>$updated['business_name'],'phone'=>$updated['phone'],'address'=>$updated['address'],'timezone'=>$updated['timezone']
        ]]);
    }

    if ($method === 'PUT' && route_ends_with($path, '/business/whatsapp-settings')) {
        $updated = $settingsRepo->updateWhatsApp($businessId, Http::jsonBody());
        if (!(bool)$updated['whatsapp_enabled']) {
            (new ReminderQueueRepository($pdo))->cancelBusiness($businessId);
        }
        Http::respond(['ok'=>true,'whatsapp'=>WhatsAppStatusService::project($updated, $config, null)]);
    }

    if (preg_match('#/appointments/([^/]+)/reminders$#', $path, $match)) {
        $appointmentId = rawurldecode($match[1]);
        $queue = new ReminderQueueRepository($pdo);
        if ($method === 'DELETE') {
            $queue->cancelAppointment($businessId, $appointmentId);
            Http::respond(['ok'=>true,'appointment_id'=>$appointmentId,'scheduled'=>0]);
        }
        if ($method === 'PUT') {
            $payload = Http::jsonBody();
            $payload['id'] = $appointmentId;
            $rows = $queue->replaceAppointment($businessId, $payload, [
                'enabled'=>(bool)$settings['whatsapp_enabled'],
                'reminder_24h'=>(bool)$settings['reminder_24h'],
                'reminder_2h'=>(bool)$settings['reminder_2h'],
                'timezone'=>(string)$settings['timezone'],
                'business_name'=>(string)$settings['business_name'],
                'template_name'=>(string)$settings['template_name'],
                'template_language'=>(string)$settings['template_language'],
            ]);
            Http::respond(['ok'=>true,'appointment_id'=>$appointmentId,'scheduled'=>count($rows),'reminders'=>array_map(fn(array $row): array => [
                'offset_minutes'=>$row['offset_minutes'],'scheduled_at_utc'=>$row['scheduled_at_utc']
            ], $rows)]);
        }
    }

    Http::error('endpoint_not_found', 404);
} catch (RuntimeException $e) {
    $error = $e->getMessage();
    $status = $error === 'unauthorized' ? 401 : ($error === 'invalid_setup_key' ? 403 : 400);
    Http::error($error, $status);
} catch (Throwable $e) {
    error_log('Randevu backend error: ' . get_class($e));
    Http::error('server_error', 500, 'Sunucu işlemi tamamlanamadı.');
}
