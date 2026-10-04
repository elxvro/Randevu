<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/src/bootstrap.php';

$slug = trim((string)($_GET['slug'] ?? ''));
$catalog = null;
$error = null;
try {
    $config = Config::load();
    $pdo = Database::pdo($config);
    $appointmentStore = new V2AppointmentRepository($pdo);
    $reminders = new V2ReminderQueueScheduler(
        new BusinessSettingsRepository($pdo),
        new ReminderQueueRepository($pdo)
    );
    $service = new PublicBookingService(
        $pdo,
        new V2AppointmentService($appointmentStore, $reminders),
        new RateLimiter($pdo)
    );
    $catalog = $service->catalog($slug);
} catch (Throwable) {
    http_response_code(404);
    $error = 'İşletme bulunamadı veya randevu sayfası şu anda kullanılamıyor.';
}

$business = $catalog['business'] ?? ['name'=>'Randevu','address'=>'','phone'=>''];
?><!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
  <meta name="color-scheme" content="dark">
  <title><?= htmlspecialchars((string)$business['name'], ENT_QUOTES, 'UTF-8') ?> — Online Randevu</title>
  <link rel="stylesheet" href="/assets/v2-booking.css">
</head>
<body data-slug="<?= htmlspecialchars($slug, ENT_QUOTES, 'UTF-8') ?>">
<main class="shell">
  <header class="hero">
    <div class="mark">R</div>
    <div>
      <p class="eyebrow">ONLINE RANDEVU</p>
      <h1><?= htmlspecialchars((string)$business['name'], ENT_QUOTES, 'UTF-8') ?></h1>
      <?php if (($business['address'] ?? '') !== ''): ?>
        <p class="muted"><?= htmlspecialchars((string)$business['address'], ENT_QUOTES, 'UTF-8') ?></p>
      <?php endif; ?>
    </div>
  </header>

  <?php if ($error !== null): ?>
    <section class="card error"><h2>Randevu alınamıyor</h2><p><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p></section>
  <?php else: ?>
    <section class="card">
      <div class="step"><span>1</span><div><h2>Hizmet</h2><p>Almak istediğiniz hizmeti seçin.</p></div></div>
      <select id="service" aria-label="Hizmet">
        <option value="">Hizmet seçin</option>
        <?php foreach ($catalog['services'] as $item): ?>
          <option value="<?= htmlspecialchars((string)$item['external_id'], ENT_QUOTES, 'UTF-8') ?>">
            <?= htmlspecialchars((string)$item['name'], ENT_QUOTES, 'UTF-8') ?> · <?= (int)$item['duration_minutes'] ?> dk
          </option>
        <?php endforeach; ?>
      </select>
    </section>

    <section class="card">
      <div class="step"><span>2</span><div><h2>Personel</h2><p>Randevunuzu gerçekleştirecek kişiyi seçin.</p></div></div>
      <select id="staff" aria-label="Personel">
        <option value="">Personel seçin</option>
        <?php foreach ($catalog['staff'] as $item): ?>
          <option value="<?= htmlspecialchars((string)$item['external_id'], ENT_QUOTES, 'UTF-8') ?>">
            <?= htmlspecialchars((string)$item['name'], ENT_QUOTES, 'UTF-8') ?><?= ($item['title'] ?? '') !== '' ? ' · '.htmlspecialchars((string)$item['title'], ENT_QUOTES, 'UTF-8') : '' ?>
          </option>
        <?php endforeach; ?>
      </select>
    </section>

    <section class="card">
      <div class="step"><span>3</span><div><h2>Tarih ve saat</h2><p>Uygun saatler işletmenin takviminden canlı hesaplanır.</p></div></div>
      <input id="date" type="date" aria-label="Tarih">
      <div id="slots" class="slots" aria-live="polite"></div>
    </section>

    <section class="card">
      <div class="step"><span>4</span><div><h2>Bilgileriniz</h2><p>Randevu ve hatırlatma için kullanılır.</p></div></div>
      <input id="customerName" autocomplete="name" placeholder="Adınız">
      <input id="customerPhone" inputmode="tel" autocomplete="tel" placeholder="Telefon">
      <textarea id="note" rows="2" placeholder="Not (isteğe bağlı)"></textarea>
      <button id="book" type="button">Randevuyu Oluştur</button>
      <p id="feedback" class="feedback" aria-live="polite"></p>
    </section>
  <?php endif; ?>
</main>
<?php if ($error === null): ?><script src="/assets/v2-booking.js" defer></script><?php endif; ?>
</body>
</html>
