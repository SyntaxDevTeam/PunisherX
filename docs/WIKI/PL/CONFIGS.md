# Konfiguracja (config.yml)

Ta strona opisuje każdą opcję możliwą do ustawienia w `config.yml`, sposób strojenia oraz znaczenie dla Twojego serwera.

## Automatyczne migracje konfiguracji (bez utraty zmian)
- PunisherX śledzi `config-version` w pliku. Przy starcie i `/punisherx reload` plugin porównuje go z wbudowanym szablonem i automatycznie scala nowe klucze, zachowując Twoje wartości.
- Przy migracji ze starszych wersji PunisherX tworzy też kopię zapasową `config.yml.<oldVersion>.bak` przed modyfikacją pliku, więc zawsze możesz wrócić.
- Nie trzeba ręcznie resetować konfiguracji — wrzucasz nowy JAR, a konfiguracja aktualizuje się do najnowszego schematu z zachowaniem customowych ustawień.

## Zakres globalny (faza eksperymentalna)
- `server` — logiczna nazwa serwera używana do zakresowania kar w sieci; zostaw `"network"`, aby kary działały wszędzie, albo ustaw nazwy per-serwer (np. `"survival"`, `"skyblock"`) dla separacji kar.

## Baza danych i cache graczy
- `database.type` — wybierz `mariadb`, `mysql`, `mssql`, `postgresql`, `sqlite` lub `h2`.
- `database.sql.*` — host/port/dbname/username/password dla silników SQL (ignorowane dla SQLite/H2).
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
- `language` — wbudowane opcje: EN, PL, NL, ES, FR, DE, RU, UK, IT, PT, AR.
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
