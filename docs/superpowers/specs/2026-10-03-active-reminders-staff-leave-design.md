# Randevu v1.2.0 — Active UX, Appointment Reminders, Staff Leave

Date: 2026-10-03
Status: Design approved in chat; written spec pending user review
Base: `build/v1.1.0-reference-redesign` at startup-fix head

## Goal

Preserve the approved dark navy + cyan reference design while fixing system-bar spacing and making the application operational rather than presentation-only. The release must add working appointment reminders and a persistent staff leave calendar, and must remove or replace dead UI actions.

## Success criteria

1. The bottom navigation is fully visible on gesture and 3-button Android navigation devices.
2. The top content starts closer to the system status bar without clipping or double safe-area padding.
3. Visible controls in the redesigned screens perform a real local action, navigate to a working screen/sheet, or are deliberately removed; no decorative dead buttons remain.
4. Appointments can be created, edited, status-changed, cancelled/deleted, and persisted locally.
5. Appointment reminders can be enabled and are scheduled locally for 24 hours and 2 hours before an appointment when those times are still in the future.
6. Rescheduling an appointment replaces its old reminder jobs; deleting/cancelling an appointment removes its future reminder jobs.
7. Android 13+ notification permission is requested only when needed, with a clear disabled state if denied.
8. Staff leave can be created, viewed, edited/removed, and persisted locally.
9. A staff member on leave cannot be selected for an overlapping appointment date/time; the UI explains why.
10. Existing reference theme, card language, radii, typography, and navigation style remain visually consistent.

## UI and layout changes

### Safe areas and header

The current root applies Scaffold content padding and an additional `statusBarsPadding()`, which can create excessive top spacing depending on device/inset handling. The new layout will centralize system insets at the app shell.

- Root Scaffold owns system-bar insets.
- Screen bodies do not add a second status-bar inset.
- Header visual height becomes 56dp while preserving touch target sizes.
- Home header top content padding is reduced so the avatar/name row sits closer to the status bar without clipping.
- Horizontal page inset remains 16dp.

### Bottom navigation

The current fixed 72dp row applies `navigationBarsPadding()` inside that fixed height, which can consume part of the content height. The new shell will separate content height from the navigation inset.

- Navigation content height: 64dp.
- System navigation inset is added outside/under the content height.
- Icons: 21–22dp; labels: 9–10sp.
- All five items remain: Ana Sayfa, Takvim, Müşteriler, Personel, Daha Fazla.
- Selected/unselected cyan/muted styling remains unchanged.

## App activity model

The redesign remains offline-capable. Backend synchronization is not required for this release.

### Appointment operations

The following operations must be wired to persistent local state:

- Create appointment.
- Open appointment details.
- Edit customer, phone, service, staff, date, time, note.
- Change status: Pending / Confirmed / Completed / Cancelled.
- Delete appointment with confirmation.
- Search/filter existing appointment lists where present.
- Customer quick actions: call, SMS, create appointment.

All appointment mutations must flow through the existing appointment domain reducer where practical, then persist through `LiveSyncStore`.

### Dead-action rule

Any currently visible action that has an empty callback must be resolved before release. For this version:

- Home notification button opens Notification Center.
- Staff header/action opens Add Staff or Staff Management.
- New Staff works and persists staff data.
- Staff card opens Staff Details / Leave Calendar.
- Settings rows open compact functional screens/sheets for Notifications, Services, Business Info, Data/Sync status, Security/App Lock placeholder only if a real toggle exists, Help/About information.
- If a legacy setting cannot be implemented meaningfully in this release, it is removed rather than shown as a dead row.

## Notification architecture

### Notification channel

Create one Android notification channel:

- ID: `appointment_reminders`
- Name: `Randevu Hatırlatmaları`
- Importance: default/high enough to be visible without being intrusive.

### Permission

- Android 13+ uses `POST_NOTIFICATIONS` runtime permission.
- The app requests permission from the notification settings/enable action, not immediately on first launch.
- If denied, the in-app notification center remains usable and shows that system notifications are disabled.

### Scheduling

Use AndroidX WorkManager one-time work requests for robust local scheduling without requiring exact-alarm special access.

For each appointment:

- Schedule reminder A at appointment minus 24 hours if future.
- Schedule reminder B at appointment minus 2 hours if future.
- Use unique work names based on appointment ID + offset so updates replace existing work.
- Cancel both works on appointment deletion or cancellation.
- Recompute both works whenever appointment date/time changes.

Notification content:

- Title: `Randevu Hatırlatması`
- Body includes customer, service, time, and staff where available.
- Tapping opens the app.

### In-app Notification Center

The home bell button opens a screen/sheet showing:

- Notification permission state.
- Reminder master switch.
- Upcoming reminder entries derived from future appointments.
- Reminder timing labels (24 saat önce / 2 saat önce).
- Empty state when no future reminders exist.

The master switch is persisted locally. Turning it off cancels scheduled reminder work; turning it on reschedules eligible future appointments.

## Staff model and leave calendar

### Persistent staff model

Introduce a small persistent staff store independent of appointment-derived staff names.

Fields:

- id
- name
- title
- phone (optional)
- active

Seed initial staff from existing reference/demo names only when no persisted staff records exist.

### Leave model

`StaffLeave` fields:

- id
- staffId
- startDate (`YYYY-MM-DD`)
- endDate (`YYYY-MM-DD`)
- optional startTime/endTime for partial-day leave
- reason
- createdAt

Default leave is full-day when times are omitted.

### Leave UI

Personel tab gains:

- Staff list with active status and next leave indicator.
- Tap staff -> Staff Detail.
- Staff Detail contains `İzin Takvimi` section.
- Add Leave form: start date, end date, optional time range, reason.
- Upcoming leave list.
- Remove leave with confirmation.
- Calendar/day markers in the same dark/cyan design language.

### Booking validation

Before saving an appointment:

1. Selected staff must be active.
2. Appointment date/time must not overlap a leave interval for that staff.
3. Existing appointment collision rules remain in force where available.
4. If blocked, the form stays open and shows an inline error such as `Bu personel seçilen tarihte izinli.`

A full-day leave blocks all appointments on dates from startDate through endDate inclusive. A partial-day leave blocks only overlapping times.

## Storage

Extend local persistence rather than introduce a database migration in this release.

`LiveSyncStore` (or a focused companion store) will persist:

- staff list
- staff leave list
- reminder-enabled preference

Storage encoding must be tolerant of malformed records: invalid individual JSON records are skipped rather than crashing app startup.

## State ownership

`RandevuReferenceApp` currently owns appointment state. v1.2.0 will promote shared operational state to the app shell:

- appointments
- staff
- leave entries
- notification preference
- selected navigation destination

Child screens receive data and event callbacks. This avoids separate per-screen copies and ensures a leave added in Personel immediately affects the New Appointment form.

## Error handling

- Invalid date/time: inline validation, no crash.
- Missing customer/staff/service: inline validation.
- Notification permission denied: informative state, no repeated prompt loop.
- WorkManager scheduling failure: appointment save still succeeds; the notification center shows reminder scheduling as unavailable/disabled where detectable.
- Corrupt persisted JSON: skip invalid records and continue startup.

## Testing strategy

### Unit tests

Add pure-domain tests for:

- Full-day leave overlap.
- Partial-day leave overlap boundaries.
- Non-overlapping leave allows booking.
- Inactive staff blocks booking.
- Reminder schedule generation for 24h/2h offsets.
- Past reminder offsets are skipped.
- Cancel/reschedule work-key generation is deterministic.
- Storage decoding skips malformed staff/leave records where testable without Android runtime.

### Regression tests

- Keep the existing theme-color startup regression test.
- Add layout contract tests for nav content height vs navigation inset policy and header height token.

### CI gate

The release is not complete until GitHub Actions successfully runs:

`gradle --no-daemon testDebugUnitTest assembleDebug`

and uploads the debug APK artifact.

## Versioning

Target release: `1.2.0`

- versionCode increments from current 11 to 12.
- versionName becomes `1.2.0`.

## Out of scope

- Remote push notifications / FCM.
- SMS sending service.
- Server-side reminder scheduler.
- Multi-device real-time synchronization.
- Payroll/HR leave approval workflow.
- Exact-alarm special permission flow.

These can be added later without changing the local reminder and leave domain models defined here.
