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

function v2_ip(): string {
    $raw = trim((string)($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0'));
    return $raw !== '' ? substr($raw,0,64) : '0.0.0.0';
}

function v2_expected_version(array $payload): int {
    $version = (int)($payload['expected_version'] ?? 0);
    if ($version < 1) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
    return $version;
}

function v2_body_without_version(array $payload): array {
    unset($payload['expected_version'],$payload['business_id']);
    return $payload;
}

try {
    if ($method === 'GET' && route_ends_with($path, '/health')) {
        Http::respond(['ok'=>true,'service'=>'Randevu API','version'=>'2.0.0','php'=>PHP_VERSION,'time'=>gmdate('c')]);
    }
    if ($method === 'GET' && route_ends_with($path, '/version')) {
        Http::respond(['ok'=>true,'service'=>'Randevu API','version'=>'2.0.0']);
    }

    $config = Config::load();
    $pdo = Database::pdo($config);

    // v2 public authentication.
    if ($method === 'POST' && route_ends_with($path, '/v2/auth/register-owner')) {
        $result = V2Auth::registerOwner($pdo,new RateLimiter($pdo),Http::jsonBody(),v2_ip());
        Http::respond(['ok'=>true,'session'=>$result],201);
    }
    if ($method === 'POST' && route_ends_with($path, '/v2/auth/login')) {
        $result = V2Auth::login($pdo,new RateLimiter($pdo),Http::jsonBody(),v2_ip());
        Http::respond(['ok'=>true,'session'=>$result]);
    }

    // v2 public booking endpoints.
    if (str_contains($path, '/v2/public/')) {
        $appointmentRepo = new V2AppointmentRepository($pdo);
        $reminderScheduler = new V2ReminderQueueScheduler(
            new BusinessSettingsRepository($pdo),
            new ReminderQueueRepository($pdo)
        );
        $public = new PublicBookingService(
            $pdo,
            new V2AppointmentService($appointmentRepo,$reminderScheduler),
            new RateLimiter($pdo)
        );

        if ($method === 'GET' && preg_match('#/v2/public/business/([a-z0-9-]+)$#',$path,$match)) {
            Http::respond(['ok'=>true,'catalog'=>$public->catalog($match[1])]);
        }
        if ($method === 'GET' && route_ends_with($path, '/v2/public/availability')) {
            $slots=$public->availability(
                (string)($_GET['slug'] ?? ''),
                (string)($_GET['service'] ?? ''),
                (string)($_GET['staff'] ?? ''),
                (string)($_GET['date'] ?? '')
            );
            Http::respond(['ok'=>true,'slots'=>$slots]);
        }
        if ($method === 'POST' && route_ends_with($path, '/v2/public/bookings')) {
            Http::respond(['ok'=>true] + $public->book(Http::jsonBody(),v2_ip()),201);
        }
        Http::error(V2ApiError::NOT_FOUND,404,'İstenen kaynak bulunamadı.');
    }

    // v2 private owner endpoints.
    if (str_contains($path, '/v2/')) {
        $auth = V2Auth::requireOwner($pdo);
        $businessId = (int)$auth['business_id'];

        if ($method === 'POST' && route_ends_with($path, '/v2/auth/logout')) {
            $header=$_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null;
            $raw=Auth::extractBearer(is_string($header)?$header:null);
            V2Auth::logout($pdo,$raw ?? '');
            Http::respond(['ok'=>true]);
        }
        if ($method === 'GET' && route_ends_with($path, '/v2/me')) {
            Http::respond(['ok'=>true,'owner'=>[
                'owner_name'=>(string)$auth['name'],
                'email'=>(string)$auth['email'],
                'business_slug'=>(string)$auth['business_slug'],
            ]]);
        }

        $businessRepo=new V2BusinessRepository($pdo);
        $catalogRepo=new V2CatalogRepository($pdo);
        $staffRepo=new V2StaffRepository($pdo);
        $tenant=new V2TenantService($businessRepo,$catalogRepo,$staffRepo);

        if ($method === 'GET' && route_ends_with($path, '/v2/business')) {
            Http::respond(['ok'=>true,'business'=>$tenant->business($businessId)]);
        }
        if ($method === 'PUT' && route_ends_with($path, '/v2/business')) {
            $body=Http::jsonBody();
            Http::respond(['ok'=>true,'business'=>$tenant->updateBusiness($businessId,v2_body_without_version($body),v2_expected_version($body))]);
        }

        if ($method === 'GET' && route_ends_with($path, '/v2/services')) {
            Http::respond(['ok'=>true,'services'=>$tenant->listServices($businessId)]);
        }
        if ($method === 'POST' && route_ends_with($path, '/v2/services')) {
            Http::respond(['ok'=>true,'service'=>$tenant->createService($businessId,Http::jsonBody())],201);
        }
        if (preg_match('#/v2/services/([^/]+)$#',$path,$match)) {
            $external=rawurldecode($match[1]);
            $body=Http::jsonBody();
            if($method==='PUT') Http::respond(['ok'=>true,'service'=>$tenant->updateService($businessId,$external,v2_body_without_version($body),v2_expected_version($body))]);
            if($method==='DELETE'){ $tenant->deleteService($businessId,$external,v2_expected_version($body)); Http::respond(['ok'=>true]); }
        }

        if ($method === 'GET' && route_ends_with($path, '/v2/staff')) {
            Http::respond(['ok'=>true,'staff'=>$tenant->listStaff($businessId)]);
        }
        if ($method === 'POST' && route_ends_with($path, '/v2/staff')) {
            Http::respond(['ok'=>true,'staff_member'=>$tenant->createStaff($businessId,Http::jsonBody())],201);
        }
        if ($method === 'GET' && route_ends_with($path, '/v2/staff-leaves')) {
            Http::respond(['ok'=>true,'staff_leaves'=>$tenant->listLeaves($businessId)]);
        }
        if ($method === 'POST' && route_ends_with($path, '/v2/staff-leaves')) {
            Http::respond(['ok'=>true,'staff_leave'=>$tenant->createLeave($businessId,Http::jsonBody())],201);
        }
        if (preg_match('#/v2/staff-leaves/([^/]+)$#',$path,$match)) {
            $external=rawurldecode($match[1]);
            $body=Http::jsonBody();
            if($method==='PUT') Http::respond(['ok'=>true,'staff_leave'=>$tenant->updateLeave($businessId,$external,v2_body_without_version($body),v2_expected_version($body))]);
            if($method==='DELETE'){ $tenant->deleteLeave($businessId,$external,v2_expected_version($body)); Http::respond(['ok'=>true]); }
        }
        if (preg_match('#/v2/staff/([^/]+)$#',$path,$match)) {
            $external=rawurldecode($match[1]);
            $body=Http::jsonBody();
            if($method==='PUT') Http::respond(['ok'=>true,'staff_member'=>$tenant->updateStaff($businessId,$external,v2_body_without_version($body),v2_expected_version($body))]);
            if($method==='DELETE'){ $tenant->deleteStaff($businessId,$external,v2_expected_version($body)); Http::respond(['ok'=>true]); }
        }

        $appointmentRepo = new V2AppointmentRepository($pdo);
        $appointmentService = new V2AppointmentService(
            $appointmentRepo,
            new V2ReminderQueueScheduler(new BusinessSettingsRepository($pdo),new ReminderQueueRepository($pdo))
        );
        if ($method === 'GET' && route_ends_with($path, '/v2/appointments')) {
            $from=(string)($_GET['from'] ?? '');
            $to=(string)($_GET['to'] ?? '');
            Http::respond(['ok'=>true,'appointments'=>$appointmentService->list($businessId,$from,$to)]);
        }
        if ($method === 'POST' && route_ends_with($path, '/v2/appointments')) {
            Http::respond(['ok'=>true,'appointment'=>$appointmentService->create($businessId,Http::jsonBody(),'android')],201);
        }
        if (preg_match('#/v2/appointments/([^/]+)$#',$path,$match)) {
            $external=rawurldecode($match[1]);
            $body=Http::jsonBody();
            if($method==='PUT') Http::respond(['ok'=>true,'appointment'=>$appointmentService->update($businessId,$external,v2_body_without_version($body),v2_expected_version($body))]);
            if($method==='DELETE') Http::respond(['ok'=>true,'appointment'=>$appointmentService->cancel($businessId,$external,v2_expected_version($body))]);
        }

        if ($method === 'GET' && route_ends_with($path, '/v2/sync/bootstrap')) {
            $business=$businessRepo->get($businessId);
            $store=new V2PdoSyncStore($pdo,$businessRepo,$catalogRepo,$staffRepo,$appointmentRepo);
            $payload=(new V2SyncService($store))->bootstrap(
                $businessId,
                new DateTimeImmutable('now',new DateTimeZone('UTC')),
                (string)$business['timezone']
            );
            Http::respond(['ok'=>true] + $payload);
        }

        Http::error(V2ApiError::NOT_FOUND,404,'İstenen kaynak bulunamadı.');
    }

    // v1.3 compatibility endpoints stay available during the v2 rollout.
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
} catch (V2VersionConflict $e) {
    Http::respond(['ok'=>false,'error'=>V2ApiError::VERSION_CONFLICT,'message'=>'Kayıt başka bir cihazda değiştirildi.','current'=>$e->current],409);
} catch (RuntimeException $e) {
    $error = $e->getMessage();
    $status = match ($error) {
        V2ApiError::UNAUTHORIZED, V2ApiError::INVALID_CREDENTIALS, 'unauthorized' => 401,
        'invalid_setup_key' => 403,
        V2ApiError::NOT_FOUND => 404,
        V2ApiError::EMAIL_IN_USE, V2ApiError::VERSION_CONFLICT, V2ApiError::SLOT_UNAVAILABLE => 409,
        V2ApiError::RATE_LIMITED => 429,
        V2ApiError::VALIDATION_ERROR => 422,
        default => 400,
    };
    $message = match ($error) {
        V2ApiError::INVALID_CREDENTIALS => 'E-posta veya şifre hatalı.',
        V2ApiError::EMAIL_IN_USE => 'Bu hesap bilgileri kullanılamıyor.',
        V2ApiError::SLOT_UNAVAILABLE => 'Bu randevu saati artık uygun değil.',
        V2ApiError::RATE_LIMITED => 'Çok fazla istek gönderildi. Bir süre sonra tekrar deneyin.',
        V2ApiError::VALIDATION_ERROR => 'Gönderilen bilgileri kontrol edin.',
        default => $error,
    };
    Http::error($error,$status,$message);
} catch (Throwable $e) {
    error_log('Randevu backend error: ' . get_class($e));
    Http::error('server_error', 500, 'Sunucu işlemi tamamlanamadı.');
}
