# Randevu v1.3.0 WhatsApp Reminder Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add official WhatsApp Business Cloud API reminder automation through a PHP/MySQL backend, with idempotent 24h/2h reminder queueing and Android synchronization that never exposes Meta secrets in the APK.

**Architecture:** Expand the existing PHP entry point into a small authenticated API backed by PDO repositories, add a MySQL reminder queue and a cron-safe sender that calls Meta Graph API using server-only configuration, then bridge appointment/business changes from Android to those endpoints. Local Android reminders remain independent and continue to function when the server is unavailable.

**Tech Stack:** PHP 8.1+, MySQL/InnoDB, PDO prepared statements, cURL, cron, Kotlin/HttpURLConnection, JSON, JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-03-business-setup-whatsapp-design.md`

## Global Constraints

- Meta access token is backend-only, never committed to Git and never stored in Android.
- Remote API URLs are HTTPS-only.
- WhatsApp reminder offsets are 1440 and 120 minutes.
- Queue uniqueness is `(appointment_external_id, offset_minutes)`.
- Cancelled/completed/deleted appointments do not produce future WhatsApp sends.
- Backend stores schedule times in UTC and converts appointment local time using the owner business timezone.
- Appointment creation must still succeed locally when backend/WhatsApp is unavailable.
- Android UI must never claim a WhatsApp reminder is scheduled unless the backend acknowledged it.
- Recipient phones are normalized to international/E.164-like form before enqueue.
- Release target is `1.3.0`, versionCode `13`.

## Review Focus

- Duplicate appointment edits must update the same queue rows rather than create duplicate sends.
- Cron retries must not send a second message after a successful Meta response if the process crashes during bookkeeping.
- Invalid timezone/phone/template configuration must fail closed with diagnostic status, not enqueue malformed sends.
- Bearer/application auth must scope reminder mutation to the owning business.
- Logs/errors must never contain the Meta access token or full Authorization header.

---

### Task 1: Backend schema and secure configuration contract

**Files:**
- Modify: `backend/schema.sql`
- Modify: `backend/config.example.php`
- Create: `backend/src/Config.php`
- Create: `backend/src/Database.php`
- Create: `backend/tests/schema_contract.php`

**Interfaces:**
- Adds `business_settings` and `whatsapp_reminder_queue` tables defined by the approved spec.
- `Config::load(): array` exposes DB settings plus non-checked-in `META_ACCESS_TOKEN`, `META_PHONE_NUMBER_ID`, `META_GRAPH_VERSION` from server config/environment.
- `Database::pdo(array $config): PDO` uses exception mode and utf8mb4.

- [ ] **Step 1: Write failing deterministic schema/config contract check**

Assert required tables/columns/unique queue key exist in `schema.sql`; config example contains placeholders but no production token literal.

- [ ] **Step 2: Run checks to verify RED**

Run: `php backend/tests/schema_contract.php`
Expected: FAIL before new schema/config contract exists.

- [ ] **Step 3: Extend schema and add config/database bootstrap**

Use InnoDB, prepared-statement-compatible column types, UTC-capable datetime fields, bounded status enum, retry fields, and indexes for due-pending rows.

- [ ] **Step 4: Run PHP syntax + schema contract**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && php backend/tests/schema_contract.php`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add whatsapp reminder backend schema`

### Task 2: Authentication and request primitives

**Files:**
- Create: `backend/src/Http.php`
- Create: `backend/src/Auth.php`
- Create: `backend/src/Phone.php`
- Create: `backend/tests/auth_phone_test.php`
- Modify: `backend/public/index.php`

**Interfaces:**
- `Auth::requireBusiness(PDO $pdo): array` validates bearer/application token and returns owning user/business identity.
- `Phone::normalize(string $raw, string $defaultCountryCode = '90'): ?string` returns digits-only international form suitable for Cloud API or null.
- `Http` centralizes JSON body parsing/response/error output without leaking secrets.

- [ ] **Step 1: Write failing auth/phone pure tests**

Cover `+90`, `0090`, Turkish local `0XXXXXXXXXX`, already-international numbers, malformed phones, missing/invalid bearer token parsing.

- [ ] **Step 2: Run focused test to verify RED**

Run: `php backend/tests/auth_phone_test.php`
Expected: FAIL before helpers exist.

- [ ] **Step 3: Implement helpers and route bootstrap**

Token validation hashes bearer token before querying `api_tokens`; all PDO queries are prepared statements. Keep `/health` and `/version` unauthenticated.

- [ ] **Step 4: Run syntax and focused tests**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && php backend/tests/auth_phone_test.php`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add backend auth and phone normalization`

### Task 3: Business profile and WhatsApp status endpoints

**Files:**
- Create: `backend/src/BusinessSettingsRepository.php`
- Create: `backend/src/WhatsAppStatusService.php`
- Modify: `backend/public/index.php`
- Create: `backend/tests/whatsapp_status_test.php`

**Interfaces:**
- Routes: `GET /whatsapp/status`, `POST /whatsapp/test-connection`, `PUT /business/profile`, `PUT /business/whatsapp-settings`.
- `WhatsAppStatusService` returns one of `connected`, `incomplete`, `unreachable`, `disabled` plus nonsecret diagnostic text.
- Test-connection is rate-limited and does not send a customer message.

- [ ] **Step 1: Write failing status projection/request-validation tests**

Pin states for disabled, missing phone-number ID/token, configured-but-client-failure, and connected; verify response projection never contains access token.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `php backend/tests/whatsapp_status_test.php`
Expected: FAIL before service/routes exist.

- [ ] **Step 3: Implement repositories/routes/status service**

Profile/settings writes are scoped to authenticated business; template name/language and enable flags are persisted, while secret token remains server config only.

- [ ] **Step 4: Run syntax and backend tests**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && php backend/tests/whatsapp_status_test.php`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add whatsapp configuration endpoints`

### Task 4: Idempotent reminder queue repository and API

**Files:**
- Create: `backend/src/ReminderSchedule.php`
- Create: `backend/src/ReminderQueueRepository.php`
- Modify: `backend/public/index.php`
- Create: `backend/tests/reminder_schedule_test.php`

**Interfaces:**
- Routes: `PUT /appointments/{id}/reminders`, `DELETE /appointments/{id}/reminders`.
- `ReminderSchedule::build(array $appointment, array $settings, DateTimeImmutable $nowUtc): array` returns eligible 1440/120-minute queue rows.
- `ReminderQueueRepository::upsertForAppointment(...)` performs unique-key upsert/reschedule.
- Cancellation route marks pending/processing-safe rows cancelled for that owned appointment.

- [ ] **Step 1: Write failing schedule/idempotency tests**

Cover business timezone conversion, future 24h+2h entries, independently skipped past offsets, cancelled/completed statuses returning no rows, invalid phone/timezone, and deterministic unique keys.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `php backend/tests/reminder_schedule_test.php`
Expected: FAIL before schedule/repository exists.

- [ ] **Step 3: Implement pure schedule builder, queue upsert, ownership validation, and routes**

Use one transaction for appointment reminder replacement/upsert; never create duplicate `(appointment_external_id, offset_minutes)` rows.

- [ ] **Step 4: Run syntax and backend tests**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && php backend/tests/reminder_schedule_test.php`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add idempotent whatsapp reminder queue`

### Task 5: Meta client and cron sender

**Files:**
- Create: `backend/src/MetaWhatsAppClient.php`
- Create: `backend/src/ReminderSender.php`
- Create: `backend/cron/send_whatsapp_reminders.php`
- Create: `backend/tests/reminder_sender_test.php`

**Interfaces:**
- `MetaWhatsAppClient::sendTemplate(string $recipient, string $template, string $language, array $parameters): SendResult`.
- `ReminderSender::run(int $batchSize = 50): SenderSummary` atomically claims due rows and updates `sent/failed/pending` with bounded retry state.
- Cron script supports CLI and guarded web-cron invocation without printing secrets.

- [ ] **Step 1: Write failing sender-state tests with fake Meta client**

Cover success -> sent+message ID, transient failure -> attempt increment/retry, permanent config failure -> failed, already-sent row never resent, token absent from error projection.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `php backend/tests/reminder_sender_test.php`
Expected: FAIL before sender/client exist.

- [ ] **Step 3: Implement Meta Graph client and claim/send/update loop**

Use cURL with `Authorization: Bearer ...`; template payload maps customer, business, date, time, service, staff. Keep a bounded batch and explicit retry ceiling/backoff fields.

- [ ] **Step 4: Run all backend checks**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`
Expected: PASS without external Meta network dependency in tests.

- [ ] **Step 5: Commit**

Commit message: `feat: add whatsapp cron sender`

### Task 6: Android WhatsApp API client and persistent sync state

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/network/WhatsAppApiClient.kt`
- Create: `app/src/main/java/com/elxvro/randevu/core/WhatsAppReminderCore.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/storage/LiveSyncStore.kt`
- Test: `app/src/test/java/com/elxvro/randevu/core/WhatsAppReminderCoreTest.kt`

**Interfaces:**
- `WhatsAppConnectionState { DISABLED, INCOMPLETE, CONNECTED, UNREACHABLE }`.
- `WhatsAppSettings(enabled, reminder24h, reminder2h, templateName, languageCode)` stored locally without Meta secret.
- `WhatsAppReminderCore.syncDecision(...)` returns enqueue/cancel/pending-local action.
- `WhatsAppApiClient` calls status/profile/settings/reminder endpoints over HTTPS with existing app bearer token.

- [ ] **Step 1: Write failing sync-decision tests**

Assert backend unavailable -> pending local sync and never `scheduled`; connected create/edit -> upsert action; cancellation/deletion -> cancel action; disabled -> no remote enqueue.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*WhatsAppReminderCoreTest*'`
Expected: FAIL before core/client exist.

- [ ] **Step 3: Implement core, client, and tolerant local settings/pending-state persistence**

Reuse existing HTTPS/token conventions but update User-Agent to v1.3.0. Never serialize Meta access token or phone-number secret into app state.

- [ ] **Step 4: Run Android unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add android whatsapp reminder sync`

### Task 7: WhatsApp settings UI and appointment lifecycle bridge

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuV13App.kt`
- Create/Modify: `app/src/main/java/com/elxvro/randevu/ui/WhatsAppReminderScreens.kt`
- Modify: appointment mutation path used by v1.3
- Test: `app/src/test/java/com/elxvro/randevu/ui/WhatsAppStatusProjectionTest.kt`

**Interfaces:**
- Bell/local notification center remains separate.
- More/Settings adds `WhatsApp Hatırlatmaları` with state, enable, 24h/2h toggles, template/language, backend URL/test, last sync status.
- Appointment add/edit/cancel/delete invokes Task 6 sync decision asynchronously; local save is never rolled back by remote failure.

- [ ] **Step 1: Write failing UI projection tests**

Assert `connected/incomplete/unreachable/disabled` map to correct Turkish labels and no backend ack means UI cannot display `planlandı/gönderilecek` as confirmed.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*WhatsAppStatusProjectionTest*'`
Expected: FAIL before projection/UI exists.

- [ ] **Step 3: Implement settings screen and lifecycle sync bridge**

Keep dark navy/cyan tokens. On remote error, persist pending operation and display `Sunucuya ulaşılamıyor`/`Bekleyen senkronizasyon`; local Android reminders continue unchanged.

- [ ] **Step 4: Run Android unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: connect whatsapp reminders to appointments`

### Task 8: CI backend gate, version bump, and release artifact

**Files:**
- Modify: `.github/workflows/android.yml`
- Modify: `app/build.gradle.kts`
- Modify: `backend/public/index.php` version response
- Test: all Android + PHP tests

**Interfaces:**
- Produces `versionCode = 13`, `versionName = "1.3.0"`.
- CI runs Android test/build and PHP syntax/backend deterministic tests before artifact upload.

- [ ] **Step 1: Add backend validation step to workflow**

Run PHP syntax gate plus deterministic backend test scripts before Android artifact upload.

- [ ] **Step 2: Update application/backend version labels to 1.3.0**

Remove stale `0.5.0` backend version and stale Android User-Agent values in active clients.

- [ ] **Step 3: Run full local-equivalent release gate**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done && gradle --no-daemon testDebugUnitTest assembleDebug`
Expected: all checks PASS and debug APK assembles.

- [ ] **Step 4: Verify GitHub Actions and uploaded APK artifact**

Expected: workflow success; artifact contains `app-debug.apk` built from v1.3 branch head.

- [ ] **Step 5: Commit**

Commit message: `release: prepare randevu v1.3.0`

## Self-review

- Spec coverage: server-only secrets, business/WhatsApp settings, queue schema, authenticated reminder routes, idempotent schedule, cron sender, failure/retry behavior, Android pending sync, status UI, lifecycle bridge, PHP CI gate, and v1.3 release artifact are all assigned.
- Step scan: each behavior task has RED evidence, concrete interface, verification command, and commit boundary.
- Type consistency: queue offsets are always integer minutes `1440/120`; Android status enum maps only nonsecret backend state.
- Review focus coverage: duplicates Task 4, crash/retry Task 5, invalid config Task 3/4, ownership Task 2/4, secret leakage Task 1/3/5/6.
- Proportion: local business setup/migration is deliberately handled by the companion first-run plan and is a prerequisite for Tasks 6–8.