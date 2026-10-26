# Analiza i testy po migracji na SyntaxDB — 2026-10-26

## Wyniki końcowe

`buildAll` zakończył się powodzeniem z włączonym testem lokalnej MariaDB.
Dodano 81 wspólnych scenariuszy wykonywanych osobno dla Paper i Spigot,
2 testy zgodności komunikatów Spigot i 6 testów bazy Velocity: łącznie
170 nowych wykonań testów.

| Moduł | Testy | Błędy | Pominięte |
| --- | ---: | ---: | ---: |
| Paper/Folia | 195 | 0 | 0 |
| Spigot | 101 | 0 | 0 |
| Velocity Bridge | 6 | 0 | 0 |
| Razem | 302 | 0 | 0 |

Gotowe artefakty serwerowe:

| Artefakt | Bajty | KiB |
| --- | ---: | ---: |
| `punisherx-paper/build/libs/PunisherX-Paper-1.8.1-EXPERIMENTAL.jar` | 994 323 | 971,0 |
| `punisherx-spigot/build/libs/PunisherX-Spigot-1.8.1-EXPERIMENTAL.jar` | 906 280 | 885,0 |

Są to kompletne artefakty przeznaczone do instalacji, korzystające z loaderów.
Nie należy mylić ich ze zwykłymi JAR-ami bez zależności bootstrapa ani z mostami proxy.

## Wynik analizy

Przegląd objął konfigurację zależności, loadery, schematy, zapytania
`DatabaseHandler`, transakcje, kopie bazy, cofanie szablonów oraz wywołania
warstwy danych z komend Paper/Folia i Spigot. Nowe testy korzystają z rzeczywistego
SyntaxDB i plików SQLite. Bukkit, odbiorcy komunikatów i usługi zewnętrzne są
zastępowane atrapami; baza, sterownik JDBC i pula nie są mockowane.

Wspólne testy znajdują się w `tests/server/kotlin` i są kompilowane niezależnie
dla obu platform. Adaptery `SyntaxDbCommandIntegrationTest` uruchamiają rzeczywiste
klasy komend danej platformy. Asercje integralności danych dodatkowo korzystają
z niezależnego połączenia JDBC, zamiast potwierdzać zapis wyłącznie tym samym
kodem odczytu pluginu.

## Potwierdzone i poprawione problemy

| Problem | Zachowanie po poprawce |
| --- | --- |
| Odczyt wygasłej kary usuwał najnowszą karę tego samego typu, także aktywną | Usuwany jest dokładny rekord wskazany przez ID wygasłej kary |
| Strony kar i historii przy tym samym `start` nie miały jednoznacznej kolejności | Sortowanie po `start DESC, id DESC` |
| Usuwanie po ID zwracało sukces również dla nieistniejących/niedostępnych rekordów | Wynik zależy od liczby zmienionych wierszy SyntaxDB |
| Zmiana powodu historii nie respektowała zakresu serwera i zgłaszała sukces dla brakującego ID | Zapytanie uwzględnia lokalny serwer oraz `network` i sprawdza liczbę zmian |
| Usuwanie danych gracza mogło pozostawić częściowo usunięte aktywne kary i referencje | Wszystkie operacje wykonują się w jednej transakcji |
| Usuwanie raportu zostawiało jego decyzję i zgłaszało sukces dla brakującego ID | Raport i decyzja są usuwane transakcyjnie, wynik sprawdza usunięty wiersz |
| Szablon `/punish` BANIP po błędzie zapisu uruchamiał stały ban vanilla oraz dopisywał historię i powiadomienia | Tylko zapisane adresy otrzymują historię i powiadomienie; częściowy błąd jest jawnie zgłaszany |
| Most komunikatów Spigot używał starego serializera Bungee wywołującego API usunięte z Adventure 5 | Konwersja przez aktualny serializer Gson i parser komponentów Bungee; test zachowania tekstu, koloru, hover i komendy kliknięcia |
| SyntaxDB i wszystkie zależności były pakowane do JAR-a mimo loaderów | Paper/Spigot korzystają z `compileOnly` i jawnej konfiguracji bibliotek pobieranych przy starcie |

Testy regresyjne potwierdziły awarie przed poprawkami. Kontrola transakcji
wstrzykuje błędy przez triggery SQLite: awaria następuje po wcześniejszym zapisie
lub usunięciu i wymusza sprawdzenie rzeczywistego rollbacku.

## Zakres nowych testów

| Obszar | Scenariusze |
| --- | --- |
| Komendy nakładania kar | `/warn`, `/mute`, `/ban`, `/banip`, `/jail`, `/kick`: czasowe/stałe kary, historia, ID, Unicode, zapis wielu IP, cache więzienia, brak lokalizacji, opcja historii kicka |
| Komendy usuwania | `/unwarn`, `/unmute`, `/unban`, `/clearall`: właściwy typ kary, najnowsze ostrzeżenie, osobne IP, zachowanie historii i obcego zakresu |
| Szablony | `/punish`, `/unpunish`: eskalacja z historii, ostatni poziom, wszystkie typy szablonów, wspólne zdarzenie BANIP, cofnięcie i znacznik historii, błędny poziom/czas, brak szablonu, awaria odczytu eskalacji, całkowita i częściowa awaria zapisu IP |
| Komendy odczytu | `/history`: druga strona; `/check`: filtrowanie typu i zakresu; `/banlist`: niezależny odczyt aktywnych banów i historii |
| Pozostałe ścieżki komend | Brak argumentów, brak uprawnień, `/change-reason`, przekazywanie trybu silent, brak akcji/powiadomień po nieudanych zapisach |
| Dane i zakresy | Wszystkie mapowane pola, klucze generowane, lokalny serwer plus `network`, obcy serwer, normalizacja zakresu, ostrzeżenia aktywne/wygasłe, IPv6 w SQLite, parametry zamiast interpolacji SQL |
| Listy i statystyki | Stabilne strony, ostatnie dziesięć wpisów, filtry GUI, ban/jail listy, liczby kar i historii, granica początku dnia |
| Raporty | Duplikaty, limit, liczba zgłoszeń podejrzanego, zwolnienie limitu po decyzji, walidacja decyzji, zachowanie metadanych, równoległe zgłoszenia na jednej instancji handlera, błędne UUID, strony, usuwanie i rollback, błędy bazy |
| Cache i lifecycle | Kolejność, Unicode, pusta lista, rollback po nieudanym insercie, idempotentny schemat, otwarcie nowego handlera na tym samym pliku, błąd schematu, nieudany insert bez webhooka |
| Backup | Wywołania eksportu/importu handlera, pusta baza, brak/błędny plik, zachowanie ID, walidacja całego snapshotu przed zapisem, atomowa podmiana pliku i usuwanie pliku tymczasowego, parser INSERT, komentarze, NULL, apostrofy, duplikaty kolumn, niedozwolone SQL |
| Konfiguracja | Wszystkie udokumentowane aliasy, nieobsługiwane silniki, porty domyślne/jawne, własny URL, nazwa i ścieżka SQLite, kombinacje parametrów stronicowania SQL Server |
| Runtime | Zgodność rozwiniętych manifestów z wersjami testowanych bibliotek; osobny classloader z platformowym rodzicem ładuje SyntaxDB, Hikari, cztery sterowniki i Protobuf oraz wykonuje zapis/odczyt SQLite i zamknięcie puli |
| Velocity | Kolejność i limit kolejki, wszystkie pola zdarzenia, oznaczenie przetworzenia z czasem, filtrowanie banów według zakresu/typu/ważności, najnowszy ban, zamknięta pula |

Dotychczasowe testy migracji starej bazy, cofania kar, migracji tożsamości,
uruchamiania pluginu, API, schedulerów, teleportacji i zasobów językowych pozostają
w zestawie. Opcjonalny `MySqlMigrationContractTest` został również wykonany na
jednorazowej MariaDB 11.8.6 ze schematem domyślnie latin1: test sprawdza utf8mb4,
klucze, cofanie i transfer SQLite ↔ MariaDB.

## Dlaczego JAR miał ponad 26 MB

`implementation(libs.syntaxdb)` pobiera pełną dystrybucję: core, mapping,
coroutines, cztery sterowniki i zależności przechodnie. `shadowJar` dołączał cały
ten runtime. Sam artefakt `syntaxdb` jest jedynie agregatorem zależności.

Przed poprawką pełny Paper JAR miał 25 879 194 bajty, Spigot 27 658 088 bajtów.
Przybliżony udział skompresowanych wpisów JAR-a:

| Zawartość | Rozmiar |
| --- | ---: |
| SQLite z bibliotekami natywnymi dla wielu platform | 11,40 MiB |
| Kotlin Reflect | 2,69–2,73 MiB |
| MySQL Connector/J | 2,28 MiB |
| Protobuf i pozostałe `com/google` | 1,61 MiB |
| SQL Server JDBC | 1,31 MiB |
| Kotlin Coroutines | 1,25 MiB |
| PostgreSQL JDBC | 1,06 MiB |

Paper/Spigot pobierają teraz `syntaxdb-core`, HikariCP i cztery sterowniki.
Nie pobierają nieużywanych modułów mapping/coroutines na potrzeby SyntaxDB.
Wersje bibliotek znajdują się w `gradle/libs.versions.toml`; jawny Protobuf jest
potrzebny szczególnie dla Libby, które nie rozwiązuje POM-ów przechodnich.
`SyntaxLoader` ma również repozytoria zapasowe dla zależności bibliotek prywatnych.
Kotlin Spigot ładuje Bukkit przed utworzeniem klasy pluginu.

`verifyDatabaseBundle` odrzuca obecność bibliotek pobieranych w JAR-ach serwerowych,
brak zadeklarowanej zależności i nierozwinięte wersje. Mosty proxy nadal pakują
biblioteki, ponieważ nie mają odpowiednika SyntaxLoader/Libby; kontrola buildu
sprawdza dla nich kompletność paczki.

## Uruchomienie

```bash
./gradlew buildAll --offline --console=plain
```

`--offline` wymaga uprzednio pobranych zależności Gradle; można go pominąć.
Włączenie testu MySQL/MariaDB na pustej, jednorazowej lokalnej bazie:

```bash
PUNISHERX_TEST_MYSQL_URL=jdbc:mysql://127.0.0.1:33367/punisherx_syntaxdb_test \
  ./gradlew buildAll --offline --console=plain
```

Zmienna URL jest wejściem zadania testowego, więc zmiana włączenia testu live
unieważnia wynik zapisany w cache Gradle. Test używa konta root bez hasła zgodnie
z istniejącym kontraktem i wymaga odizolowanej, pustej instancji.

Raporty HTML: `punisherx-paper/build/reports/tests/test/index.html`,
`punisherx-spigot/build/reports/tests/test/index.html` i
`punisherx-velocity-bridge/build/reports/tests/test/index.html`.

## Granice potwierdzenia

- PostgreSQL i SQL Server: załadowano rzeczywiste klasy sterowników; SQL Server ma
  testy przekształcania zapytań i kolejności parametrów. Nie wykonano połączeń do
  rzeczywistych serwerów tych dwóch silników.
- Komendy korzystają z prawdziwej bazy, lecz nie z uruchomionego Minecrafta.
  Nie zastępuje to testu klienta, rejestracji komend na żywym serwerze, sieci
  proxy/backend ani zachowania schedulerów Folia w działającym serwerze.
- Test równoległych zgłoszeń dotyczy jednej instancji handlera. Nie potwierdza
  globalnego egzekwowania limitu przez kilka serwerów jednocześnie.
- Izolowany loader testuje kompletność lokalnego runtime. Nie symuluje pierwszego
  pobrania z pustym cache, awarii repozytorium ani pełnego startu Bukkit/Paper.
- BungeeCord ma kontrolę kompilacji i pakowania; nie dodano testu uruchomienia
  rzeczywistego proxy. Istniejący scenariusz runtime Velocity pozostaje oddzielny
  w `punisherx-velocity-bridge/src/runtimeTest`.

## Sprzątanie środowiska testowego

Instancja MariaDB użyta w udanym teście została zamknięta przez `mariadb-admin`.
Wcześniejsza próba inicjalizacji w sandboxie pozostawiła zawieszony proces
bootstrapu `mariadbd`, PID `44011`, pracujący wyłącznie na
`/tmp/punisherx-syntaxdb-mariadb-4Nn9gw/data`. Ograniczenia izolacji odmówiły
jego zakończenia, również po wejściu do jego przestrzeni użytkownika.
Można zakończyć go ze zwykłego terminala użytkownika poleceniem `kill -9 44011`,
po uprzednim sprawdzeniu, że PID nadal wskazuje ten proces bootstrapu.
Istniejąca systemowa usługa MariaDB nie była modyfikowana.
