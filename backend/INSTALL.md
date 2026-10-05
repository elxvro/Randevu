# Randevu v2.0 Core — Shared Hosting Kurulum

## Gereksinimler

- PHP 8.1 veya üstü
- MySQL/MariaDB, InnoDB ve utf8mb4
- HTTPS
- Cron desteği
- Apache rewrite desteği önerilir

## 1. Dosya yerleşimi

`backend/public/` içeriği web kökünden erişilebilir olmalıdır. `backend/src/`, `backend/config.php` ve migration dosyalarını mümkünse public web root dışında tutun.

Örnek:

- `/home/USER/randevu/backend/src/`
- `/home/USER/randevu/backend/config.php`
- `/home/USER/public_html/randevu/` → `backend/public/` içeriği

Hosting buna izin vermiyorsa tüm `backend/` klasörünü yükleyebilirsiniz; ancak `src`, `tests`, `migrations` ve config dosyalarının web üzerinden doğrudan sunulmasını sunucu kuralıyla kapatın.

## 2. Veritabanı

Yeni kurulum için `backend/schema.sql` dosyasını içe aktarın.

v1.3.1 veritabanından yükseltme için önce tam DB yedeği alın, ardından:

`backend/migrations/2026_10_04_v2_core.sql`

dosyasını bir kez çalıştırın.

Migration tamamlanmadan v2 APK'yı üretim hesabına bağlamayın.

## 3. Config

`backend/config.example.php` dosyasını `backend/config.php` olarak kopyalayın ve gerçek değerleri yalnızca sunucuda tanımlayın.

Gerekli DB değerleri:

- `RANDEVU_DB_HOST`
- `RANDEVU_DB_PORT`
- `RANDEVU_DB_NAME`
- `RANDEVU_DB_USER`
- `RANDEVU_DB_PASS`

Uyumluluk / servis değerleri:

- `RANDEVU_APP_SETUP_KEY` — yalnızca v1.3 uyumluluk bootstrap akışı için
- `RANDEVU_CRON_KEY` — cron/operasyon güvenliği için
- `META_ACCESS_TOKEN` — Meta sunucu erişim anahtarı
- `META_PHONE_NUMBER_ID`
- `META_GRAPH_VERSION`

Meta erişim anahtarını APK, JavaScript, HTML veya Git deposuna koymayın.

## 4. HTTPS ve Android bağlantısı

Android v2 ilk giriş ekranında API temel adresini HTTPS olarak girin. Örnek:

`https://example.com/randevu`

Uygulama HTTP adreslerini kabul etmez.

## 5. Public müşteri randevu sayfası

Apache rewrite aktif olduğunda işletmenin public sayfası:

`https://example.com/randevu/r/{business-slug}`

Public akış hizmet → personel → tarih → uygun saat → müşteri adı/telefonu → onay şeklindedir.

## 6. WhatsApp cron

WhatsApp gönderici dosyası:

`backend/cron/send_whatsapp_reminders.php`

Önerilen cron sıklığı: 5 dakika.

Hosting panelinde PHP CLI yolu farklı olabilir. Örnek:

`*/5 * * * * /usr/bin/php /home/USER/randevu/backend/cron/send_whatsapp_reminders.php >/dev/null 2>&1`

Cron yolu hosting sağlayıcınızın PHP CLI yoluna göre değiştirilmelidir.

## 7. Sağlık kontrolü

Kurulumdan sonra:

- `GET /health`
- `GET /version`

yanıtlarının `2.0.0` göstermesini doğrulayın.

## 8. Rollback

Sorun durumunda:

1. v2 dosyalarını geri alın.
2. Migration öncesi DB yedeğini geri yükleyin.
3. Eski APK'yı kullanın.

Canlı veritabanında migration'ı ters SQL ile manuel sökmek yerine yedekten dönmek daha güvenlidir.
