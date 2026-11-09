# Anti-cheat persistence



New metadata is stored in `punishment_marks` and `punishment_mark_pauses`; the existing `punishment_undos` audit is reused. Existing punishment tables and rows are retained. Marked active/history/metadata writes are atomic. A failed marked database write never falls back to an untracked platform ban. Issuance and marked reversals use a database transaction lock shared by server instances.

Exports, imports and database-engine migration include marks and pauses. The internal `punishment_mark_lock` table contains no player data and is initialized by the schema rather than exported. Paper identity migration and rollback also transfer mark ownership.
