# [Data: 2025-12-09 18:30] Integracja serwisu zapytań z eventami i schedulerem

## Co zostało zrobione
- Dodano w `punisherx-core` adapter `PunishmentCacheRefresher`, który asynchronicznie unieważnia i dogrzewa cache zapytań po zmianach w karach.
- W module Paper wprowadzono eventy `PunishmentAppliedEvent`/`PunishmentRevokedEvent` z listenerem, który korzysta z nowego adaptera cache.
- Komendy nakładające/znoszące kary oraz logika logowania/jailu publikują eventy, a stróż online i checkerzy korzystają z `PunishmentQueryService`, spinając ścieżkę punish→cache→event→query.
- Dodano testy jednostkowe spinające przepływ zdarzeń z serwisem zapytań w core i na warstwie Paper.

## Uzasadnienie
- Zgodnie z planem 2.0 cache Caffeine w core ma być jedynym źródłem prawdy dla zapytań – eventy i scheduler muszą go odświeżać po każdej mutacji.
- Integracja ścieżek nakładania/odwoływania kar z eventami i schedulerem eliminuje niespójności między bazą, cache i API platformowym.

## Sugerowany następny krok
- Rozszerzyć tę integrację na warstwy proxy/Folia (mostek Velocity/Folia) oraz dodać monitorowanie rozproszonego odświeżania cache przy komunikacji międzyserwerowej.
