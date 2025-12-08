# [Data: 2025-12-08 19:02] Inicjalna struktura modułów PunisherX 2.0

## Co zostało zrobione
- Przeniesiono dotychczasowy kod pluginu Paper do nowego modułu `punisherx-platform-paper`, aby oddzielić warstwę platformową od wspólnego rdzenia.
- Dodano szkielet modułów `punisherx-api`, `punisherx-core`, `punisherx-compat`, wariantów platform (`punisherx-platform-folia`, `punisherx-platform-velocity`) oraz dodatków (`punisherx-addons-*`) wraz z minimalnymi konfiguracjami Gradle.
- Zdefiniowano wspólną konfigurację Gradle na poziomie root oraz włączono zależności modułów platformowych od nowego API/core, przygotowując projekt do dalszej migracji.

## Uzasadnienie
- Plan 2.0 zakłada podział na czytelne moduły oraz współdzielony rdzeń niezależny od platformy; przygotowanie struktury umożliwia iteracyjne przenoszenie istniejącej logiki bez blokowania rozwoju.
- Wspólna konfiguracja repozytoriów i toolchainu Kotlin/JVM upraszcza utrzymanie oraz spójność ustawień dla wszystkich modułów.

## Sugerowany następny krok
- Stopniowo przenosić interfejsy API i logikę kar do modułów `punisherx-api` i `punisherx-core`, aktualizując zależności modułów platformowych oraz dodając testy potwierdzające poprawność po migracji.
