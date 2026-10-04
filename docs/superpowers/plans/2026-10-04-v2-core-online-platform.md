# Randevu v2.0 Core Online Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert Randevu v1.3.1 into a production-oriented online appointment platform with owner authentication, tenant-isolated PHP/MySQL data, Android offline/cloud synchronization, one-time local import, public customer booking and server-owned WhatsApp reminder scheduling.

**Architecture:** PHP 8.1+/MySQL becomes authoritative under versioned `/v2` endpoints while Android retains a durable offline cache and mutation queue. Public booking is server-rendered PHP plus small progressive JavaScript, with transactional server-side availability checks. Existing v1.3.1 UI components and WhatsApp sender are reused; v2 adds isolated domain/repository files instead of expanding the already-large v1.3 application file.

**Tech Stack:** Android/Kotlin/Jetpack Compose, Kotlin coroutines, SharedPreferences + Android Keystore, PHP 8.1+, PDO/MySQL/InnoDB, server-rendered HTML/CSS/vanilla JS, GitHub Actions with Java 17/Gradle 8.7.

**Spec:** `docs/superpowers/specs/2026-10-04-v2-core-online-platform-design.md`

## Global Constraints

- Production API and booking URLs require HTTPS.
- PHP runtime floor is 8.1 and production hosting must not require Node.js, Docker, Redis or shell access.
- MySQL tables use InnoDB and every private operational row is scoped by `business_id`.
- Authenticated tenancy comes only from the bearer token; private endpoints must ignore/reject client-supplied tenant IDs.
- Owner authentication is email + password; owner tokens expire after 30 days and only SHA-256 token hashes are stored server-side.
- Meta WhatsApp access tokens remain server-only and must never be stored in the APK, public HTML, logs or Git history.
- Android remains usable from cache while offline and must never display a cloud-success state before server acknowledgement.
- Mutable synchronized records use integer `version`; stale writes return HTTP 409 with the current server record.
- Public booking uses 15-minute slot increments and must re-check the slot transactionally immediately before insert.
- v1.3 local import is one-time, non-destructive and idempotent by external UUID.
- Fresh v2 installs enter through owner login or owner registration, not the old standalone local-business setup flow.
- Android release target is `versionName = "2.0.0"`, `versionCode = 20`.
- Full owner/staff web dashboard, staff login/permissions, customer accounts, payments, subscriptions, multi-branch and advanced analytics remain out of scope.

## Review Focus

- Duplicate/malformed owner registration, brute-force login and rate-limit expiry must fail without revealing whether an account exists — covered in Task 2 tests.
- A stale Android `expected_version` must yield 409, preserve the server record and surface a conflict instead of silently overwriting it — covered in Tasks 3 and 8 tests.
- Availability at opening/closing boundaries, partial-day leave, overnight-invalid inputs and timezone conversion must produce deterministic slots — covered in Task 4 tests.
- Two booking attempts for the same staff/time must not both commit; the loser receives `slot_unavailable` — covered in Task 4 repository/service tests.
- Offline replay after token expiry or interrupted v1.3 import must retain unsent operations, avoid duplicate imports and require sign-in before continuing — covered in Tasks 7 and 8 tests.

---

### Task 1: v2 schema migration and shared backend contracts

**Files:**
- Create: `backend/migrations/2026_10_04_v2_core.sql`
- Create: `backend/src/V2Types.php`
- Create: `backend/src/Slug.php`
- Create: `backend/tests/v2_schema_contract_test.php`
- Create: `backend/tests/v2_slug_test.php`
- Modify: `backend/src/bootstrap.php`
- Modify: `backend/schema.sql`

**Interfaces:**
- Produces: `Slug::base(string $name): string`
- Produces: `Slug::unique(PDO $pdo, string $name): string`
- Produces: `V2ApiError` constants for stable machine error codes.
- Schema adds business slug/hours/active, owner email role support, external UUIDs/version fields, staff leaves, rate-limit events and appointment source/version.

- [ ] **Step 1: Write failing schema and slug tests**

Add assertions that the migration/schema contain: unique `businesses.slug`, `users.email`, owner role support, service/staff/leave/appointment external UUIDs, appointment/source/version fields, `staff_leaves`, and `rate_limit_events`. Add slug cases for Turkish characters, punctuation, repeated whitespace and empty-after-normalization names.

- [ ] **Step 2: Run the PHP tests and verify RED**

Run: `for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: the new v2 tests fail because migration/types/slug contracts do not exist.

- [ ] **Step 3: Implement migration, schema mirror, slug helper and error constants**

Use additive ALTER/CREATE statements so an existing v1.3 database can migrate in place. Keep legacy user roles valid during transition; v2-created business owners use role `owner`. `Slug::unique()` appends `-2`, `-3`, etc. after checking existing business slugs.

- [ ] **Step 4: Run PHP validation**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: all PHP files lint clean and all tests pass.

- [ ] **Step 5: Commit**

Commit message: `feat: add v2 online schema contracts`

---

### Task 2: owner registration, login, logout and rate limiting

**Files:**
- Create: `backend/src/V2Auth.php`
- Create: `backend/src/RateLimiter.php`
- Create: `backend/tests/v2_auth_test.php`
- Create: `backend/tests/v2_rate_limit_test.php`
- Modify: `backend/src/Auth.php`
- Modify: `backend/public/index.php`

**Interfaces:**
- Produces: `V2Auth::registerOwner(PDO $pdo, RateLimiter $limits, array $input, string $ip): array`
- Produces: `V2Auth::login(PDO $pdo, RateLimiter $limits, array $input, string $ip): array`
- Produces: `V2Auth::logout(PDO $pdo, string $rawToken): void`
- Produces: `V2Auth::requireOwner(PDO $pdo): array` returning `user_id`, `business_id`, `role`, `token_hash`.
- Routes: `POST /v2/auth/register-owner`, `POST /v2/auth/login`, `POST /v2/auth/logout`, `GET /v2/me`.

Registration input is `email`, `password`, `business_name`, `owner_name`, optional `phone`, `address`, `timezone`. Email must pass `FILTER_VALIDATE_EMAIL`; password minimum is 8 characters; business/owner names are 2–160 trimmed characters. Token lifetime is exactly 30 days.

- [ ] **Step 1: Write failing auth tests**

Cover password hashing/verification, duplicate email, invalid email, short password, unique slug assignment, generic invalid-login response, token hash storage, 30-day expiry, logout revocation and token-derived business context. Add repeated-login and repeated-registration rate-limit cases that block after 10 failed attempts per 15-minute window keyed by IP + normalized identifier.

- [ ] **Step 2: Run v2 auth tests and verify RED**

Run: `php backend/tests/v2_auth_test.php && php backend/tests/v2_rate_limit_test.php`

Expected: FAIL because `V2Auth` and `RateLimiter` are absent.

- [ ] **Step 3: Implement rate limiter and owner auth**

Use `password_hash(..., PASSWORD_DEFAULT)`, `password_verify()`, `random_bytes(32)`, and SHA-256 token hashes. Registration creates business, owner, business settings and token in one transaction. Login failure uses one generic `invalid_credentials` code whether email is unknown or password is wrong.

- [ ] **Step 4: Wire v2 auth routes**

Keep existing v1.3 bootstrap endpoint unchanged. v2 owner routes are separate and never accept `business_id` from the client.

- [ ] **Step 5: Run PHP lint and auth suite**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && php backend/tests/v2_auth_test.php && php backend/tests/v2_rate_limit_test.php`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add v2 owner authentication`

---

### Task 3: tenant-scoped business, services, staff and leave API

**Files:**
- Create: `backend/src/V2BusinessRepository.php`
- Create: `backend/src/V2CatalogRepository.php`
- Create: `backend/src/V2StaffRepository.php`
- Create: `backend/src/V2TenantService.php`
- Create: `backend/tests/v2_tenant_api_test.php`
- Create: `backend/tests/v2_version_conflict_test.php`
- Modify: `backend/public/index.php`

**Interfaces:**
- Produces: `V2TenantService::business(int $businessId): array`
- Produces: `V2TenantService::updateBusiness(int $businessId, array $input, int $expectedVersion): array`
- Produces service CRUD methods using external UUIDs and expected versions.
- Produces staff CRUD and staff-leave CRUD methods using external UUIDs and expected versions.
- Routes exactly match the private Business, Services, Staff and Staff Leave endpoints in the spec.

All repository lookups are `WHERE business_id = :business` plus external ID where applicable. Any client `business_id` field is ignored and never becomes query scope.

- [ ] **Step 1: Write failing tenancy/version tests**

Use fake repository implementations to prove the authenticated business ID is passed through every read/write/delete and that a payload business ID cannot override it. Add 409 tests that assert response shape contains `error: version_conflict` and `current` server record.

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `php backend/tests/v2_tenant_api_test.php && php backend/tests/v2_version_conflict_test.php`

Expected: FAIL because repositories/service/routes do not exist.

- [ ] **Step 3: Implement tenant repositories and service**

Services/staff/leaves use external UUIDs supplied by Android or generated server-side for web-created records. Successful updates increment `version` by 1 atomically with `WHERE version=:expected`; zero affected rows trigger current-record fetch and `version_conflict`.

- [ ] **Step 4: Wire private v2 routes**

Use `V2Auth::requireOwner()` once per private request and pass only that context’s `business_id` into the service.

- [ ] **Step 5: Run PHP suite**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add tenant scoped v2 catalog api`

---

### Task 4: server appointment model, availability engine and transactional reservation

**Files:**
- Create: `backend/src/AvailabilityService.php`
- Create: `backend/src/V2AppointmentRepository.php`
- Create: `backend/src/V2AppointmentService.php`
- Create: `backend/tests/v2_availability_test.php`
- Create: `backend/tests/v2_appointment_service_test.php`
- Create: `backend/tests/v2_booking_lock_contract_test.php`
- Modify: `backend/public/index.php`
- Modify: `backend/src/ReminderQueueRepository.php`

**Interfaces:**
- Produces: `AvailabilityService::slots(array $business, array $service, array $staff, array $leaves, array $appointments, string $date): array`
- Produces: `V2AppointmentService::create(int $businessId, array $payload, string $source): array`
- Produces: `V2AppointmentService::update(int $businessId, string $externalId, array $payload, int $expectedVersion): array`
- Produces: `V2AppointmentService::cancel(int $businessId, string $externalId, int $expectedVersion): array`
- Repository reservation acquires a staff-scoped lock in a transaction, repeats the overlap query and inserts only if free.
- Private appointment routes match the spec and use date-bounded GET.

Slots are 15-minute increments; a candidate is valid only if the entire service duration fits inside business hours and does not overlap leave or pending/confirmed appointments.

- [ ] **Step 1: Write failing availability tests**

Cover opening boundary, closing boundary, 45-minute service on 15-minute increments, full-day leave, partial-day leave, cancelled/completed appointment exclusion, pending/confirmed overlap exclusion, inactive staff/service, invalid/overnight hours, and `Europe/Istanbul` date-to-UTC conversion around a DST-capable generic timezone fixture.

- [ ] **Step 2: Write failing appointment/locking tests**

Assert stale version returns current record, cancellation increments version, Android/public sources persist correctly, and the repository source contract contains transaction + staff lock + overlap recheck before INSERT. Add a fake reservation-store test where the first reservation wins and the second maps to `slot_unavailable`.

- [ ] **Step 3: Run focused tests and verify RED**

Run: `php backend/tests/v2_availability_test.php && php backend/tests/v2_appointment_service_test.php && php backend/tests/v2_booking_lock_contract_test.php`

Expected: FAIL.

- [ ] **Step 4: Implement availability and transactional appointment repository/service**

Use business IANA timezone for local booking inputs and store `starts_at`/`ends_at` consistently. Appointment create/update/cancel invokes existing WhatsApp reminder queue logic after the appointment transaction succeeds. Reminder failure is caught separately and returned as `reminder_state: needs_attention` without rolling back the appointment.

- [ ] **Step 5: Wire private appointment routes and bootstrap query support**

`GET /v2/appointments` requires valid `from` and `to` dates and caps the window at 180 days. The service exposes a reusable bounded query used later by sync bootstrap.

- [ ] **Step 6: Run full PHP suite**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat: add v2 availability and appointments`

---

### Task 5: public customer booking page and public API

**Files:**
- Create: `backend/src/PublicBookingService.php`
- Create: `backend/public/booking.php`
- Create: `backend/public/assets/v2-booking.css`
- Create: `backend/public/assets/v2-booking.js`
- Create: `backend/public/.htaccess`
- Create: `backend/tests/v2_public_booking_test.php`
- Create: `backend/tests/v2_public_redaction_test.php`
- Modify: `backend/public/index.php`
- Modify: `backend/src/RateLimiter.php`

**Interfaces:**
- Page: `GET /r/{business-slug}`
- Public API: `GET /v2/public/business/{slug}`
- Public API: `GET /v2/public/availability?slug={slug}&service={service_uuid}&staff={staff_uuid}&date=YYYY-MM-DD`
- Public API: `POST /v2/public/bookings`
- Produces: `PublicBookingService::catalog(string $slug): array`
- Produces: `PublicBookingService::availability(string $slug, string $serviceId, string $staffId, string $date): array`
- Produces: `PublicBookingService::book(array $payload, string $ip): array`

Public catalog contains only business display fields, active services and public staff. It never returns customer data, notes, staff phone numbers, bearer tokens or numeric internal IDs.

- [ ] **Step 1: Write failing public booking/redaction tests**

Assert private fields are absent, inactive/non-public resources are absent, malformed slug/UUID/date returns stable errors, booking requires customer name + normalized phone, and more than 20 booking submissions per IP in 15 minutes rate-limits.

- [ ] **Step 2: Run tests and verify RED**

Run: `php backend/tests/v2_public_booking_test.php && php backend/tests/v2_public_redaction_test.php`

Expected: FAIL.

- [ ] **Step 3: Implement public service and routes**

Reuse `AvailabilityService` and `V2AppointmentService::create(..., 'public_web')`. A transactional slot collision returns HTTP 409 `slot_unavailable`.

- [ ] **Step 4: Implement mobile-first booking UI**

Follow the existing dark/navy + cyan design contract in CSS. Flow is service → staff → date → server-returned slot → name/phone → confirmation. JavaScript only loads public JSON and submits booking; the initial business page and failure/success fallbacks remain readable without a JS framework.

- [ ] **Step 5: Run PHP suite**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add public online booking`

---

### Task 6: v2 sync bootstrap endpoint

**Files:**
- Create: `backend/src/V2SyncService.php`
- Create: `backend/tests/v2_sync_bootstrap_test.php`
- Modify: `backend/public/index.php`

**Interfaces:**
- Produces: `V2SyncService::bootstrap(int $businessId, DateTimeImmutable $now): array`
- Route: `GET /v2/sync/bootstrap`
- Response contains `business`, `services`, `staff`, `staff_leaves`, `appointments`, `server_time`.
- Appointment window is local business date from 90 days before today through 365 days after today.

- [ ] **Step 1: Write failing bootstrap tests**

Assert all collections are tenant-scoped, include inactive service/staff records for cache correctness, bound appointments to the specified window, and never include auth hashes/secrets.

- [ ] **Step 2: Run test and verify RED**

Run: `php backend/tests/v2_sync_bootstrap_test.php`

Expected: FAIL.

- [ ] **Step 3: Implement bootstrap service/route**

Reuse Task 3/4 repositories rather than duplicating SQL. Include entity versions and external UUIDs in every mutable record.

- [ ] **Step 4: Run full PHP suite**

Run: `find backend -name '*.php' -print0 | xargs -0 -n1 php -l && for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add v2 sync bootstrap`

---

### Task 7: Android v2 account, API contract, secure session and entry flow

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/online/V2Models.kt`
- Create: `app/src/main/java/com/elxvro/randevu/online/V2ApiContract.kt`
- Create: `app/src/main/java/com/elxvro/randevu/network/V2ApiClient.kt`
- Create: `app/src/main/java/com/elxvro/randevu/storage/V2SessionStore.kt`
- Create: `app/src/main/java/com/elxvro/randevu/ui/V20AuthScreens.kt`
- Create: `app/src/test/java/com/elxvro/randevu/online/V2ApiContractTest.kt`
- Create: `app/src/test/java/com/elxvro/randevu/online/V2EntryGateTest.kt`
- Create: `app/src/test/java/com/elxvro/randevu/storage/V2SessionCodecTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `data class V2Session(val ownerName: String, val email: String, val businessSlug: String, val expiresAt: String)`; raw token is stored separately.
- Produces: `enum class V2EntryDestination { AUTH, SETUP, APP }`
- Produces: `V2EntryGate.destination(hasToken: Boolean, profileComplete: Boolean, hasActiveService: Boolean): V2EntryDestination`
- Produces: `V2ApiClient.registerOwner(...)`, `login(...)`, `logout(...)`, `me(...)`.
- `V2SessionStore` stores the bearer token encrypted with Android Keystore AES/GCM under alias `randevu_v2_api_token`; SharedPreferences stores only non-secret session metadata.

- [ ] **Step 1: Write failing API/entry/session tests**

Assert exact `/v2` paths, registration/login payload field names, AUTH/SETUP/APP gate decisions, token ciphertext envelope round-trip codec, and that non-secret session JSON does not contain `token` or password.

- [ ] **Step 2: Run Android unit tests and verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V2ApiContractTest' --tests '*V2EntryGateTest' --tests '*V2SessionCodecTest'`

Expected: FAIL because v2 Android classes do not exist.

- [ ] **Step 3: Implement models, contract, network client and secure session store**

Require HTTPS base URL. Map HTTP 401 to `auth_required`, 409 to conflict-bearing API result, 429 to `rate_limited`, and network exceptions to offline without deleting the stored session.

- [ ] **Step 4: Implement login/register Compose screens**

Use existing Reference theme/tokens. Registration collects owner name, business name, email, password, optional phone/address and timezone. Do not expose the old server setup-key flow in v2 entry.

- [ ] **Step 5: Run focused tests**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V2*'`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add v2 android account entry`

---

### Task 8: Android durable multi-entity sync queue, conflict handling and v1.3 import

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/online/OnlineSyncCore.kt`
- Create: `app/src/main/java/com/elxvro/randevu/online/V13OnlineMigration.kt`
- Create: `app/src/main/java/com/elxvro/randevu/storage/V2OnlineStore.kt`
- Create: `app/src/test/java/com/elxvro/randevu/online/OnlineSyncCoreTest.kt`
- Create: `app/src/test/java/com/elxvro/randevu/online/V13OnlineMigrationTest.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/storage/LiveSyncStore.kt`

**Interfaces:**
- Produces enums `OnlineEntityType { BUSINESS, SERVICE, STAFF, STAFF_LEAVE, APPOINTMENT }` and `OnlineMutationType { UPSERT, DELETE }`.
- Produces `OnlineMutation(operationId, entityType, externalId, mutationType, expectedVersion, payloadJson, createdAtEpochMs, retryCount, lastError)`.
- Produces `OnlineSyncQueue.enqueue(current, next): List<OnlineMutation>` with coalescing per entity/external ID.
- Produces `OnlineConnectionState { ONLINE, SYNCING, OFFLINE, AUTH_REQUIRED, CONFLICT }`.
- Produces `SyncConflict(entityType, externalId, currentServerPayload)`.
- Produces `V13OnlineMigration.plan(profile, services, staff, leaves, appointments, importedIds): List<OnlineMutation>`.

- [ ] **Step 1: Write failing queue/conflict/migration tests**

Cover UPSERT→UPSERT coalescing, unsynced UPSERT→DELETE removal, synced UPDATE→DELETE replacement, independent entity IDs, retry count retention, 401 transition to AUTH_REQUIRED, 409 transition to CONFLICT while preserving current server payload, and interrupted migration retry without duplicate external UUIDs.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*OnlineSyncCoreTest' --tests '*V13OnlineMigrationTest'`

Expected: FAIL.

- [ ] **Step 3: Implement queue engine and persistent store**

Persist queue, version index, conflicts, import marker/imported external IDs and last successful bootstrap time. Do not delete existing v1.3 operational cache during import.

- [ ] **Step 4: Implement v1.3 import planner**

Import order is BUSINESS → SERVICE → STAFF → STAFF_LEAVE → APPOINTMENT. A successfully acknowledged entity is marked imported immediately, so process death can resume without duplication.

- [ ] **Step 5: Run focused tests**

Run: `gradle --no-daemon testDebugUnitTest --tests '*OnlineSyncCoreTest' --tests '*V13OnlineMigrationTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add v2 durable online sync queue`

---

### Task 9: Android bootstrap reconciliation and cloud-backed operational mutations

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/online/V2SyncCoordinator.kt`
- Create: `app/src/main/java/com/elxvro/randevu/ui/OnlineConnectionProjection.kt`
- Create: `app/src/test/java/com/elxvro/randevu/online/V2SyncCoordinatorTest.kt`
- Create: `app/src/test/java/com/elxvro/randevu/ui/OnlineConnectionProjectionTest.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/network/V2ApiClient.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/storage/V2OnlineStore.kt`

**Interfaces:**
- Produces: `V2SyncCoordinator.bootstrap(sessionToken: String): V2SyncOutcome`
- Produces: `V2SyncCoordinator.enqueueAndSync(mutation: OnlineMutation): V2SyncOutcome`
- Produces: `V2SyncCoordinator.flush(): V2SyncOutcome`
- `V2SyncOutcome` carries connection state, pending count, optional conflict and user-safe message.
- Produces Turkish labels exactly: `Online`, `Senkronize ediliyor`, `Çevrimdışı — N işlem bekliyor`, `Oturum gerekli`, `Çakışma var`.

- [ ] **Step 1: Write failing coordinator tests**

Use a fake API transport. Assert bootstrap replaces cache with server-authoritative records, 2xx removes only acknowledged mutation and updates version, network failure leaves queue intact, 401 stops replay without dropping operations, and 409 stores server current record/conflict without retry-looping the same mutation.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V2SyncCoordinatorTest' --tests '*OnlineConnectionProjectionTest'`

Expected: FAIL.

- [ ] **Step 3: Implement remaining v2 client CRUD/bootstrap calls and coordinator**

Replay mutations in created-order. Stop on AUTH_REQUIRED, CONFLICT or first transient failure. A successful bootstrap is allowed after the queue is empty; do not overwrite unsent local mutations with bootstrap data.

- [ ] **Step 4: Implement projection**

Keep status text compact and use existing reference success/warning/danger colors.

- [ ] **Step 5: Run focused Android tests**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V2SyncCoordinatorTest' --tests '*OnlineConnectionProjectionTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add v2 android sync coordinator`

---

### Task 10: v2 Android shell, one-time import UI and server-owned WhatsApp behavior

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/ui/RandevuV20App.kt`
- Create: `app/src/main/java/com/elxvro/randevu/ui/V20MigrationDialog.kt`
- Create: `app/src/test/java/com/elxvro/randevu/ui/V20AppPolicyTest.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/MainActivity.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/BusinessSetupScreens.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/BusinessSettingsScreens.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/V13AppointmentDialog.kt`

**Interfaces:**
- `MainActivity` launches `RandevuV20App()`.
- `RandevuV20App` owns AUTH/SETUP/APP routing, starts bootstrap after valid session, delegates all business/service/staff/leave/appointment writes to `V2SyncCoordinator`, and renders cached data while offline.
- Existing v1.3 visual shell/components are reused; v2 shell adds a compact online-state indicator.
- WhatsApp settings remain editable, but appointment reminder scheduling is no longer initiated as a second Android-owned reminder mutation; the v2 appointment API response’s `reminder_state` is authoritative.

- [ ] **Step 1: Write failing app policy tests**

Assert fresh install without token routes AUTH, authenticated incomplete profile routes SETUP, completed cloud profile routes APP, local v1.3 data shows import offer exactly once after first successful online session, dismissing import does not erase data, and appointment save does not mark cloud/WhatsApp success before coordinator acknowledgement.

- [ ] **Step 2: Run focused test and verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V20AppPolicyTest'`

Expected: FAIL.

- [ ] **Step 3: Implement RandevuV20App routing and state composition**

Reuse existing tab screens where possible, but keep v2 cloud orchestration outside `RandevuV13App.kt`. New/edit/delete callbacks update local cache optimistically, enqueue durable mutation, then request sync.

- [ ] **Step 4: Implement one-time import UI**

Show the exact action copy: `Bu cihazdaki mevcut verileri online hesabına aktar.` Provide `Aktar` and `Şimdi Değil`; successful import progress can resume after process restart.

- [ ] **Step 5: Remove duplicate appointment-owned WhatsApp scheduling from v2 shell**

Keep existing v1.3 code untouched for compatibility; only the v2 app path uses server-owned reminder state.

- [ ] **Step 6: Run complete Android unit suite**

Run: `gradle --no-daemon testDebugUnitTest`

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat: connect android app to v2 cloud platform`

---

### Task 11: release version, deployment package, CI and end-to-end release gate

**Files:**
- Create: `backend/INSTALL.md`
- Create: `backend/migrations/README.md`
- Create: `docs/superpowers/ledger-v2.0.0.md`
- Modify: `backend/config.example.php`
- Modify: `backend/public/index.php`
- Modify: `app/build.gradle.kts`
- Modify: `.github/workflows/android.yml`
- Modify: `README.md`

**Interfaces:**
- Android reports `2.0.0` / versionCode 20.
- Backend `GET /health` and `GET /version` report `2.0.0`.
- INSTALL documents shared-hosting upload layout, DB migration order, HTTPS, config placement, public web root, `.htaccess`, WhatsApp cron command/cadence and rollback/backup prerequisite.
- CI must lint all PHP, run every backend test, run Android unit tests and assemble debug APK.

- [ ] **Step 1: Add release/deployment documentation and exact config placeholders**

Document required DB fields, `RANDEVU_APP_SETUP_KEY` compatibility setting, Meta server credentials, public base URL and cron. No real credentials appear in examples.

- [ ] **Step 2: Set v2 versions and update health/version responses**

Set Android `versionCode = 20`, `versionName = "2.0.0"`; backend version is `2.0.0`.

- [ ] **Step 3: Run the full local-equivalent verification gate**

Run:
`find backend -name '*.php' -print0 | xargs -0 -n1 php -l`

Then:
`for t in backend/tests/*_test.php backend/tests/schema_contract.php; do php "$t"; done`

Then:
`gradle --no-daemon testDebugUnitTest assembleDebug`

Expected: every command exits 0 and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Perform release requirement review**

Check against the spec Definition of Done: owner register/login, isolated tenant, server-backed five data groups, offline queue, idempotent v1.3 import, public `/r/{slug}` booking, no staff double-book, bootstrap visibility of public booking, server WhatsApp queue, tenant enforcement and artifact production. Record evidence/gaps in `docs/superpowers/ledger-v2.0.0.md`.

- [ ] **Step 5: Commit**

Commit message: `release: prepare randevu v2.0.0 core`

- [ ] **Step 6: Push/build on `build/v2.0.0-online-core` and verify GitHub Actions**

Expected: backend validation success, Android test/build success and `randevu-debug-apk` artifact uploaded. Download the artifact and provide the APK only after this fresh green run.

