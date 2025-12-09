# [Data: 2025-12-09 10:15] Przeniesienie logiki zapytań kar do core

## Co zostało zrobione
- Dodano w `punisherx-core` wspólny cache `PunishmentDataCache` (Caffeine) oraz serwis `PunishmentQueryService`, który filtruje stan aktywnych kar, obsługuje historię i deleguje listy banów/aresztów do repozytorium.
- Przełączono `PunisherXApiImpl` i inicjalizację pluginu Paper na nowy serwis core, dzięki czemu logika filtrowania, stanu mute/jail i budowania list aktywnych kar nie jest dublowana w warstwie platformowej.
- Rozszerzono testy jednostkowe (core + Paper API) o scenariusze filtracji typów, wykorzystania cache i raportowania statusów kar.

## Uzasadnienie
- Plan 2.0 zakłada centralizację logiki kar w `punisherx-core`; wspólny cache i serwis zapytań eliminują powielanie kodu na Paper i przygotowują grunt pod Folia/Velocity.
- Włączenie Caffeine w core zapewnia spójność zależności cache i umożliwia kontrolę zachowań (np. TTL) niezależnie od platformy.

## Sugerowany następny krok
- Zintegrować serwis zapytań z pozostałymi ścieżkami nakładania/odwoływania kar (eventy, scheduler), dodać adaptery platformowe do odświeżania cache oraz pokryć testami scenariusze przepływu „punish→cache→event→query”.
