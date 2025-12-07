# PunisherX Velocity Bridge

This module lets a Velocity 3.4 proxy consume PunisherX ban events from the same database used by the Bukkit-side plugin.

## Konfiguracja
- `velocity-bridge/src/main/resources/bridge.properties` (automatycznie kopiowany do folderu danych) zawiera parametry host/port/database/username/password – ustaw je dokładnie tak samo jak w `database.sql.*` na serwerach z PunisherX, aby oba końce patrzyły w tę samą bazę i tabelę `bridge_events`.
- `poll-interval-ms` kontroluje jak często proxy odpytuje kolejkę; domyślnie 1000 ms, minimalnie 200 ms.

## Jak to działa
1. PunisherX podczas BAN/BANIP zapisuje zdarzenie w tabeli `bridge_events` (po stronie serwera Bukkit), niezależnie od tego, czy ktoś jest online.
2. Velocity bridge przy starcie tworzy tabelę (jeśli jej brakuje) i uruchamia cykliczne zadanie schedulera, które co `poll-interval-ms` pobiera nieprzetworzone rekordy i od razu disconnectuje pasujących graczy.
3. Po obsłużeniu wpis jest oznaczany `processed=1`, więc kolejne odpytywania nie wykonują kicka ponownie.

Efekt: nawet komenda z konsoli na pustym serwerze trafia do wspólnej bazy, a proxy samodzielnie odpyta kolejkę i wyrzuci gracza, jeśli jest online na innym serwerze.
