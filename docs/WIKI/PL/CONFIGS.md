# Konfiguracja (config.yml)

Ta strona opisuje główne opcje możliwe do ustawienia w `config.yml`, sposób strojenia oraz znaczenie dla Twojego serwera.

## Automatyczne migracje konfiguracji (bez utraty zmian)
- PunisherX śledzi `config-version` w pliku. Przy starcie i `/punisherx reload` plugin porównuje go z wbudowanym szablonem i automatycznie scala nowe klucze, zachowując Twoje wartości.
- Przy migracji ze starszych wersji PunisherX tworzy też kopię zapasową `config.yml.<oldVersion>.bak` przed modyfikacją pliku, więc zawsze możesz wrócić.
- Nie trzeba ręcznie resetować konfiguracji — wrzucasz nowy JAR, a konfiguracja aktualizuje się do najnowszego schematu z zachowaniem customowych ustawień.

## Zakres globalny (faza eksperymentalna)
- `server` — logiczna nazwa serwera używana do zakresowania kar w sieci; zostaw `"network"`, aby kary działały wszędzie, albo ustaw nazwy per-serwer (np. `"survival"`, `"skyblock"`) dla separacji kar.

## Baza danych i cache graczy
- `database.type` — wybierz `mariadb`, `mysql`, `mssql`, `postgresql`, `sqlite`.
- `database.sql.*` — host/port/dbname/username/password dla silników SQL (ignorowane dla SQLite).
- `playerCache.storage` — przechowuj szyfrowane IP graczy w `file` (per-serwer) lub `database` (współdzielone między serwerami w sieci).

## Logowanie kar i akcje po karze
- `kick.history` — jeśli `true`, kicki zapisują się do historii gracza.
- `actions.kicked` / `actions.mute` — komendy uruchamiane po kicku lub mutowaniu gracza (np. potrącenie waluty).
- `actions.warn.count` — mapowanie liczby warningów na komendy; w przykładach dostępne są placeholdery `{player}` i `{warn_no}`.

## Obsługa muta
- `mute.pm` — blokuje prywatne wiadomości podczas muta.
- `mute.cmd` — biała/czarna lista komend dla zmutowanych graczy, gdy `mute.pm` jest włączone.

## Powiadomienia
- `notifications.punishment_bar.enabled` — pokazuje action bar z odliczaniem dla aktywnych `jail`/`mute`.
- `notifications.punishment_bar.period_ticks` — częstotliwość odświeżania (20 ticków = 1 sekunda).

## Formatowanie placeholderów
- `placeholders.punishment_list_limit` — maksymalna liczba wierszy zwracanych przez placeholdery list.
- `placeholders.message_format` — format wyjściowy placeholderów: `MINI_MESSAGE`, `LEGACY_AMPERSAND`, `LEGACY_SECTION` lub `PLAIN_TEXT`.

## Przepływ jail i zwolnienia
- `jail.location` / `jail.radius` — środek więzienia i dozwolony promień.
- `jail.allowed_commands` — komendy dozwolone podczas pobytu w jailu (`*` pozwala na wszystko).
- `unjail.unjail_location` — domyślny punkt po zwolnieniu (używany, gdy `spawn_type_select.set` to `unjail`).
- `unjail.spawn_type_select.set` — wybór logiki odradzania: `unjail`, `last_location`, `bed`, `essx` (EssentialsX/FoliEssentials) lub `world`.

## Presety GUI (faza eksperymentalna)
- `gui.punish.times` — przyciski czasu widoczne w GUI.
- `gui.punish.reasons` — domyślne powody widoczne w GUI.
- `gui.punish.use_force` — dopisuje `--force` do komend wykonywanych z GUI, aby omijać standardowe bypass permissions.

## Język i aliasy
- `language` — wbudowane opcje: EN, PL, NL, ES, FR, DE, RU, UK, IT, PT, AR, KO; `auto` dobiera język automatycznie.
- `aliases.*` — własne aliasy komend dla każdej głównej komendy (ban, history, jail itd.).

## Integracje i aktualizacje
- `geoDatabase.licenseKey` — klucz MaxMind do wyszukiwania GeoIP.
- `webhook.discord.enabled` / `url` — włącza/wyłącza powiadomienia webhook Discord.
- `webhook.discord.username` / `avatar-url` — nadpisuje nazwę i avatar webhooka.
- `webhook.discord.colors.*` — kolory embedów per typ kary z fallbackiem `default`; przyjmuje wartości dziesiętne Discorda, hex (`#` lub `0x`) albo podstawowe nazwy typu `red`, `orange`, `blue`, `white`, `black`.
- `webhook.discord.embed.thumbnail-url` / `image-url` / `author.*` / `footer.icon-url` — sterowanie wyglądem embeda.
- `webhook.discord.embed.fields.*` — wybór pól widocznych w embedzie (gracz, operator, typ, powód, czas).
- `update.check-for-updates` / `auto-download` — kontrola sprawdzania aktualizacji i auto-downloadu; flagi `hangar`, `github`, `modrinth` wybierają źródła.

## Debugowanie i telemetry
- `debug` — włącza szczegółowe logowanie.
- `stats.enabled` — włącza/wyłącza anonimowe statystyki.
> Uwaga! Funkcje i ustawienia oznaczone jako eksperymentalne mogą działać niepoprawnie albo wcale; autor odradza używanie ich na serwerach produkcyjnych.

## Zgłoszenia i interfejsy

- `reports.player-interface` / `reports.admin-interface` — niezależny wybór `AUTO` (domyślnie), `GUI`, `DIALOG` lub `BOOK`.
- `reports.use-dialogs` / `reports.admin-use-dialogs` — domyślnie `true`; sterują odpowiednim interfejsem w trybie AUTO. Dialogi wymagają Paper 1.21.7+; starszy Paper i Spigot korzystają z ekwipunku/czatu.
- `reports.max-open-per-reporter` — domyślnie `3`, minimum `1`; limit otwartych zgłoszeń autora. Niezależnie od limitu autor może mieć tylko jedno otwarte zgłoszenie na daną osobę.
- `reports.page-size` — domyślnie `10`, zakres 1–50 dla list tekstowych.
- `reports.reasons` — kategorie powodów w menu gracza.
- `dialogs.use-change-reason` / `dialogs.use-list-views` — formularz edycji powodu i listy `/check`, `/history`, `/banlist` na obsługiwanym Paper; domyślnie `true`.
- `dialogs.list-page-size` — domyślnie `10`, zakres 1–20 dla list kar w dialogach.

[Zgłoszenia: instrukcja gracza i administratora](REPORTS.md). Oznaczenia i wstrzymania Anti-cheatów obsługuje się komendami, nie przez tę sekcję konfiguracji: [poradnik Anti-cheat](ANTICHEAT-MARKS.md).

## Bezpieczny YAML i aktualizacja bazy

`/prx validate` (lub `/punisherx validate`) sprawdza pliki bez ich zmieniania i zgłasza znalezione błędy. `/prx reload` i `/punisherx reload` wykonują taką samą kontrolę przed wyłączeniem działającej instancji. Sprawdzane są:

- `config.yml`,
- `punish-templates.yml`,
- `DBAPI_config.yml`,
- wybrany plik językowy, a przy `language: auto` wszystkie dostępne pliki `messages_*.yml`.

Jeśli YAML jest błędny, przeładowanie jest przerywane przed wyłączeniem zadań, listenerów, bazy i obsługi wiadomości. Dotychczasowa instancja nadal działa.

Własne pliki `lang/messages_*.yml` korzystają kolejno z poprawnej wartości użytkownika, wartości dołączonej do JAR, a następnie końcowego komunikatu zastępczego. Brak klucza nie powoduje przepisywania pliku. Jeśli plik językowy jest błędny przy starcie, plugin może użyć wbudowanego tłumaczenia w pamięci. Oryginał pozostaje zachowany bez zmian, a kopia bezpieczeństwa z datą trafia do `lang/backups/`.

H2 nie jest obsługiwane. Przed aktualizacją zachowaj kopię bazy i katalogu pluginu; starszą bazę H2 wyeksportuj lub przekonwertuj przed wymianą JAR. Restart jest wymagany przy aktualizacji pluginu.

## Nazwy LuckPerms

`name-formatting.operator-display` i `name-formatting.player-display` definiują układ nazw (domyślnie `$prefix$name$suffix`). `name-formatting.name-color-meta` wskazuje klucz metadanych koloru nicku (domyślnie `name-color`). [Placeholdery i przykład](PLACEHOLDERS.md).
