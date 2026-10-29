# Poradnik administratora

[Strona główna](HOME.md) · [Komendy](COMMANDS.md) · [Uprawnienia](PERMISSIONS.md) · [English](../EN/ADMIN-GUIDE.md)

## Instalacja i aktualizacja

1. Wybierz edycję Paper/Folia albo Spigot zgodnie ze [stroną główną](HOME.md). Zainstaluj JAR w `plugins/` i uruchom serwer.
2. Ustaw `language: PL`, połączenie bazy, szablony w `punish-templates.yml` i uprawnienia kadry.
3. Wykonaj `/prx validate`, następnie `/prx reload`. Sprawdź `/prx version` i `/prx diag`.
4. Po wymianie JAR zawsze uruchamiaj serwer ponownie.

Przed aktualizacją zachowaj kopię katalogu `plugins/PunisherX` i bazy. Pierwszy start wymaga dostępu do repozytoriów bibliotek; nie trzeba instalować osobnego pluginu SyntaxDB. SQLite wystarczy do samodzielnego serwera. Sieć może używać MySQL/MariaDB, PostgreSQL albo SQL Server. H2 nie jest obsługiwane — przed aktualizacją wyeksportuj lub przekonwertuj dotychczasową bazę.

## Sieć i kopie danych

Ustaw tę samą bazę na backendach, a w mostach proxy zgodne dane połączenia. `server: network` oznacza wspólny zakres kar; nazwy lokalne, np. `survival`, rozdzielają zakresy. Most instaluje się dodatkowo na proxy. Zgłoszenia są wspólne dla całej bazy. [Instrukcja mostu Velocity](../../../punisherx-velocity-bridge/README.md).

`/prx export` i `/prx import` wykonują pracę w tle i zgłaszają wynik. W danej instancji może trwać jedna taka operacja naraz. Import dopisuje rekordy i nie zastępuje istniejącej bazy; konflikt powoduje wycofanie transakcji. Zachowaj również niezależną kopię bazy przed importem. `/prx migrate <z> <do>` zmienia silnik bazy: najpierw skonfiguruj połączenie docelowe, następnie powtórz komendę, aby potwierdzić, albo użyj `--force`. Wszystkie te operacje wymagają `punisherx.cmd.prx`.

## Uprawnienia kadry

Nadaj konkretne węzły z [listy uprawnień](PERMISSIONS.md), zamiast globalnego `*`. Dostęp do `/panel` daje `punisherx.cmd.panel` lub zarządzanie, ale każda akcja sprawdza własne uprawnienie. Szybkie kary szablonowe wymagają `punisherx.cmd.punish`; edycja presetów czasu i powodów wymaga `punisherx.manage` lub `punisherx.manage.*`.

Do odczytu zgłoszeń i powiadomień nadaj `punisherx.see.reports`, a do decyzji dodatkowo `punisherx.manage.reports`. Dostęp do `/unmark` (`punisherx.cmd.unmark`) pozostaw osobom odpowiedzialnym za integrację Anti-cheata. Na Spigot sprawdź również uprawnienia dyspozytora komend opisane w referencji.

## Codzienna moderacja i panel

Otwórz `/panel`, wyszukaj gracza, sprawdź aktywne kary i historię. Możesz też użyć `/check Steve all`, `/history Steve` i `/banlist`. Panel działa obecnie i obsługuje graczy online oraz offline.

Skonfiguruj drabiny kar w `punish-templates.yml`. `/punish Steve spam` dobiera kolejny poziom z historii; `/punish Steve spam 2` wskazuje poziom jawnie. `/unpunish Steve spam` cofa ostatnie niecofnięte zdarzenie szablonu i jego udział w eskalacji, zachowując historię. Każde wywołanie cofa jedno zdarzenie; wykonany kick i efekty zewnętrznych pluginów nie są odwracane. Zmiana nazwy lub usunięcie szablonu zrywa jego powiązanie z nową historią.

`--silent` wycisza powiadomienia czatu i ogłoszenia dla obsługiwanych komend, zachowując prywatne potwierdzenie operatora. Ekrany rozłączenia, egzekwowanie kar i historia nadal działają. `--force` omija ochronę gracza; obie flagi można łączyć. Szczegóły i składnia: [Komendy](COMMANDS.md).

## Zgłoszenia — od gracza do decyzji

Gracz używa `/report`, `/report Steve` lub `/report Steve Powód zgłoszenia`. Administrator otwiera `/reports`, weryfikuje dowody, osobno nakłada ewentualną karę i zamyka zgłoszenie przez `resolve` albo `reject` z uzasadnieniem. Historia decyzji jest dostępna przez `/reports history`.

[Pełna instrukcja zgłoszeń](REPORTS.md) obejmuje limity, komendy konsolowe, uprawnienia, książki, GUI i dialogi. Interfejs gracza i administracji wybiera się niezależnie przez `reports.player-interface` i `reports.admin-interface`.

## Integracja z Anti-cheatami

**Przypisz oznaczenie do automatycznych kar**, np. `ban Steve 7d Cheating --mark=grim.speed`. W razie fałszywych detekcji `/unmark pause grim.speed` cofa oznaczone kary i blokuje kolejne tej detekcji. Po naprawie użyj `/unmark resume grim.speed`. Zwykłe kary bez oznaczenia pozostają aktywne.

[Osobny poradnik Anti-cheat](ANTICHEAT-MARKS.md) opisuje zakresy serwerowe, cofanie kar IP, dziedziczenie oznaczeń i ograniczenia. Wstrzymanie przetrwa restart i nie jest omijane przez `--force`.

## Bezpieczna konfiguracja i rozwiązywanie problemów

- Wykonaj `/prx validate` przed przeładowaniem. Błędny YAML zatrzymuje `/prx reload` przed wyłączeniem działającej instancji.
- Brakujące klucze tłumaczeń korzystają z wersji dołączonej do JAR. Błędny plik językowy przy starcie pozostaje zachowany, otrzymuje kopię w `lang/backups/`, a plugin może użyć wbudowanego tłumaczenia.
- Gdy komenda nie działa, sprawdź uprawnienia, aliasy i wersję przez `/prx version`.
- Przy problemach z synchronizacją sprawdź połączenie bazy, `server` i logi mostu. Przy błędach zapisu znaków MySQL/MariaDB sprawdź kodowanie kolumn i połączenia.
- Jeśli stare przyciski książki nie reagują, otwórz menu ponownie. Dialogi wymagają Paper 1.21.7+; na Spigot użyj GUI lub BOOK.

[Konfiguracja](CONFIGS.md) opisuje ustawienia. Zgłaszając problem na [Discordzie](https://discord.gg/Zk6mxv7eMh) lub GitHub Issues, podaj wersję pluginu, platformę i wersję serwera, Javę, fragment logu oraz kroki odtworzenia.
