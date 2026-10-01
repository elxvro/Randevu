# Randevu Android UI Design

## Amaç
İlk APK, backend olmadan çalışan ve nihai ürünün görsel/etkileşim temelini doğrulayan profesyonel bir Android randevu uygulaması olacaktır. PHP/MySQL entegrasyonu bu aşamada kapsam dışıdır.

## Kullanıcı Akışları
1. Müşteri ana ekranı: işletme ve kategori keşfi.
2. İşletme detayı: hizmetler ve işletme özeti.
3. Personel seçimi.
4. Tarih ve saat seçimi.
5. Randevu özeti.
6. Başarılı randevu onayı.
7. Randevularım ekranı.
8. Profil ekranı.
9. İşletme paneli ana görünümü.
10. İşletme menüsü.

## Tasarım İlkeleri
- Minimal, profesyonel ve mobil öncelikli görünüm.
- Material 3 ve Jetpack Compose.
- Açık tema ana tema; işletme menüsünde koyu yüzey kullanılabilir.
- Mavi ana vurgu rengi, yuvarlatılmış kartlar, tutarlı boşluk sistemi.
- Türkçe arayüz metinleri.
- Demo veriler yerel ve deterministik tutulur.

## Teknik Mimari
- Tek Activity + Jetpack Compose.
- Ekranlar uygulama içi state tabanlı yönlendirme ile açılır.
- `BookingDraft` seçilen işletme, hizmet, personel, tarih ve saati taşır.
- `BookingReducer` gibi saf Kotlin mantığı UI'dan ayrılır ve unit test ile doğrulanır.
- Ağ katmanı, veritabanı ve kimlik doğrulama yoktur.

## Paket ve Sürümler
- Paket adı: `com.elxvro.randevu`
- Sürüm: `0.1.0`
- minSdk: 26
- targetSdk/compileSdk: 35
- Java: 17

## Kapsam Dışı
- PHP/MySQL API
- Google ile giriş
- Gerçek bildirim
- Ödeme
- Harita/konum servisleri
- Gerçek fotoğraf indirme veya yükleme

## Build
GitHub Actions, debug APK üretir ve `randevu-debug-apk` artifact'i olarak yükler.
