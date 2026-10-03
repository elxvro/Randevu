# Randevu v1.2.0 Active UX, Reminders & Staff Leave Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the approved dark navy + cyan redesign while fixing system-bar layout, activating visible actions, adding local 24h/2h appointment reminders, and adding a persistent staff leave calendar that blocks invalid bookings.

**Architecture:** Keep `RandevuReferenceApp` as the presentation shell but move shared operational state into focused domain/store helpers so appointment, staff, leave, and reminder state have one owner. Use `LiveSyncStore` for tolerant local JSON persistence, AndroidX WorkManager for deferred reminder work, and small Compose sheets/dialogs for active controls while preserving the current reference design tokens.

**Tech Stack:** Kotlin 1.9.24, Jetpack Compose Material 3, Android SDK 26–35, AndroidX WorkManager, SharedPreferences/JSON, JUnit 4, GitHub Actions/Gradle 8.7.

**Spec:** `docs/superpowers/specs/2026-10-03-active-reminders-staff-leave-design.md`

## Global Constraints

- Preserve the approved dark navy + cyan reference theme; do not add a light theme.
- Bottom navigation must remain the same five destinations and visual language.
- Header visual height becomes 56dp; bottom navigation content height becomes 64dp with navigation inset outside the content height.
- App remains fully usable offline; no FCM/server scheduler in v1.2.0.
- Local reminder offsets are exactly 24 hours and 2 hours before an appointment when those trigger times are still in the future.
- Android 13+ notification permission is requested from notification/reminder enable flow, not on app launch.
- A full-day staff leave blocks all appointments from start date through end date inclusive; partial-day leave blocks only overlapping times.
- Corrupt persisted individual JSON records must be skipped rather than crash app startup.
- Target version is `1.2.0`, versionCode `12`.

## Review Focus

- Gesture navigation and 3-button navigation devices: bottom nav labels/icons must remain fully visible above the system bar.
- Corrupt or partially missing persisted staff/leave JSON: startup must continue with valid records retained.
- Reminder scheduling around current time: past 24h/2h triggers must be skipped without cancelling other eligible reminders.
- Cross-midnight/multi-day leave boundaries: inclusive date blocking and optional time windows must not reject valid appointments outside the leave interval.
- Notification permission denied/revoked: appointments still save and the app remains usable without repeated permission loops.

---

### Task 1: Layout contract and system-inset fix

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/ReferenceDesignContract.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Test: `app/src/test/java/com/elxvro/randevu/ui/ReferenceLayoutContractTest.kt`

**Interfaces:**
- Consumes: existing `ReferenceDesignContract`, `ReferenceBottomNavigation`, `ReferenceHeader`.
- Produces: `headerHeightDp = 56`, `bottomNavContentHeightDp = 64`, a shell where Scaffold owns system insets and the screen body no longer applies duplicate status-bar padding.

- [ ] **Step 1: Write the failing layout contract test**

Assert `headerHeightDp == 56`, `bottomNavContentHeightDp == 64`, and the old fixed-height/inset coupling token is absent/replaced.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `gradle --no-daemon testDebugUnitTest --tests '*ReferenceLayoutContractTest*'`
Expected: FAIL against the current 64dp header / 72dp bottom-nav contract.

- [ ] **Step 3: Implement the contract and shell fix**

Update tokens and `RandevuReferenceApp` so the root Scaffold handles system insets once. Remove the extra root `.statusBarsPadding()`. Build bottom navigation as 64dp visual content plus `navigationBarsPadding()` outside that fixed content region. Keep icon/label colors and five destinations unchanged.

- [ ] **Step 4: Run the focused test and full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `fix: correct reference app system insets`

### Task 2: Staff and leave domain with tolerant persistence

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/staff/StaffModels.kt`
- Create: `app/src/main/java/com/elxvro/randevu/staff/StaffLeaveEngine.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/storage/LiveSyncStore.kt`
- Test: `app/src/test/java/com/elxvro/randevu/staff/StaffLeaveEngineTest.kt`

**Interfaces:**
- Produces: `StaffRecord(id: String, name: String, title: String, phone: String, active: Boolean)`, `StaffLeave(id: String, staffId: String, startDate: String, endDate: String, startTime: String?, endTime: String?, reason: String, createdAt: Long)`, `StaffLeaveEngine.blocks(staff: StaffRecord, leave: StaffLeave, date: String, time: String): Boolean`, `StaffLeaveEngine.canBook(staff: StaffRecord, leaves: List<StaffLeave>, date: String, time: String): Boolean`.
- Extends `LiveSyncStore` with `loadStaff()/saveStaff()`, `loadStaffLeaves()/saveStaffLeaves()`, `loadReminderEnabled()/saveReminderEnabled()`.

- [ ] **Step 1: Write failing leave-domain tests**

Cover full-day inclusive overlap, partial-day boundaries, non-overlap, inactive staff, invalid leave record skipping, and multi-day dates.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `gradle --no-daemon testDebugUnitTest --tests '*StaffLeaveEngineTest*'`
Expected: FAIL because the staff/leave domain does not yet exist.

- [ ] **Step 3: Implement models, overlap rules, and persistence**

Use `LocalDate`/`LocalTime` parsing inside `runCatching`; invalid persisted records are omitted. Seed staff only when no persisted staff list exists.

- [ ] **Step 4: Run focused and full unit tests**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add persistent staff leave domain`

### Task 3: Reminder planning domain and WorkManager scheduler

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/notifications/ReminderPlan.kt`
- Create: `app/src/main/java/com/elxvro/randevu/notifications/AppointmentReminderWorker.kt`
- Create: `app/src/main/java/com/elxvro/randevu/notifications/AppointmentReminderScheduler.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/notifications/AppointmentNotifier.kt`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/elxvro/randevu/notifications/ReminderPlanTest.kt`

**Interfaces:**
- Produces: `ReminderPlan.entries(appointment: Appointment, now: LocalDateTime): List<ReminderEntry>` where eligible offsets are 24h and 2h; deterministic unique work names `appointment-reminder-<id>-24h` and `appointment-reminder-<id>-2h`.
- Produces: `AppointmentReminderScheduler.schedule(context, appointment)`, `cancel(context, appointmentId)`, `rescheduleAll(context, appointments)`.

- [ ] **Step 1: Write failing reminder-plan tests**

Assert both offsets for a sufficiently future appointment, skip past triggers independently, deterministic work keys, and cancelled appointment produces no planned reminders.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `gradle --no-daemon testDebugUnitTest --tests '*ReminderPlanTest*'`
Expected: FAIL because planner/scheduler classes do not exist.

- [ ] **Step 3: Add WorkManager and implement planner/worker/scheduler**

Add `androidx.work:work-runtime-ktx`. Worker posts through the `appointment_reminders` notification channel and builds a `PendingIntent` that opens `MainActivity`. Scheduler uses unique one-time work per appointment+offset and replaces existing work on reschedule.

- [ ] **Step 4: Run focused and full tests**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: schedule local appointment reminders`

### Task 4: Active appointment operations and booking validation

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/core/AppointmentCore.kt` only if an existing reducer action required by the UI is missing
- Test: extend existing appointment domain tests or create `app/src/test/java/com/elxvro/randevu/ui/ReferenceBookingRulesTest.kt`

**Interfaces:**
- Consumes: Task 2 `StaffLeaveEngine`, Task 3 `AppointmentReminderScheduler`, existing `AppointmentEngine`.
- Produces: create/edit/status/cancel/delete flows that persist through `LiveSyncStore`; appointment create/edit rejects inactive or leave-blocked staff and exposes an inline Turkish error.

- [ ] **Step 1: Write failing booking-rule tests**

Pin validation for leave-blocked staff, inactive staff, and valid booking outside leave windows.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `gradle --no-daemon testDebugUnitTest --tests '*ReferenceBookingRulesTest*'`
Expected: FAIL until booking rules are integrated.

- [ ] **Step 3: Implement active appointment controls**

Make appointment cards open details; add edit/status/delete controls; persist every mutation; schedule/remap reminders after create/edit and cancel reminders after cancellation/deletion. Keep the same reference card/radius/color system.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: activate reference appointment workflows`

### Task 5: Notification Center and permission flow

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/notifications/AppointmentReminderScheduler.kt`

**Interfaces:**
- Consumes: reminder-enabled preference from Task 2 and reminder planner/scheduler from Task 3.
- Produces: bell button opens Notification Center; reminder master switch persists; Android 13+ permission is requested only on enable; center lists upcoming 24h/2h entries derived from future appointments.

- [ ] **Step 1: Add a pure notification-center projection test**

Create/test a function that maps future appointments + `now` to display rows and returns no rows for past/cancelled appointments.

- [ ] **Step 2: Run focused test to verify failure**

Run: `gradle --no-daemon testDebugUnitTest --tests '*NotificationCenter*'`
Expected: FAIL before projection implementation.

- [ ] **Step 3: Implement center, toggle, and permission handling**

Use `rememberLauncherForActivityResult(RequestPermission())`. Denial leaves reminders disabled at OS level without repeated prompts; local appointment save still works. Turning master reminders off cancels all unique reminder works; turning on reschedules eligible appointments.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add appointment notification center`

### Task 6: Staff management and leave-calendar UI

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Optional split if the reference file becomes unwieldy: create `app/src/main/java/com/elxvro/randevu/ui/StaffLeaveScreens.kt`

**Interfaces:**
- Consumes: Task 2 staff/leave models/store.
- Produces: active New Staff flow, Staff Detail, leave list/calendar markers, add/remove leave, active toggle, next-leave indicator.

- [ ] **Step 1: Add pure staff-screen projection tests**

Test next-leave selection and stable staff/leave grouping for a given current date.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `gradle --no-daemon testDebugUnitTest --tests '*Staff*Projection*'`
Expected: FAIL before projection helpers exist.

- [ ] **Step 3: Implement staff and leave UI**

Wire the existing empty Staff header and New Staff callbacks. Add Staff Detail and Add Leave forms with reference-theme cards, compact controls, inclusive date validation, optional time validation, and delete confirmation.

- [ ] **Step 4: Run full unit suite**

Run: `gradle --no-daemon testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: add staff leave calendar UI`

### Task 7: Remove dead controls, version bump, and release verification

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Modify: `app/build.gradle.kts`
- Test: all unit tests including `ReferenceThemeColorTest` and `ReferenceLayoutContractTest`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: no intentionally empty visible callbacks; versionCode 12/versionName 1.2.0; buildable debug APK.

- [ ] **Step 1: Audit visible callbacks and settings rows**

Replace remaining empty callbacks with working local screens/actions or remove the control. Preserve call/SMS intents and existing working search/filter controls.

- [ ] **Step 2: Bump app version**

Set `versionCode = 12` and `versionName = "1.2.0"`.

- [ ] **Step 3: Run the full required release gate**

Run: `gradle --no-daemon testDebugUnitTest assembleDebug`
Expected: all tests PASS, APK assembled successfully.

- [ ] **Step 4: Verify artifact and installability inputs**

Confirm GitHub Actions upload step succeeds and artifact contains `app-debug.apk`; keep startup color regression green.

- [ ] **Step 5: Commit**

Commit message: `release: prepare randevu v1.2.0`

## Self-review

- Spec coverage: layout, bottom navigation, appointment activation, reminder channel/permission/scheduling, notification center, staff persistence, staff leave calendar, booking blocking, malformed storage handling, and v1.2.0 release gate are all assigned to tasks.
- Step scan: each task has a red test, minimal implementation target, verification command, and commit boundary.
- Type consistency: staff/leave types are defined once in Task 2 and consumed by Tasks 4/6; reminder planner/scheduler are defined in Task 3 and consumed by Tasks 4/5.
- Review focus coverage: system-inset layout is Task 1; corrupt persistence and leave edges are Task 2; past reminder triggers and deterministic keys are Task 3; denied permission behavior is Task 5.
- Proportion: plan intentionally keeps implementation bodies out and records only interfaces, exact values, test evidence, and boundaries required by the approved spec.
