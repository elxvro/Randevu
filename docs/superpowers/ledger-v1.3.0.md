# v1.3.0 execution ledger

Plan: `docs/superpowers/plans/2026-10-03-business-first-run-setup.md` + `docs/superpowers/plans/2026-10-03-whatsapp-reminder-backend.md`

- RED business/setup suite: workflow run 37103690422 failed as expected before v1.3 domain classes existed.
- Ruling: v1.2 seed appointment dates were generated at runtime, so the migration cannot reconstruct the original date. It removes known `seed-*` records only when ID plus every stable shipped field matches and the date is valid; any edit to a stable field is preserved.
- Execution: Native, continuous through APK.
