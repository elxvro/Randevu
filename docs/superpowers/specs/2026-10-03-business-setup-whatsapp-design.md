# Randevu v1.3.0 — First-Run Business Setup & WhatsApp Reminder Design

Date: 2026-10-03
Base: `build/v1.2.0-active-ux`
Status: Written design for user review

## Intent

The application must no longer open as a preconfigured demo business. A fresh install must contain no demo appointments, customers, staff, business identity, or sample operational records. The owner creates the business from a first-run setup flow and can later change those settings.

The release must also add automatic customer reminder messaging through the official WhatsApp Business Cloud API. The mobile application must never contain the Meta access token or other server secrets. WhatsApp automation is therefore split between the Android app and a PHP/MySQL backend deployed by the owner.

## Current-state findings

- v1.2.0 seeds demo appointments whenever local appointment storage is empty.
- `LiveSyncStore.loadStaff()` currently returns three predefined demo staff when no saved staff exist.
- The visible profile/business identity is still presentation data rather than a required owner-created business profile.
- The current PHP backend exposes only `/health` and `/version`; it does not use MySQL for WhatsApp reminders.
- Local Android appointment reminders already use WorkManager and remain useful independently of WhatsApp.

## Selected architecture

### Considered approaches

1. **Open WhatsApp with a prefilled message on the phone.** Simple and works without a server, but it is not automatic because the user must confirm/send the message. This does not meet the requested outcome.
2. **Put WhatsApp Cloud API credentials in the Android app and send directly.** Technically direct, but insecure: an APK can be inspected and credentials extracted. Rejected.
3. **Official WhatsApp Business Cloud API through the PHP backend, with a MySQL reminder queue and cron worker.** Selected. It keeps secrets server-side, supports unattended sends, and remains compatible with shared hosting.

Meta's official WhatsApp Business Platform material describes Cloud API as the official hosted business messaging API and uses a Bearer access token plus a registered phone-number ID for message sending. The design therefore treats the token and phone-number ID as backend-only configuration.

## First-run business setup

### Fresh-install rule

On a fresh install:

- `appointments = []`
- `staff = []`
- `staffLeaves = []`
- no customer list is synthesized from seed appointments
- no hard-coded person names are shown as real records
- no hard-coded business name/profile is considered configured

Existing v1.2 installations must not unexpectedly regain demo data if their real lists become empty. Empty data is a valid state.

### Setup gate

Introduce a persistent `BusinessProfile` with at least:

- `businessName`
- `phone`
- `address`
- `timezoneId` (default device zone when the owner accepts it)
- `ownerName`
- `setupCompleted`

If `setupCompleted == false`, the normal bottom-navigation application shell is not shown. The user sees the same dark navy + cyan design language in a compact multi-step `İşletmeni Kur` flow.

### Setup steps

1. **İşletme bilgileri** — business name, owner name, phone, address.
2. **Çalışma düzeni** — timezone and basic opening hours. Detailed weekly exceptions can remain in the later settings screen.
3. **Hizmetler** — owner adds at least one service; no built-in salon/beauty service names are inserted as saved data.
4. **Personel** — optional at setup. The owner may add one or more staff members or skip and add staff later.
5. **Bildirimler** — local Android reminders can be enabled here; WhatsApp connection is optional and can be completed later.

Setup completion requires a nonblank business name and at least one active service. Staff may be empty, but booking UI must then explain that staff must be added before a staff-assigned appointment can be saved.

### Re-entry and reset

The More/Settings area provides `İşletme Bilgileri` for editing profile data. A destructive `İşletme Kurulumunu Sıfırla` action must require confirmation and must not be triggered accidentally. Reset clears business configuration and returns to setup; appointment/customer deletion behavior must be explicitly shown before confirmation.

## Service persistence

The current appointment form contains hard-coded service labels. v1.3.0 introduces persistent `ServiceRecord` entries:

- `id`
- `name`
- `durationMinutes`
- `active`

The setup flow writes these records. Appointment forms use active saved services only. Empty services block booking with a clear message directing the owner to add a service.

## WhatsApp reminder behavior

### User-visible settings

A new `WhatsApp Hatırlatmaları` settings screen shows:

- connection state: `Bağlı`, `Eksik kurulum`, `Sunucuya ulaşılamıyor`, `Kapalı`
- master enable/disable toggle
- 24-hour reminder toggle
- 2-hour reminder toggle
- template name and language code used by the backend
- backend API base URL / connection test when remote mode is used
- last known enqueue/sync status

No access token is entered into or stored in the Android APK.

### Message model

WhatsApp reminder messages are template-based. The recommended template is a utility reminder whose variables map to:

1. customer name
2. business name
3. appointment date
4. appointment time
5. service
6. staff name (or a neutral fallback if unassigned)

Example rendered meaning:

`Merhaba {{1}}, {{2}} işletmesindeki {{3}} {{4}} tarih/saatli {{5}} randevunuzu hatırlatırız. Personel: {{6}}.`

The exact approved text is ultimately defined in the Meta WhatsApp template, not hard-coded as free-form outbound text in the app.

### Reminder offsets

Default enabled offsets:

- 24 hours before appointment
- 2 hours before appointment

The Android local notification system continues to use the same offsets independently.

For WhatsApp, the backend owns the authoritative schedule. Android sends appointment/business changes to the backend, and the backend stores due reminder rows. This prevents missed WhatsApp sends when the Android app is killed, offline, or not opened.

## Backend data model

Extend MySQL with:

### `business_settings`

One row per business/application installation as supported by the current deployment model:

- business profile fields needed by the server
- timezone
- WhatsApp enabled flag
- 24h/2h flags
- template name
- template language
- phone-number ID reference

Secrets such as the Meta access token must not be stored in mobile-synchronized JSON. Prefer server environment/config outside the web root.

### `whatsapp_reminder_queue`

Fields:

- `id`
- `appointment_external_id`
- `offset_minutes` (`1440` or `120`)
- `recipient_phone`
- `scheduled_at_utc`
- `status` (`pending`, `processing`, `sent`, `failed`, `cancelled`)
- `attempt_count`
- `last_error`
- `meta_message_id`
- `created_at`
- `updated_at`

Unique constraint: `(appointment_external_id, offset_minutes)` so appointment edits reschedule instead of duplicating reminders.

### Appointment sync payload

Android sends the minimum data required for reminders:

- appointment id
- customer name
- customer phone
- date/time
- service name
- staff display name
- status
- business timezone

Cancellation/deletion marks queued reminders cancelled.

## Backend endpoints

Versioned API routes under the existing PHP entry point:

- `GET /health`
- `GET /whatsapp/status`
- `POST /whatsapp/test-connection`
- `PUT /business/profile`
- `PUT /business/whatsapp-settings`
- `PUT /appointments/{id}/reminders`
- `DELETE /appointments/{id}/reminders`

All mutation routes require application authentication/token validation. Database queries use PDO prepared statements.

## Cron worker

Provide a CLI/web-cron-compatible PHP worker, for example `backend/cron/send_whatsapp_reminders.php`.

Execution rules:

1. Load up to a bounded batch of due `pending` rows using UTC timestamps.
2. Atomically mark a row `processing` before send attempt.
3. Call `https://graph.facebook.com/{api-version}/{phone-number-id}/messages` with a Bearer token from server config.
4. On success, save returned Meta message ID and mark `sent`.
5. On transient failure, increment attempts and return to `pending` with a later retry time or bounded retry policy.
6. On permanent/configuration failure, mark `failed` and preserve a human-readable error code/message for diagnostics.
7. Never log the access token.

The hosting owner configures cron at a practical cadence (for example every 5 minutes). The worker must also support safe manual invocation for testing.

## Security

- Meta access token: backend only, never in Git and never in Android storage.
- Phone-number ID/WABA identifiers may be server config; only nonsecret status may be exposed to Android.
- All Android-to-backend mutation calls require bearer/application auth.
- Normalize recipient phone numbers to E.164-like international format before enqueueing.
- Validate appointment ownership/business association server-side before changing reminder rows.
- Rate-limit connection-test and send-test operations.
- Use HTTPS only for remote API URLs.

## Offline and failure behavior

- Business setup, appointments, staff, services, leave calendar, and local Android reminders continue to work offline.
- If WhatsApp backend is not configured, appointment creation still succeeds locally; UI reports `WhatsApp kurulumu gerekli` rather than pretending the message was scheduled.
- If backend is temporarily unreachable, Android records a pending reminder-sync operation and retries when connectivity returns / the app resumes.
- Local notification reminders are not disabled just because WhatsApp is unavailable.

## Migration from v1.2.0

### Fresh installs

No demo appointments or staff are created. Setup screen is mandatory.

### Existing installs

Because v1.2.0 contains seeded demo records indistinguishable from some real local records except their known IDs/names, migration will:

- remove known `seed-*` appointment records only when their exact known demo payload still matches the shipped demo payload
- remove known `staff-mert`, `staff-zeynep`, `staff-deniz` records only when their fields still match the original shipped defaults
- preserve any edited/user-created records
- still require creating a real `BusinessProfile` before entering the main app

This avoids deleting records merely because they share a human name with demo data.

## UI consistency

- Keep the v1.2 dark navy/cyan design system.
- Do not introduce light mode.
- Setup screens use the same card radius, controls, typography, status colors, and safe-area handling.
- Bottom navigation remains hidden during first-run setup and unchanged after setup completion.
- WhatsApp status uses the existing restrained status-pill language; WhatsApp brand green is not introduced as a second application theme color.

## Testing

### Pure-domain tests

- empty storage does not create demo appointments/staff/services
- migration removes only exact shipped demo records and preserves edited records
- setup completion validation
- service CRUD/active filtering
- phone normalization
- reminder offset calculation in business timezone
- reminder queue upsert/idempotency semantics
- cancelled/completed appointments do not produce future WhatsApp reminders

### Android tests/unit tests

- setup gate decision from persisted profile
- booking cannot proceed with zero services
- staff-empty state gives a useful action rather than crash
- backend-unavailable state never reports WhatsApp as scheduled

### PHP checks

- `php -l` for every PHP file
- database queue repository tests where feasible or deterministic pure PHP unit-like checks
- request validation and signed/authenticated route tests where feasible without external Meta calls

### CI release gate

Android:

`gradle --no-daemon testDebugUnitTest assembleDebug`

Backend syntax gate:

`find backend -name '*.php' -print0 | xargs -0 -n1 php -l`

Release target: `v1.3.0`, `versionCode = 13`.

## Definition of done

v1.3.0 is not complete unless:

- a fresh install shows `İşletmeni Kur` instead of a ready/demo business
- no demo appointment/staff/service is silently inserted into empty storage
- owner-created business/services/staff persist locally
- appointment UI consumes owner-created services/staff
- local reminders remain operational
- WhatsApp settings clearly distinguish configured/unconfigured states
- backend queue, endpoints, and cron sender are implemented without exposing Meta secrets to the APK
- appointment create/edit/cancel operations create/reschedule/cancel backend reminder rows when connected
- CI Android tests/build and PHP syntax checks pass
- final APK is produced

## Out of scope

- Meta Business account creation/verification on the user's behalf
- automatic creation/approval of WhatsApp message templates
- storing production Meta secrets in the repository
- guaranteeing WhatsApp delivery when the Meta account/template/phone number has not been configured by the owner
- multi-tenant SaaS billing or separate business accounts in this release
