# [Data: 2025-12-08 21:29] Migracja API i core w stronę modularnej architektury

## Co zostało zrobione
- Wyniesiono publiczne kontrakty PunisherX (API, `PunishmentData`, provider instancji) do nowego modułu `punisherx-api`, aby odseparować je od kodu platformowego.
- Dodano podstawowe abstrakcje scheduler/repozytorium kar w `punisherx-core` i przełączono implementację Paper (`DatabaseHandler`, scheduler) na ich użycie.
- Rejestrowanie API na Paper odbywa się teraz przez `PunisherXApiProvider`, a lekkie testy jednostkowe weryfikują filtrowanie i zapytania w `PunisherXApiImpl`.

## Uzasadnienie
- Dokumentacja planu 2.0 wymaga przeniesienia API i logiki kar do wspólnych modułów; obecny krok odcina platformy od detali kontraktów i tworzy miejsce na dalszą migrację logiki core.
- Wspólne interfejsy schedulera i repozytorium zmniejszają zależność od Bukkit, co ułatwi późniejsze wdrożenie Folii i Velocity.

## Sugerowany następny krok
- Kontynuować migrację logiki kar (serwisy, cache, eventy) do `punisherx-core`, dopinając adaptery platformowe i poszerzając testy o scenariusze zapytań/budowania danych kar.
