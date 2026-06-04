# Witamy!

PunisherX to rozbudowany system kar dla serwerów Minecraft opartych o Paper/Folia. Obejmuje komendy takie jak `kick`, `warn`, `mute`, `jail`, `ban` i `ban-ip` (czasowe lub permanentne), a także narzędzia `/check`, `/history` i `/banlist` do szybkiego monitoringu. Konfigurowalne szablony kar zapewniają spójność egzekwowania zasad, a w pełni tłumaczalny plik wiadomości wspiera formaty MiniMessage, Legacy i plain Minecraft. Plugin ma bogaty plik konfiguracyjny i wspiera MySQL/MariaDB lub PostgreSQL do synchronizacji między serwerami; dla lekkich wdrożeń standalone dostępne są SQLite oraz H2.

## Informacje techniczne
- Zaprojektowany pod Minecraft 1.20.6 - 1.21+, zoptymalizowany dla forków Paper, Pufferfish, Purple i Leaves (z pełną kompatybilnością z Folią i jej forkami!)
- Napisany we współczesnym Kotlinie, wymaga Java 21 lub nowszej
- Plik językowy z pełnym wsparciem formatowania MiniMessage i Legacy
- Obsługuje bazy MySQL/MariaDB, MSSQL, PostgreSQL, SQLite i H2
- Rozbudowany plik konfiguracyjny [sprawdź domyślne ustawienia tutaj](https://github.com/SyntaxDevTeam/PunisherX/blob/main/src/main/resources/config.yml)
- **Automatyczne migracje** przy zmianach w konfiguracji — zawsze masz najnowszą wersję bez utraty bieżących ustawień
- Opcjonalny checker aktualizacji z automatycznym pobieraniem update’ów pluginu
- Geolokalizacja IP (wymaga darmowego klucza licencji MaxMind)
- Integracja **Discord webhook** i wsparcie **PlaceholderAPI** z listą przydatnych placeholderów
- Proste komendy eksport/import/migrate bazy, fallbackowy system banów i czyszczenie kar jedną komendą
- Pełny system jaila przez komendy lub konfigurację

Masz pytania? Dołącz do naszego [Discorda](https://discord.gg/Zk6mxv7eMh).

## Pierwsze kroki

### Pobieranie
* Stabilna, zawsze aktualna wersja do pobrania na:
    * Modrinth ![Modrinth Downloads](https://img.shields.io/modrinth/dt/VCNRcwC2)
    * Hangar   [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* Wersje SNAPSHOT i DEV są od razu dostępne na Githubie [![GitHub Release Date](https://img.shields.io/github/release-date/SyntaxDevTeam/PunisherX)](https://github.com/SyntaxDevTeam/PunisherX/releases)
* Możesz też samodzielnie zbudować wersję developerską [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX) lub pobrać ostatni poprawny build [![Build Plugin](https://github.com/SyntaxDevTeam/PunisherX/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/SyntaxDevTeam/PunisherX/actions/workflows/gradle.yml)

### Instalacja
1. Pobierz najnowsze wydanie PunisherX
2. Umieść plik JAR w folderze `plugins` serwera
3. Uruchom serwer Minecraft
4. Skonfiguruj plugin przez `config.yml`
5. Zrestartuj serwer Minecraft
6. Gotowe.

### Dokumentacja
- Praktyczny poradnik dla administracji i moderatorów:
  - [ADMIN-GUIDE (PL)](ADMIN-GUIDE.md)

## Kontakt
Potrzebujesz pomocy lub masz pytania? Napisz na [Discordzie](https://discord.gg/Zk6mxv7eMh).

## Licencja
Ten plugin jest licencjonowany na MIT License. Szczegóły znajdziesz w pliku [LICENSE](https://github.com/SyntaxDevTeam/PunisherX/blob/main/LICENSE).

**Dzięki za korzystanie z PunisherX! Mamy nadzieję, że spełni Twoje oczekiwania. 😊**
