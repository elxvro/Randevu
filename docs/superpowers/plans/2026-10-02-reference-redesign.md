# Reference Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Android UI to match the approved dark cyan/blue reference image as closely as practical while keeping the appointment app functional and visually consistent across all main screens.

**Architecture:** Add a single shared reference design contract and a new unified Compose app entry point. Reuse the existing appointment core and local persistence, but stop routing users through the layered legacy V4/V6/V7/V8 visual wrappers. All main screens will share one scaffold, one header system, one bottom navigation, one palette, one spacing grid, and one set of component shapes.

**Tech Stack:** Kotlin, Jetpack Compose Material3, existing AppointmentEngine/LiveSyncStore, JUnit 4.

**Spec:** User-approved reference image and explicit design constraints from the conversation.

## Global Constraints

- Single dark visual theme only; no second light theme.
- Reference palette: near-black navy background, blue/cyan accents, white primary text, blue-grey secondary text, restrained green/yellow/red status colors.
- Bottom navigation remains identical on all main screens with five entries: Ana Sayfa, Takvim, Müşteriler, Personel, Daha Fazla.
- Compact mobile density: 16dp horizontal page inset, 8/12/16dp spacing rhythm, 14–18dp component radii, 72dp bottom nav target height.
- System bars and safe areas must be respected.
- Default Material appearance must be overridden for cards, buttons, fields, dialogs, sheets and navigation.
- Existing appointment functionality and local persistence must continue to work.

## Review Focus

- Small Android screens must not overflow vertically or horizontally.
- Navigation must never swap to a different visual style between tabs.
- Dialogs/forms must use the same dark surface/border/radius system as main screens.
- Long customer/service names must ellipsize rather than expand card height unpredictably.
- Empty lists must render intentional compact empty states rather than blank screens.

---

### Task 1: Reference design contract

**Files:**
- Create: `app/src/test/java/com/elxvro/randevu/ui/ReferenceDesignContractTest.kt`
- Create: `app/src/main/java/com/elxvro/randevu/ui/ReferenceDesignContract.kt`

**Interfaces:**
- Produces `ReferenceDesignContract`, `ReferenceTab`, shared palette/spacing/radius/navigation constants.

- [ ] Write failing unit tests for fixed five-tab navigation, dark-only palette, compact radii and spacing values.
- [ ] Run `gradle --no-daemon testDebugUnitTest` and verify the tests fail because the contract does not exist.
- [ ] Implement the contract with exact reference values.
- [ ] Re-run unit tests and verify green.

### Task 2: Unified reference UI

**Files:**
- Create: `app/src/main/java/com/elxvro/randevu/ui/RandevuReferenceApp.kt`
- Modify: `app/src/main/java/com/elxvro/randevu/ui/AppTheme.kt`

**Interfaces:**
- Consumes `ReferenceDesignContract`, `Appointment`, `AppointmentEngine`, `LiveSyncStore`.
- Produces `RandevuReferenceApp()`.

- [ ] Build a single dark Material3 theme using contract tokens.
- [ ] Implement one shared scaffold/header/bottom navigation.
- [ ] Implement reference-style home dashboard, calendar, customers, staff and more/settings screens.
- [ ] Implement appointment creation dialog with shared field/button styling and persistence through `AppointmentEngine` + `LiveSyncStore`.
- [ ] Implement compact empty states, status chips and responsive list rows.
- [ ] Run unit tests and assemble debug APK.

### Task 3: Release integration

**Files:**
- Modify: `app/src/main/java/com/elxvro/randevu/MainActivity.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- MainActivity launches only `RandevuReferenceApp()`.

- [ ] Switch app entry point from legacy V8 wrapper to reference app.
- [ ] Bump to versionCode 11 / versionName 1.1.0.
- [ ] Run full `testDebugUnitTest assembleDebug`.
- [ ] Download produced APK artifact and verify workflow success.
