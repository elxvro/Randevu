# v1.3.1 execution ledger

Plan: `docs/superpowers/plans/2026-10-03-business-first-run-setup.md` + `docs/superpowers/plans/2026-10-03-whatsapp-reminder-backend.md`

- RED business/setup suite: workflow run 37103690422 failed as expected before v1.3 domain classes existed.
- GREEN business/setup suite: workflow run 37104196955 passed after first-run setup, persistent services, setup gate, migration, and owner-created catalog were implemented.
- RED WhatsApp backend suite: workflow run 37104547413 failed at the PHP validation gate before the queue/auth/Cloud API classes existed.
- GREEN WhatsApp core suite: workflow run 37162621595 passed after queue, API contract, persistence codec and mutation mapping were implemented.
- v1.3.1 integration: Android now persists WhatsApp settings/server session/pending operations, exposes setup/status UI, queues offline appointment mutations, flushes them after backend acknowledgement, and never claims a message as scheduled before the server confirms it.
- Backend safety: disabling WhatsApp cancels pending/processing reminder rows for that business.
- Ruling: v1.2 seed appointment dates were generated at runtime, so the migration cannot reconstruct the original date. It removes known `seed-*` records only when ID plus every stable shipped field matches and the date is valid; any edit to a stable field is preserved.
- Ruling: fresh installations need an authenticated way to connect a privately hosted backend. v1.3 adds one-time `/auth/bootstrap` using server-only `RANDEVU_APP_SETUP_KEY`; the setup key is sent only over HTTPS and is exchanged for a normal expiring bearer token. Meta credentials remain backend-only.
- Execution: Native, continuous through APK.
