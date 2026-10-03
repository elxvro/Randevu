<?php

declare(strict_types=1);

final class SendResult
{
    public function __construct(
        public bool $ok,
        public bool $transient,
        public string $messageId,
        public string $error
    ) {}

    public static function success(string $messageId): self { return new self(true, false, $messageId, ''); }
    public static function transientFailure(string $error): self { return new self(false, true, '', $error); }
    public static function permanentFailure(string $error): self { return new self(false, false, '', $error); }
}

interface MetaWhatsAppClientContract
{
    public function sendTemplate(string $recipient, string $template, string $language, array $parameters): SendResult;
}

final class MetaWhatsAppClient implements MetaWhatsAppClientContract
{
    public function __construct(private array $config, private ?string $phoneNumberIdOverride = null) {}

    public function sendTemplate(string $recipient, string $template, string $language, array $parameters): SendResult
    {
        $token = trim((string)($this->config['meta_access_token'] ?? ''));
        $phoneId = trim((string)($this->phoneNumberIdOverride ?: ($this->config['meta_phone_number_id'] ?? '')));
        $version = trim((string)($this->config['meta_graph_version'] ?? ''));
        if ($token === '' || $phoneId === '' || $version === '') return SendResult::permanentFailure('meta_configuration_incomplete');
        $components = [[
            'type' => 'body',
            'parameters' => array_map(fn($value) => ['type'=>'text','text'=>(string)$value], array_values($parameters)),
        ]];
        $body = [
            'messaging_product' => 'whatsapp',
            'to' => $recipient,
            'type' => 'template',
            'template' => ['name'=>$template,'language'=>['code'=>$language],'components'=>$components],
        ];
        $response = $this->request('POST', "https://graph.facebook.com/{$version}/{$phoneId}/messages", $token, $body);
        if ($response['code'] >= 200 && $response['code'] < 300) {
            $id = (string)($response['json']['messages'][0]['id'] ?? '');
            return $id !== '' ? SendResult::success($id) : SendResult::permanentFailure('meta_message_id_missing');
        }
        $error = (string)($response['json']['error']['message'] ?? ('meta_http_' . $response['code']));
        $transient = $response['code'] === 429 || $response['code'] >= 500 || $response['code'] === 0;
        return $transient ? SendResult::transientFailure($error) : SendResult::permanentFailure($error);
    }

    public function probe(): ?string
    {
        $token = trim((string)($this->config['meta_access_token'] ?? ''));
        $phoneId = trim((string)($this->phoneNumberIdOverride ?: ($this->config['meta_phone_number_id'] ?? '')));
        $version = trim((string)($this->config['meta_graph_version'] ?? ''));
        if ($token === '' || $phoneId === '' || $version === '') return 'meta_configuration_incomplete';
        $response = $this->request('GET', "https://graph.facebook.com/{$version}/{$phoneId}?fields=id", $token, null);
        if ($response['code'] >= 200 && $response['code'] < 300) return null;
        return (string)($response['json']['error']['message'] ?? ('meta_http_' . $response['code']));
    }

    private function request(string $method, string $url, string $token, ?array $body): array
    {
        $ch = curl_init($url);
        $headers = ['Accept: application/json', 'Authorization: Bearer ' . $token];
        $options = [CURLOPT_RETURNTRANSFER=>true,CURLOPT_CONNECTTIMEOUT=>8,CURLOPT_TIMEOUT=>15,CURLOPT_CUSTOMREQUEST=>$method,CURLOPT_HTTPHEADER=>$headers];
        if ($body !== null) {
            $headers[] = 'Content-Type: application/json';
            $options[CURLOPT_HTTPHEADER] = $headers;
            $options[CURLOPT_POSTFIELDS] = json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        }
        curl_setopt_array($ch, $options);
        $raw = curl_exec($ch);
        $code = $raw === false ? 0 : (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        $curlError = $raw === false ? curl_error($ch) : '';
        curl_close($ch);
        $json = is_string($raw) ? json_decode($raw, true) : null;
        if (!is_array($json)) $json = $curlError !== '' ? ['error'=>['message'=>$curlError]] : [];
        return ['code'=>$code,'json'=>$json];
    }
}
