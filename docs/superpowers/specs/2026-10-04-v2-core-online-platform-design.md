# Randevu v2.0 Core Online Platform Design

## 1. Intent

Randevu v2.0 converts the current Android-first, locally persisted appointment application into a server-authoritative online platform without discarding the existing Android UI or WhatsApp reminder work.

The complete platform is intentionally split into independently shippable sub-projects:

1. **v2.0 Core Online Platform** — business account, multi-tenant API, Android cloud sync, one-time v1.3 data import, public customer booking page, server-side availability and WhatsApp queue integration.
2. **v2.1 Web Management** — owner/staff browser dashboard, staff permissions and operational management.
3. **v2.2 CRM & Reporting** — customer history, no-show metrics, service/staff analytics and exports.
4. **v2.3 Multi-branch & SaaS** — multiple branches, plans/subscriptions and platform administration.

This document specifies only sub-project 1. It must leave clean interfaces for the later phases but must not implement billing, multi-branch, advanced analytics or a full owner web dashboard.

## 2. Existing System to Preserve

The existing v1.3.1 branch already has:

- Android business first-run setup and owner-created service/staff records.
- Local appointment, staff and leave persistence.
- Local notification reminders.
- PHP 8.1+/MySQL backend primitives.
- Authenticated business-scoped WhatsApp settings and reminder queue.
- Official WhatsApp Cloud API delivery through server-side credentials.
- Dark/cyan Android design contract.

v2.0 reuses these assets. The existing Meta access token remains server-only and must never be added to the APK, public HTML or Git history.

## 3. Chosen Architecture

### 3.1 Server authority

PHP/MySQL becomes the source of truth for business profile, services, staff, leaves and appointments. Android keeps an offline cache and a durable mutation queue, but successful server state always wins after reconciliation.

The API is versioned under `/v2`. Existing v1.3.1 WhatsApp endpoints remain temporarily available for compatibility while v2 is introduced.

### 3.2 Hosting model

The platform targets normal PHP 8.1+ shared hosting with MySQL/InnoDB and cron support. It must not require Node.js, Docker, Redis or a shell on production hosting.

Public web pages are server-rendered PHP with progressive JavaScript only where needed for slot selection. This keeps deployment compatible with the user's hosting.

### 3.3 Tenancy

Every private operational row is scoped by `business_id`. Authenticated API requests derive `business_id` from the access token; clients never choose another tenant by sending a business ID.

Public booking identifies a business only by its unique public slug.

## 4. Authentication and Accounts

### 4.1 Owner account

v2.0 uses **email + password** for owner authentication. Phone remains a business/customer communication field, not an authentication identifier.

Owner registration creates, in one transaction:

- a business,
- its unique public slug,
- an owner user,
- default business settings,
- an expiring API token.

Passwords use PHP `password_hash()` with `PASSWORD_DEFAULT`. Raw passwords and raw bearer tokens are never stored.

### 4.2 Session model

Bearer tokens are random 32-byte values. Only SHA-256 token hashes are stored in `api_tokens`. Tokens expire after 30 days and can be revoked on logout. Android may renew by logging in again in v2.0; refresh tokens are deferred.

### 4.3 Staff accounts

v2.0 stores staff records and optional staff-user linkage, but staff interactive login/permissions are deferred to v2.1. This prevents the first online slice from mixing identity/authorization work with the booking migration.

## 5. Data Model

The existing schema is extended rather than replaced.

### 5.1 businesses

Add:

- `slug VARCHAR(96) NOT NULL UNIQUE`
- `opening_time TIME NOT NULL DEFAULT '09:00:00'`
- `closing_time TIME NOT NULL DEFAULT '18:00:00'`
- `active TINYINT(1) NOT NULL DEFAULT 1`

Timezone remains IANA format.

### 5.2 users

For v2 owner authentication:

- add `email VARCHAR(190) NULL`
- add unique index on non-null email through normal MySQL uniqueness semantics
- roles support `owner`, `staff`; legacy values may remain for migration but are not used by v2 registration
- `password_hash` is mandatory for owner accounts created through v2.

### 5.3 services

Add:

- `external_id CHAR(36) NOT NULL UNIQUE`
- keep name, duration, price and active.

Android service UUIDs map to `external_id`.

### 5.4 staff

Add:

- `external_id CHAR(36) NOT NULL UNIQUE`
- `title VARCHAR(120) NULL`
- `user_id BIGINT UNSIGNED NULL`
- `public_booking TINYINT(1) NOT NULL DEFAULT 1`

### 5.5 staff_leaves

Create a server equivalent of the Android leave model:

- id
- business_id
- external_id UUID
- staff_id
- start_date/end_date
- optional start_time/end_time
- reason
- version
- created_at/updated_at

### 5.6 appointments

Extend with:

- `external_id CHAR(36) NOT NULL UNIQUE`
- `source ENUM('android','public_web','system')`
- `version INT UNSIGNED NOT NULL DEFAULT 1`
- existing business/service/staff/customer/start/end/status/note fields.

Customer accounts are not required for v2.0. Public customers submit name and phone; `customer_id` remains nullable.

### 5.7 optimistic concurrency

Mutable synchronized entities use integer `version`. Update/delete operations send `expected_version`.

If the stored version differs, the API returns HTTP 409 with the current server record. Android keeps the server record, marks the local mutation conflicted and shows a non-destructive conflict notice.

## 6. v2 Private API

All JSON responses contain `ok`. Errors contain stable machine `error` codes and may include a Turkish `message`.

### Authentication

- `POST /v2/auth/register-owner`
- `POST /v2/auth/login`
- `POST /v2/auth/logout`
- `GET /v2/me`

### Business

- `GET /v2/business`
- `PUT /v2/business`

### Services

- `GET /v2/services`
- `POST /v2/services`
- `PUT /v2/services/{external_id}`
- `DELETE /v2/services/{external_id}`

### Staff and leave

- `GET /v2/staff`
- `POST /v2/staff`
- `PUT /v2/staff/{external_id}`
- `DELETE /v2/staff/{external_id}`
- `GET /v2/staff-leaves`
- `POST /v2/staff-leaves`
- `PUT /v2/staff-leaves/{external_id}`
- `DELETE /v2/staff-leaves/{external_id}`

### Appointments

- `GET /v2/appointments?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /v2/appointments`
- `PUT /v2/appointments/{external_id}`
- `DELETE /v2/appointments/{external_id}`

Deletes of appointments are represented operationally as cancellation unless the record has never been externally visible. The API returns the resulting record/status.

### Sync bootstrap

- `GET /v2/sync/bootstrap`

This returns business profile, active and inactive services, staff, current/future leaves and appointments in a bounded window. It is used at sign-in and recovery.

v2.0 deliberately avoids a general event-stream/cursor subsystem. Normal mutations are sent immediately or from the Android durable queue; bootstrap refresh is the recovery mechanism. A delta event log can be added later only if scale requires it.

## 7. Android Online Behavior

### 7.1 Entry flow

Fresh v2.0 installs show:

1. **Giriş Yap**
2. **Yeni İşletme Oluştur**

The old local business setup wizard is not the primary entry path in v2.0. After registration, owner profile/service setup continues using the existing dark/cyan setup UI, but saves through the v2 API.

### 7.2 Offline-first cache

Android keeps local cached copies of the server records for responsive UI and temporary offline use.

Every offline mutation is stored durably with:

- operation id
- entity type
- external id
- mutation type
- expected server version
- serialized payload
- created time
- retry count
- last error.

When connectivity returns, operations replay in order. HTTP 2xx removes the mutation. HTTP 409 preserves the conflict for user review. Authentication failures stop replay until sign-in is restored. 5xx/network failures remain queued with bounded retry.

### 7.3 v1.3 migration

After the first successful v2 login/registration, if v1.3 local operational data exists, Android shows a one-time migration card:

**"Bu cihazdaki mevcut verileri online hesabına aktar."**

Import order:

1. business profile,
2. services,
3. staff,
4. leaves,
5. appointments.

The import is idempotent by external UUID. Successfully imported records are not duplicated on retry. Local data is retained as cache after import; it is not destructively erased.

### 7.4 connection state

The main UI must visibly distinguish:

- Online
- Senkronize ediliyor
- Çevrimdışı — N işlem bekliyor
- Oturum gerekli
- Çakışma var

The app must never claim cloud synchronization before server acknowledgement.

## 8. Public Customer Booking

### 8.1 URL

Each business receives:

`/r/{business-slug}`

The page is public and mobile-first.

### 8.2 flow

Customer selects:

1. service,
2. eligible staff,
3. date,
4. available time,
5. name and phone,
6. confirmation.

No customer account is required in v2.0.

### 8.3 slot engine

Server computes availability using the business timezone and must exclude:

- outside business opening/closing hours,
- inactive services/staff,
- staff with `public_booking=0`,
- staff leave,
- existing pending/confirmed appointments,
- intervals where the full service duration does not fit.

Default slot increment is 15 minutes.

Booking creation rechecks availability inside a database transaction immediately before insert. A slot that became occupied returns HTTP 409 `slot_unavailable`; the page refreshes available times instead of double-booking.

### 8.4 public privacy

The public page exposes only data needed for booking: business display fields, active services, public staff and available slots. It never exposes private notes, customer lists, staff phone numbers, tokens or internal numeric IDs.

## 9. WhatsApp Integration

v2.0 moves WhatsApp scheduling behind appointment domain operations.

After a successful appointment create/update/cancel:

- the backend updates/cancels the existing `whatsapp_reminder_queue` in the same business scope;
- public-web bookings receive the same reminder behavior as Android-created bookings;
- Android no longer needs to be the source of truth for reminder creation.

The existing WhatsApp settings UI and connection test remain. Meta credentials stay server-only.

Failure to enqueue WhatsApp must not corrupt the appointment. The appointment response includes `reminder_state` so Android/web can state whether reminders are scheduled, disabled or need attention.

## 10. Public Web UI

v2.0 web scope is only the customer booking journey and minimal success/error pages.

Visual direction:

- same near-black/navy background family as Android,
- cyan/blue accent,
- white primary text,
- compact rounded cards,
- mobile-first,
- no dependence on a JS framework.

A full owner/staff web dashboard is explicitly v2.1.

## 11. Security

- Production API and booking form require HTTPS.
- PDO prepared statements for every query with user input.
- Token-derived tenancy on every private endpoint.
- Password hashing through PHP native password APIs.
- Rate limits on login, registration and public booking by IP plus identifier.
- Public booking validates and normalizes phone numbers.
- CSRF tokens for browser form POSTs where cookie/session state is ever introduced; v2 public booking can remain stateless JSON/form POST.
- No stack traces, SQL text, credentials or Meta tokens in responses/logs.
- Server config remains outside public web root where hosting permits it.
- CORS defaults to same-origin; Android is not a browser and does not require permissive CORS.

## 12. Error Handling

Stable error codes include:

- `validation_error`
- `unauthorized`
- `email_in_use`
- `invalid_credentials`
- `tenant_mismatch`
- `not_found`
- `version_conflict`
- `slot_unavailable`
- `rate_limited`
- `server_error`

Client-facing Turkish messages are mapped separately from machine codes.

## 13. Testing and Release Gates

Backend deterministic tests must cover:

- tenant isolation,
- registration/login/token revocation,
- password hashing behavior,
- CRUD ownership,
- version conflict handling,
- slot generation across opening hours/leaves/overlaps,
- transactional double-book prevention,
- public data redaction,
- WhatsApp queue update/cancel integration.

Android unit tests must cover:

- login/setup gate,
- durable sync queue coalescing,
- 409 conflict projection,
- migration idempotency,
- connection state projection,
- no false "synced" state before server acknowledgement.

CI runs PHP syntax/tests and Android unit tests/build. A v2.0 APK is not declared complete until the full gate passes and the artifact is produced.

## 14. Deployment Package

The v2.0 deliverable consists of:

- Android APK for device testing,
- Android source in the repository,
- `backend/` PHP/MySQL package,
- SQL migration from v1.3 schema,
- public booking files,
- `config.example.php`,
- shared-hosting installation guide,
- cron instructions for WhatsApp sender.

No production password, API token, setup key or Meta credential is committed.

## 15. Definition of Done for v2.0 Core

v2.0 Core is complete when:

- an owner can register and sign in from Android;
- a new business receives an isolated server tenant and public slug;
- Android business/services/staff/leaves/appointments are server-backed with offline queueing;
- existing v1.3 local data can be imported once without duplication;
- a customer can create a real booking from `/r/{slug}`;
- the slot engine prevents staff double-booking;
- Android sees public-web bookings after refresh/bootstrap;
- WhatsApp reminders are queued server-side for both Android and public-web appointments when enabled;
- all private endpoints enforce tenant ownership;
- CI passes and produces the v2.0 APK.

## 16. Explicitly Deferred

The following belong to later sub-projects and are not blockers for v2.0 Core:

- owner/staff browser dashboard,
- staff login and granular permissions,
- customer login,
- online payments,
- subscription billing,
- multi-branch,
- advanced CRM/reporting,
- push/WebSocket real-time updates,
- marketplace/discovery.
