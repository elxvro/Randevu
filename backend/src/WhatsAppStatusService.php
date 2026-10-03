<?php

declare(strict_types=1);

final class WhatsAppStatusService
{
    public static function project(array $settings, array $config, ?string $probeError): array
    {
        $enabled = (bool) ($settings['enabled'] ?? $settings['whatsapp_enabled'] ?? false);
        $token = trim((string) ($config['meta_access_token'] ?? ''));
        $phoneId = trim((string) ($settings['meta_phone_number_id'] ?? $config['meta_phone_number_id'] ?? ''));
        $template = trim((string) ($settings['template_name'] ?? 'appointment_reminder'));
        $language = trim((string) ($settings['template_language'] ?? 'tr'));

        $state = match (true) {
            !$enabled => 'disabled',
            $token === '' || $phoneId === '' || $template === '' || $language === '' => 'incomplete',
            $probeError !== null && $probeError !== '' => 'unreachable',
            default => 'connected',
        };

        return [
            'state' => $state,
            'enabled' => $enabled,
            'reminder_24h' => (bool) ($settings['reminder_24h'] ?? true),
            'reminder_2h' => (bool) ($settings['reminder_2h'] ?? true),
            'template_name' => $template,
            'template_language' => $language,
            'phone_number_configured' => $phoneId !== '',
            'error' => $probeError,
        ];
    }
}
