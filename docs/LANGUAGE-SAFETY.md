# Language and YAML safety

PunisherX validates runtime YAML without rewriting administrator-owned configuration.

## Language files

Existing `lang/messages_*.yml` files are handled by MessageHandler using a read-only overlay:

1. valid user value,
2. bundled value from the PunisherX JAR,
3. final missing-message fallback.

A missing key therefore no longer causes PunisherX to rewrite the whole language file.

If a language file contains invalid YAML during server startup, PunisherX can still start by using the bundled language in memory. The broken file is preserved byte-for-byte and MessageHandler creates a timestamped safety copy under `lang/backups/`.

## Reload safety

`/prx reload` and `/punisherx reload` perform a read-only preflight before any PunisherX teardown occurs. The preflight checks:

- `config.yml`,
- `punish-templates.yml`,
- `DBAPI_config.yml`,
- the configured language file, or all available `messages_*.yml` files when `language: auto` is enabled.

If any checked YAML file is invalid, reload is aborted before tasks, listeners, database services or the currently active MessageHandler are dismantled. The existing working instance continues to run.

## Manual validation

Administrators can run:

```text
/prx validate
```

or:

```text
/punisherx validate
```

The command performs the same read-only validation and reports every invalid file it finds. No file is changed.

## Language versioning

The historical `# Ver. x.y.z` language migration mechanism is disabled. Its helper code remains in MessageHandler only as a technical trace for a future redesign and is not executed by PunisherX or MessageHandler runtime paths.
