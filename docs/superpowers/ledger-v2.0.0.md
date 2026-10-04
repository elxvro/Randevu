# SDD ledger — plan: docs/superpowers/plans/2026-10-04-v2-core-online-platform.md

Execution method: Native.

Ruling: This harness has no mounted git checkout/worktree, so execution uses the isolated GitHub branch `build/v2.0.0-online-core` and this tracked ledger instead of a local .superpowers workspace. TDD evidence comes from GitHub Actions runs on each RED/GREEN commit. Cost if wrong: execution metadata is visible in git instead of ignored scratch space.

Pre-flight: Task 1 schema/errors feed Tasks 2–6 backend services; contracts align with the spec.
Pre-flight: Task 2 V2Auth business context feeds Tasks 3–6 private routes; tenant source is bearer token only.
Pre-flight: Task 3 catalog/staff repositories feed Tasks 4 and 6; all synchronized records use external UUID + version.
Pre-flight: Task 4 appointment/availability interfaces feed Task 5 public booking and Task 6 bootstrap; slot checks remain server-authoritative.
Pre-flight: Tasks 7–9 Android session/queue/coordinator feed Task 10 shell; token stays separate from non-secret session metadata.
Pre-flight: Task 10 v2 shell consumes server-owned reminder_state from Task 4; it must not reintroduce Android-owned WhatsApp scheduling.

Task 1: RED workflow 37230250214 failed on `v2 migration file missing` before production files existed.
Task 1: complete (commits d71666c..d383ba4, workflow 37230385897: PHP validation success; Android unit tests/build success; artifact upload success).

Ruling: Tasks 2–6 backend RED tests are committed together and CI is changed to execute every PHP test even after individual failures. This preserves test-first RED evidence for every backend contract while reducing GitHub runner usage. Cost if wrong: commit history is less granular than the written plan, but production code still follows only after all tests have demonstrably failed.
