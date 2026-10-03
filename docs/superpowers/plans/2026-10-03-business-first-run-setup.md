# Randevu v1.3.0 Business First-Run Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all shipped demo business data and make a fresh install start with a persistent owner-created business, service catalog, and optional staff setup before the main application shell is accessible.

**Architecture:** Add focused business/service domain models plus migration helpers, persist them through `LiveSyncStore`, and place a setup gate in front of `RandevuV12App`/its v1.3 successor. Existing appointment, staff-leave, and local notification subsystems remain offline-first and are fed only owner-created records after setup.

**Tech Stack:** Kotlin 1.9.24, Jetpack Compose Material 3, Android SDK 26–35, SharedPreferences/JSON, java.time, JUnit 4, Gradle 8.7.

**Spec:** `docs/superpowers/specs/2026-10-03-business-setup-whatsapp-design.md`

## Global Constraints

- Fresh installs create no demo appointments, staff, services, customers, or business identity.
- Existing edited/user-created records are preserved; only exact shipped demo payloads may be removed by migration.
- `BusinessProfile.setupCompleted == false` blocks the normal bottom-navigation shell.
- Setup completion requires a nonblank business name and at least one active service; staff is optional.
- Empty appointments/staff/services are valid persisted states and must not trigger reseeding.
- Preserve the approved dark navy + cyan theme; no light theme or WhatsApp-green secondary theme.
- Setup uses the same safe-area, typography, radii, control, and status-pill tokens as v1.2.
- Remote/WhatsApp secrets are never stored in Android business profile data.

## Review Focus

- Existing users whose lists are intentionally empty must not be mistaken for fresh installs or reseeded.
- Migration must compare complete known demo payloads, not only IDs or human names, before deleting.
- Corrupt persisted business/service JSON must not crash startup; invalid records are skipped or setup is re-entered.
- Timezone IDs must be validated with `ZoneId.of`; invalid saved zones fall back safely without silently marking setup complete.
- Setup interruption/process death must preserve completed steps without exposing the main shell before final completion.

---

### Task 1: Business/service domain and setup validation

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/business/BusinessSetupModels.kt`
- Create: `app/src/main/java/com/elxvro/randevu/business/BusinessSetupEngine.kt`
- Test: `app/src/test/java/com/elxvro/randevu/business/BusinessSetupEngineTest.kt`

**Interfaces:**
- Produces: `BusinessProfile(businessName: String, phone: String, address: String, timezoneId: String, ownerName: String, setupCompleted: Boolean)`.
- Produces: `ServiceRecord(id: String, name: String, durationMinutes: Int, active: Boolean)`.
- Produces: `BusinessSetupEngine.canComplete(profile: BusinessProfile, services: List<ServiceRecord>): Boolean`.
- Produces: `BusinessSetupEngine.activeServices(services: List<ServiceRecord>): List<ServiceRecord>` and timezone validation helper.

- [ ] **Step 1: Write failing setup-domain tests**

Pin: blank business name fails, zero active services fails, one active service passes, invalid timezone is rejected/falls back, inactive services are excluded.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*BusinessSetupEngineTest*'`
Expected: FAIL because business setup domain does not exist.

- [ ] **Step 3: Implement the domain and validation helpers**

Use immutable data classes and pure validation; default timezone source is supplied by caller rather than hard-coded business data.

- [ ] **Step 4: Run focused and full unit tests**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add business setup domain`

### Task 2: Exact demo migration and seed removal

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/business/V13Migration.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/staff/StaffModels.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/storage/LiveSyncStore.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuV12App.kt` or successor shell
- Test: `app/src/test/java/com/elxvro/randevu/business/V13MigrationTest.kt`

**Interfaces:**
- Produces: `V13Migration.cleanAppointments(records: List<Appointment>): List<Appointment>`.
- Produces: `V13Migration.cleanStaff(records: List<StaffRecord>): List<StaffRecord>`.
- Produces: migration marker `migration_v13_complete` persisted once.
- Changes `LiveSyncStore.loadStaff()` so missing storage returns `emptyList()`, not predefined staff.

- [ ] **Step 1: Write failing migration tests**

Assert exact shipped `seed-*` rows and exact `staff-mert/staff-zeynep/staff-deniz` defaults are removed, but any field-edited copy or user-created record is preserved; empty input remains empty.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V13MigrationTest*'`
Expected: FAIL before migration helpers exist.

- [ ] **Step 3: Implement exact-match migration and remove all runtime seed fallback**

Delete/retire `v12SeedAppointments()` and default staff insertion from the active v1.3 path. Migration executes once before first v1.3 state load.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `fix: remove shipped demo operational data`

### Task 3: Persistent business profile and service catalog

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/storage/LiveSyncStore.kt`
- Test: `app/src/test/java/com/elxvro/randevu/business/BusinessStorageCodecTest.kt`

**Interfaces:**
- Adds: `loadBusinessProfile()/saveBusinessProfile()`.
- Adds: `loadServices()/saveServices()`.
- Adds: `loadSetupStep()/saveSetupStep()` for resumable setup.
- Adds: `clearBusinessSetup(clearOperationalData: Boolean)` with explicit caller choice.

- [ ] **Step 1: Write codec/empty-state tests around pure JSON helpers extracted from the store**

Assert empty/missing storage yields unconfigured profile and empty services; malformed individual services are skipped; valid empty service list remains empty.

- [ ] **Step 2: Run tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*BusinessStorageCodecTest*'`
Expected: FAIL before codecs/store functions exist.

- [ ] **Step 3: Implement tolerant persistence and reset semantics**

Do not use empty-list-as-missing sentinel logic. Reset without operational deletion keeps appointments/staff/leaves; destructive reset path explicitly clears them only after confirmation.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: persist business profile and services`

### Task 4: First-run `İşletmeni Kur` setup gate and UI

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/ui/RandevuV13App.kt`
- Create: `app/src/main/java/com/elxvro/randevu/ui/BusinessSetupScreens.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/MainActivity.kt`
- Test: `app/src/test/java/com/elxvro/randevu/ui/BusinessSetupGateTest.kt`

**Interfaces:**
- Produces pure `BusinessSetupGate.destination(profile, services): SetupDestination` used by Compose root.
- Main shell is rendered only for `SetupDestination.APP`.
- Setup steps: business info, working arrangement/timezone, services, optional staff, notifications.

- [ ] **Step 1: Write failing gate tests**

Assert missing profile -> setup, incomplete profile -> setup, completed profile with no active services -> setup, completed profile + active service -> app.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*BusinessSetupGateTest*'`
Expected: FAIL before gate exists.

- [ ] **Step 3: Implement the multi-step setup UI and resumable state**

Use existing reference theme tokens; bottom navigation remains hidden during setup. Persist each completed step and only set `setupCompleted=true` after final validation.

- [ ] **Step 4: Run full unit suite and assemble**

Run: `gradle --no-daemon testDebugUnitTest assembleDebug`
Expected: PASS and APK assembles.

- [ ] **Step 5: Commit**

Commit message: `feat: add first run business setup`

### Task 5: Booking uses owner-created services/staff only

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuV12App.kt` or shared v1.3 appointment screens
- Modify: `app/src/main/java/com/elxvro/randevu/ui/V12Dialogs.kt` or v1.3 replacement
- Test: `app/src/test/java/com/elxvro/randevu/ui/V13BookingCatalogTest.kt`

**Interfaces:**
- Consumes: Task 1 `ServiceRecord`, Task 3 persisted services, existing `StaffRecord`/leave rules.
- Produces booking catalog projection using active saved services and active staff only.

- [ ] **Step 1: Write failing booking-catalog tests**

Assert zero services blocks save with service-directed message, inactive services are hidden, zero staff gives useful add-staff guidance, and no hard-coded salon service/person names are returned.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*V13BookingCatalogTest*'`
Expected: FAIL before projection is wired.

- [ ] **Step 3: Replace hard-coded service/staff options with persisted catalogs**

Keep existing leave/conflict validation; do not silently create staff/service records from typed appointment text.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: use owner business catalog for booking`

### Task 6: Business settings editing and safe reset

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuV13App.kt`
- Modify/Create: `app/src/main/java/com/elxvro/randevu/ui/BusinessSettingsScreens.kt`
- Test: `app/src/test/java/com/elxvro/randevu/business/BusinessResetPolicyTest.kt`

**Interfaces:**
- Produces editable `İşletme Bilgileri` and `Hizmetler` screens.
- Produces reset policy that explicitly distinguishes setup-only reset from setup+operational-data deletion.

- [ ] **Step 1: Write failing reset-policy tests**

Assert non-destructive reset keeps appointments/staff/leaves while clearing profile/setup/services as specified; destructive reset clears all after explicit confirmation path.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `gradle --no-daemon testDebugUnitTest --tests '*BusinessResetPolicyTest*'`
Expected: FAIL before policy exists.

- [ ] **Step 3: Implement settings editors and two-stage destructive confirmation**

No reset action fires directly from a row tap. Show exact data-loss scope before final confirmation.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add business settings and safe reset`

## Self-review

- Spec coverage: fresh-install emptiness, exact migration, setup gate, business profile, services, optional staff, persistence, booking catalog, editing, reset, theme consistency, and offline behavior are all assigned.
- Step scan: each task has a RED test, an exact implementation boundary, verification command, and commit boundary.
- Type consistency: `BusinessProfile` and `ServiceRecord` originate in Task 1 and are consumed unchanged by Tasks 3–6.
- Review focus coverage: exact migration is Task 2; corrupt storage/empty-state semantics Task 3; invalid timezone/setup interruption Task 1/4.
- Proportion: backend/WhatsApp delivery is intentionally excluded into the companion plan so this plan remains independently testable.