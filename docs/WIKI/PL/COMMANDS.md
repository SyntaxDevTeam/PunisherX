## Podstawowe komendy

* `/ban <gracz> (czas) <powód> [--force]` — banuje i natychmiast wyrzuca gracza, jeśli jest online; przy problemie z bazą używa listy banów Paper `PROFILE` jako fallback.
* `/banip <ip|gracz|uuid> (czas) <powód> [--force]` — banuje wszystkie IP powiązane z celem, a przy błędach bazy uruchamia komendę Paper `ban-ip`; dodatkowo wyrzuca cele online.
* `/unban <ip|gracz|uuid>` — zdejmuje bany po nazwie gracza, IP lub UUID, łącznie z powiązanymi IP w bazie.
* `/jail <gracz> (czas) <powód> [--force]` — teleportuje gracza do skonfigurowanego więzienia (z uwzględnieniem bypassa, chyba że użyto `--force`) i zapisuje poprzednią pozycję do zwolnienia.
* `/unjail <gracz>` — usuwa aktywną karę więzienia z cache i teleportuje gracza do lokalizacji wyjścia z więzienia, jeśli jest ustawiona.
* `/setjail <radius>` — zapisuje lokalizację nadawcy i promień jako obszar więzienia w `config.yml`.
* `/setunjail` — zapisuje lokalizację nadawcy jako punkt po wyjściu z więzienia używany przez `/unjail`.
* `/mute <gracz> (czas) <powód> [--force]` / `/unmute <gracz>` — nakłada/zdejmuje muta na czat, z opcjonalnym `--force`, który ignoruje bypass dla graczy online.
* `/warn <gracz> (czas) <powód>` / `/unwarn <gracz>` — dodaje lub usuwa ostrzeżenia, obsługując ostrzeżenia czasowe wyzwalające akcje z konfiguracji.
* `/kick <gracz> <powód> [--force]` — natychmiast wyrzuca gracza z serwera z podanym powodem.
* `/clearall <gracz>` — usuwa aktywne kary gracza oraz powiadamia go, jeśli jest online.
* `/change-reason <penalty_id> <new_reason>` — aktualizuje zapisany powód istniejącej kary.
* `/check <gracz> <all|warn|mute|ban|jail>` — wyświetla aktywne kary (lub dane własne gracza), filtrowane po typie.
* `/history <gracz> (strona)` — stronicowana historia wszystkich kar danego gracza.
* `/banlist (strona) [--h]` — stronicowana lista aktualnie zbanowanych graczy; `--h` pokazuje bany historyczne.

## Szablony kar i panel

* `/punish <gracz> <szablon> (poziom)` — nakłada karę z `punish-templates.yml`. Bez podania poziomu wybiera kolejny poziom eskalacji na podstawie historii kar. Wymaga `punisherx.cmd.punish`.
* `/panel` — otwiera panel moderacji; wymaga `punisherx.cmd.panel` lub uprawnień zarządzania. Poszczególne akcje nadal sprawdzają własne uprawnienia.
* Panel gracza udostępnia wyszukiwanie, szczegóły kar i szybkie nakładanie kar z szablonów. Szybkie kary wymagają `punisherx.cmd.punish`, a edycja presetów czasu i powodu — `punisherx.manage` lub `punisherx.manage.*`. Presety są zapisywane w `gui.punish.times` i `gui.punish.reasons` w `config.yml`.

## Zgłoszenia graczy

| Komenda | Działanie | Uprawnienie |
| --- | --- | --- |
| `/report` | Wybór gracza i powodu w formularzu lub GUI ekwipunku. | Dostępne graczom; brak osobnej permisji. |
| `/report <gracz>` | Wybór powodu dla wskazanego gracza. | Dostępne graczom. |
| `/report <gracz> <powód>` | Wysłanie zgłoszenia z własnym powodem. | Dostępne graczom. |
| `/reports` | Otwarcie skrzynki zgłoszeń; w konsoli lista. | `punisherx.see.reports` lub uprawnienia zarządzania zgłoszeniami. |
| `/reports gui (strona)` | Otwarcie przeglądarki zgłoszeń. | Ten sam dostęp do odczytu; tylko gracze. |
| `/reports list (strona)` | Przegląd otwartych zgłoszeń. | Ten sam dostęp do odczytu. |
| `/reports view <id>` | Szczegóły zgłoszenia i decyzji, jeśli zostało zamknięte. | Ten sam dostęp do odczytu. |
| `/reports history (strona)` | Historia rozpatrzonych i odrzuconych zgłoszeń. | Ten sam dostęp do odczytu. |
| `/reports resolve <id> <uzasadnienie>` | Zamknięcie zgłoszenia jako rozpatrzonego. | `punisherx.manage.reports` lub uprawnienia zarządzania. |
| `/reports reject <id> <uzasadnienie>` | Zamknięcie zgłoszenia jako odrzuconego. | Ten sam dostęp do zarządzania. |

Powód i uzasadnienie decyzji muszą mieć 3–255 znaków. Nie można zgłosić siebie ani ponownie zgłosić tego samego gracza, gdy wcześniejsze zgłoszenie pozostaje otwarte. `reports.max-open-per-reporter` ogranicza liczbę otwartych zgłoszeń autora (domyślnie `3`). Zamknięcie zwalnia miejsce i nie nakłada automatycznie kary. Strony są numerowane od `1`. Wszystkie operacje administracyjne poza `gui` działają również w konsoli.

## Narzędzia administracyjne

`/prx` jest aliasem `/punisherx`. Poniższe operacje administracyjne wymagają `punisherx.cmd.prx`; na Paper `/prx help` jest obsługiwane osobno.

| Komenda | Działanie |
| --- | --- |
| `/prx help (strona)` | Pomoc dotycząca komend. |
| `/prx version` | Informacje o pluginie i wersji. |
| `/prx validate` | Sprawdzenie `config.yml`, `punish-templates.yml`, `DBAPI_config.yml` i wybranych tłumaczeń bez zmiany plików. |
| `/prx reload` | Walidacja YAML i przeładowanie pluginu. Błędny YAML zatrzymuje operację przed wyłączeniem działającej instancji. |
| `/prx diag` lub `/prx diagnostics` | Diagnostyka środowiska; na Paper także bibliotek załadowanych podczas działania. |
| `/prx export` | Eksport bazy danych. |
| `/prx import` | Import bazy danych. |
| `/prx migrate <źródło> <cel> [--force]` | Migracja między typami baz. Najpierw skonfiguruj połączenie docelowe; powtórz komendę, aby potwierdzić, lub użyj `--force`. |
| `/langfix` | Konwersja starszych placeholderów tłumaczeń. Wymaga `punisherx.cmd.prx`. |
| `/cache` | Najnowsze wpisy cache IP graczy dla każdego UUID. Tylko konsola. |

Migracja bazy jest niezależna od usługi migracji UUID na Paper używanej przez AuthGatewayX. Ta druga jest API integracji, a nie podkomendą `/prx migrate`.

## Platformy i składnia

Paper 1.21.7+ może używać natywnych dialogów dla zgłoszeń, `/banlist`, `/history`, `/check` i `/change-reason`. Na obsługiwanym Paper `/change-reason` lub `/change-reason <id>` może otworzyć formularz; nadal działa `/change-reason <id> <nowy_powód>`. Dialogami sterują `reports.use-dialogs`, `reports.admin-use-dialogs`, `dialogs.use-change-reason` i `dialogs.use-list-views`. Starszy Paper i Spigot używają GUI ekwipunku lub czatu. `/reports list` również może otworzyć natywny dialog, gdy jest włączony.

Aliasy w `config.yml` udostępniają alternatywne nazwy obsługiwanych komend. Na Spigot rejestracja komend dodatkowo sprawdza uprawnienia z `plugin.yml`; różnice dotyczące starszych węzłów opisuje strona [Uprawnienia](PERMISSIONS.md).

`<argument>` jest wymagany, `(argument)` opcjonalny, a `[--force]` to opcjonalna flaga. Przykłady czasu: `30s`, `10m`, `2h`, `7d`. Pominięcie czasu oznacza karę bezterminową. W obsługujących tę flagę komendach kar `--force` pomija ochronę celu; przy migracji bazy pomija potwierdzenie.
