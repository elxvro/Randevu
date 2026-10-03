<?php

declare(strict_types=1);

final class BusinessSettingsRepository
{
    public function __construct(private PDO $pdo) {}

    public function get(int $businessId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT b.id,b.name AS business_name,b.phone,b.address,b.timezone,COALESCE(s.whatsapp_enabled,0) AS whatsapp_enabled,COALESCE(s.reminder_24h,1) AS reminder_24h,COALESCE(s.reminder_2h,1) AS reminder_2h,COALESCE(s.template_name,\'appointment_reminder\') AS template_name,COALESCE(s.template_language,\'tr\') AS template_language,s.meta_phone_number_id,s.last_connection_test_at FROM businesses b LEFT JOIN business_settings s ON s.business_id=b.id WHERE b.id=:id'
        );
        $stmt->execute(['id'=>$businessId]);
        $row = $stmt->fetch();
        if (!$row) throw new RuntimeException('business_not_found');
        return $row + [
            'enabled'=>(bool)$row['whatsapp_enabled'],
            'reminder_24h'=>(bool)$row['reminder_24h'],
            'reminder_2h'=>(bool)$row['reminder_2h'],
        ];
    }

    public function updateProfile(int $businessId, array $payload): array
    {
        $name = trim((string)($payload['business_name'] ?? $payload['name'] ?? ''));
        $timezone = trim((string)($payload['timezone'] ?? ''));
        if ($name === '' || $timezone === '') throw new RuntimeException('invalid_business_profile');
        try { new DateTimeZone($timezone); } catch (Throwable) { throw new RuntimeException('invalid_timezone'); }
        $stmt = $this->pdo->prepare('UPDATE businesses SET name=:name,phone=:phone,address=:address,timezone=:timezone WHERE id=:id');
        $stmt->execute(['name'=>$name,'phone'=>trim((string)($payload['phone'] ?? '')),'address'=>trim((string)($payload['address'] ?? '')),'timezone'=>$timezone,'id'=>$businessId]);
        return $this->get($businessId);
    }

    public function updateWhatsApp(int $businessId, array $payload): array
    {
        $template = trim((string)($payload['template_name'] ?? 'appointment_reminder'));
        $language = trim((string)($payload['template_language'] ?? 'tr'));
        if ($template === '' || $language === '') throw new RuntimeException('invalid_whatsapp_settings');
        $phoneId = trim((string)($payload['meta_phone_number_id'] ?? '')) ?: null;
        $sql = 'INSERT INTO business_settings(business_id,whatsapp_enabled,reminder_24h,reminder_2h,template_name,template_language,meta_phone_number_id) VALUES(:business,:enabled,:r24,:r2,:template,:language,:phone_id) ON DUPLICATE KEY UPDATE whatsapp_enabled=VALUES(whatsapp_enabled),reminder_24h=VALUES(reminder_24h),reminder_2h=VALUES(reminder_2h),template_name=VALUES(template_name),template_language=VALUES(template_language),meta_phone_number_id=VALUES(meta_phone_number_id)';
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            'business'=>$businessId,'enabled'=>(int)(bool)($payload['enabled'] ?? false),
            'r24'=>(int)(bool)($payload['reminder_24h'] ?? true),'r2'=>(int)(bool)($payload['reminder_2h'] ?? true),
            'template'=>$template,'language'=>$language,'phone_id'=>$phoneId,
        ]);
        return $this->get($businessId);
    }

    public function touchConnectionTest(int $businessId): void
    {
        $stmt = $this->pdo->prepare('UPDATE business_settings SET last_connection_test_at=UTC_TIMESTAMP() WHERE business_id=:id');
        $stmt->execute(['id'=>$businessId]);
    }
}
