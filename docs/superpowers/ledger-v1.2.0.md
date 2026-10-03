# SDD ledger — plan: docs/superpowers/plans/2026-10-03-active-reminders-staff-leave.md
Pre-flight: Tasks 2→4 share StaffLeaveEngine; Tasks 2→5 share reminder preference; Tasks 3→4/5 share AppointmentReminderScheduler; Tasks 1→7 share layout contract. No conflicts found against approved spec.
Task 1: complete — layout contract set to 56dp header and 64dp navigation content; legacy token retained as a 64dp alias so older reference code still compiles.
Task 2: complete — persistent StaffRecord/StaffLeave domain and tolerant SharedPreferences JSON storage implemented; leave overlap rules are pure and unit-tested.
Task 3: complete — 24h/2h reminder planning, WorkManager scheduling, notification channel and worker implemented; planner tests green.
Ruling: implement Tasks 4–7 in a new `RandevuV12App.kt` presentation shell rather than further expanding the 42k legacy `RandevuReferenceApp.kt`; the approved visual tokens remain shared, while this isolates operational state and reduces regression risk. Cost if wrong: duplicated presentation code until the legacy reference screen is retired.
