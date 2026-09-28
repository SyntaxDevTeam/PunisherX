# Migracja UUID gracza

PunisherX udostępnia przez Bukkit ServicesManager usługę
`PunisherXIdentityMigrationService` przeznaczoną dla kontrolowanej migracji
`OFFLINE UUID -> PREMIUM UUID` koordynowanej przez AuthGatewayX.

Migracja obejmuje:

- aktywne kary w `punishments.uuid`,
- historię kar w `punishmenthistory.uuid`,
- zgłaszającego i podejrzanego w `reports`,
- dokładne UUID w `bridge_events.target`,
- zaszyfrowany player cache niezależnie od trybu `file` / `database`,
- lokalny `jail_cache.json`.

Dane SQL posiadają własny journal `punisherx_identity_migrations` oraz
`punisherx_identity_migration_items`. Cache IP i jail cache otrzymują osobne kopie
przed zmianą. Rollback odtwarza wyłącznie rekordy zapisane przez daną migrację.

Jeżeli pod docelowym UUID istnieje już wpis jail cache i jednocześnie istnieje wpis
źródłowy, migracja jest blokowana zamiast automatycznie scalać stan kary.

Publiczny bridge używa wyłącznie typów JDK, więc PunisherX nie zależy od klas
AuthGatewayX.
