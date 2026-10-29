# PunisherX — WIKI

Dokumentacja bieżącej gałęzi `main`. Funkcje konkretnego wydania sprawdzisz w [historii zmian](../../../CHANGELOG.md). PunisherX obsługuje kary, historię graczy, panel moderacji i zgłoszenia na Paper/Folia oraz Spigot, z opcjonalnymi mostami Velocity i BungeeCord.

## Zacznij tutaj

| Temat | Instrukcja |
| --- | --- |
| Instalacja i administracja | [Poradnik administratora](ADMIN-GUIDE.md) |
| Zgłoszenie naruszenia przez gracza | [Zgłoszenia — instrukcja gracza i administracji](REPORTS.md) |
| Składnia komend i flagi | [Komendy](COMMANDS.md) |
| Dostęp graczy i kadry | [Uprawnienia](PERMISSIONS.md) |
| Ustawienia i bezpieczne aktualizacje | [Konfiguracja](CONFIGS.md) |
| PlaceholderAPI i nazwy LuckPerms | [Placeholdery](PLACEHOLDERS.md) |
| Integracja własnego pluginu | [API dla programistów](API.md) |

## Integracja z Anti-cheatami

**Oznaczaj automatyczne kary według detekcji i cofaj skutki fałszywych alarmów.** `--mark=grim.speed` wiąże karę z detekcją, `/unmark` ją cofa, a `/unmark pause grim.speed` dodatkowo blokuje kolejne oznaczone kary do czasu naprawy. [Pełna instrukcja, przykłady i ograniczenia](ANTICHEAT-MARKS.md).

## Platformy i instalacja

Wybierz `PunisherX-Paper-<wersja>.jar` dla Paper/Folia lub `PunisherX-Spigot-<wersja>.jar` dla Spigot. Wymagana jest Java 21+ oraz spełnienie wymagań silnika serwera. Most Velocity wymaga Java 25+, a BungeeCord Java 21+; most instaluje się na proxy dodatkowo do pluginów na serwerach backend.

1. Pobierz plik z [Hangar](https://hangar.papermc.io/SyntaxDevTeam/PunisherX), [Modrinth](https://modrinth.com/plugin/punisherx) lub [GitHub Releases](https://github.com/SyntaxDevTeam/PunisherX/releases).
2. Umieść właściwy JAR w `plugins/` i uruchom serwer, aby wygenerować pliki.
3. Ustaw język, bazę i uprawnienia według poradnika administratora.
4. Wykonaj `/prx validate`, następnie `/prx reload`. Wymiana JAR wymaga restartu serwera.

Natywne dialogi wymagają Paper 1.21.7+; starszy Paper i Spigot korzystają z ekwipunku/czatu. Książki zgłoszeń są dostępne na obu edycjach. Bazy: SQLite, MySQL, MariaDB, PostgreSQL i SQL Server. **H2 nie jest obsługiwane** — przed aktualizacją wyeksportuj lub przekonwertuj starszą bazę H2.

[English](../EN/HOME.md) · [Pomoc Discord](https://discord.gg/Zk6mxv7eMh) · [Licencja MIT](../../../LICENSE)
