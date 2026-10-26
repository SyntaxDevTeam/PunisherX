# Migracja PunisherX 1.8.1-DEV do SyntaxDB

## Zakres

Paper/Folia, Spigot, Velocity Bridge i BungeeCord Bridge korzystają z
SyntaxDB `0.1.0-R0.1-SNAPSHOT`. Paper/Folia i Spigot używają `syntaxdb-core`
przez `compileOnly`: bibliotekę, HikariCP i sterowniki pobierają odpowiednio
`SyntaxLoader` i `LibraryLoader` według `paper-libraries.yml` oraz
`spigot-libraries.yml`. Wersje pochodzą z katalogu Gradle. Libby nie rozwiązuje
zależności przechodnich, dlatego lista jawnie zawiera też Protobuf dla MySQL.
Kotlin dla Spigot ładuje Bukkit przez `plugin.yml` przed utworzeniem klasy pluginu.
Pierwszy start wymaga dostępu do repozytoriów; pobrane biblioteki trafiają do cache.
Nie trzeba instalować osobnego pluginu SyntaxDB.

Mosty Velocity i BungeeCord nie mają tych loaderów i nadal dołączają pełną
dystrybucję `syntaxdb` do swoich JAR-ów. Ich rozmiar nie jest rozmiarem pluginu
Paper/Spigot. Test `verifyDatabaseBundle` sprawdza osobny kontrakt pakowania dla
modułów serwerowych i proxy.
SyntaxCore pozostaje zależnością pozostałych funkcji (np. logowania), ale jego
`DatabaseManager`, `DatabaseConfig`, `DatabaseType`, `TableSchema` i `Column` nie
są już używane przez PunisherX.

SyntaxDB zarządza pulą, zapytaniami, kluczami generowanymi, transakcjami i zamknięciem.
Migracje tożsamości korzystają ze scoped `withConnection` SyntaxDB: połączenie
nie opuszcza callbacka i nie jest zamykane przez kod PunisherX. Ten istniejący
moduł zachowuje własną transakcję JDBC; tworzenie tabel odbywa się przed pracą
transakcyjną, aby DDL MySQL nie zatwierdzało jej częściowo.

## Aktualizacja istniejącej instalacji

1. Wyłącz serwer i zachowaj kopię katalogu `plugins/PunisherX` oraz zewnętrznej bazy.
2. Wymień JAR serwera i używanych mostów proxy. Uruchom ponownie serwer.
3. Zachowaj istniejące `database.type` oraz `database.sql.*`.
4. Sprawdź `/prx diagnostics`, kary, historię, raporty i działanie mostu.

SQLite otwiera dokładnie dotychczasowy plik `plugins/PunisherX/<database.sql.dbname>`.
Nie dopisujemy rozszerzenia `.db` ani `.sqlite`. Tabele i rekordy są zachowywane;
brakująca kolumna `server` zostaje dodana z wartością `network`. Błąd inicjalizacji
schematu przerywa uruchamianie modułu zamiast oznaczyć niedziałającą bazę jako gotową.

Obsługiwane silniki:

| Konfiguracja | SyntaxDB / sterownik |
| --- | --- |
| `sqlite`, `sqlite3` | SQLite |
| `mysql` | MySQL Connector/J |
| `mariadb` | MySQL Connector/J, zgodny protokół MariaDB |
| `postgresql`, `postgres`, `pgsql` | PostgreSQL |
| `mssql`, `sqlserver`, `sql_server`, `sql-server` | Microsoft SQL Server |

H2 nie jest obsługiwane przez SyntaxDB. Instalacja z `database.type: h2` otrzyma
czytelny błąd przed otwarciem nowej bazy. Przed wymianą JAR-a wyeksportuj H2 przy
użyciu poprzedniej wersji i przenieś dane do obsługiwanego silnika. Stary eksport
PunisherX obejmował tylko kary, historię, cofnięcia i przypisania szablonów;
raporty, cache, kolejkę mostu i dziennik migracji tożsamości należy dodatkowo
przenieść narzędziem H2/JDBC. Samo przestawienie typu bazy nie przenosi danych.

Domyślne porty przy pustym/zerowym `database.sql.port`: MySQL/MariaDB 3306,
PostgreSQL 5432, SQL Server 1433. Jawnie ustawiony port jest zachowany.
Opcjonalne `database.sql.jdbc-url` pozwala skonfigurować np. szyfrowanie SQL Server.
Przy zmianie silnika usuń/zmień taki URL, jeżeli dotyczy poprzedniego silnika.
Mosty proxy zachowują dotychczasowy kontrakt MySQL i `bridge.properties`.
Nowe tabele MySQL/MariaDB otrzymują kodowanie utf8mb4. Aktualizacja nie zmienia
kodowania istniejących kolumn: jeżeli dotychczasowe tabele miały latin1, wykonaj
kontrolowaną konwersję po kopii zapasowej (zob. `docs/BANIP_DATABASE_ENCODING.md`).

## Eksport, import i zmiana silnika

Komendy `/prx export` i `/prx import` wykonują operacje bazodanowe i plikowe
asynchronicznie przez adapter schedulera Paper/Folia lub Spigot. Wykonujący
otrzymuje informację o rozpoczęciu i wyniku operacji; szczegóły błędów są w konsoli.
W czasie pracy kolejna komenda eksportu/importu jest odrzucana. Odpowiedź dla
gracza wraca przez jego scheduler, a dla konsoli przez scheduler serwera.

Nowy eksport `dump/backup.sql` obejmuje wszystkie dziesięć tabel: kary, historię,
cofnięcia, szablony, cache graczy, raporty, decyzje raportów, kolejkę mostu oraz
obie tabele dziennika migracji tożsamości. Zapis pliku jest atomowy; nieudany eksport
nie zastępuje poprzedniej kopii. Powody z apostrofami, znakami Unicode, ukośnikami,
średnikami i nowymi liniami są zachowywane.

Import akceptuje instrukcje INSERT wygenerowane przez obecny i poprzedni eksport
PunisherX. Parser nie wykonuje dowolnych poleceń SQL z pliku: waliduje nazwy tabel,
kolumn i wartości, a zapis wykonuje z parametrami w jednej transakcji. Import dodaje
rekordy; nie kasuje istniejących danych. Konflikt klucza wycofuje całą operację.
Nie jest to importer dowolnych dumpów mysqldump/pg_dump.

Asynchroniczne wykonanie nie ogranicza zużycia pamięci: eksport buduje snapshot,
a import odczytuje plik i gromadzi rekordy przed zapisem. Próba SQLite ze 100 tys.
rekordów i plikiem około 23 MB zakończyła się poprawnie w testach obu modułów.
Nie potwierdza to obsługi dowolnie dużej bazy ani pracy schedulerów na żywym
serwerze. PostgreSQL i SQL Server nadal wymagają osobnego testu rzeczywistego
eksportu/importu. Szczegóły: [weryfikacja komend kopii](BACKUP_COMMANDS_VERIFICATION.md).

Zmianę silnika wykonuj w oknie serwisowym:

1. Zatrzymaj dostęp graczy oraz zapisy ze wszystkich pozostałych serwerów i mostów.
   Bieżący serwer administracyjny musi pozostać uruchomiony do wykonania komendy.
   Wstrzymaj też zadania/pluginy mogące zapisywać dane w trakcie transferu.
2. Wykonaj niezależną kopię źródła. Uruchom `/prx migrate <źródło> <cel>`.
3. Ustaw w pliku `config.yml` parametry docelowego połączenia `database.sql.*`,
   pozostawiając aktywne źródło w pamięci serwera. Nie wykonuj reload w tym kroku.
4. Powtórz komendę. `--force` pomija wyłącznie potwierdzenie, nie zabezpieczenia danych.
5. Cel musi mieć puste tabele PunisherX. Nie usuwamy istniejących danych ani tabel.
   Transfer używa jednej transakcji odczytu źródła i jednej transakcji
   zapisu celu. Kontroluje liczby rekordów i przywraca generowanie ID PostgreSQL;
   SQL Server używa `IDENTITY_INSERT` dla przenoszonych ID.
6. Dopiero po udanym transferze aktualizujemy `database.type` na dysku i zlecamy
   reload. Sprawdź diagnostykę oraz dane przed wznowieniem ruchu.

Transfer nie blokuje zapisów z całej sieci. Okno serwisowe jest wymagane, aby
uniknąć zmian źródła po wykonaniu snapshotu. Błąd transferu nie przełącza konfiguracji
źródła. Jeśli błąd wystąpi już po zatwierdzeniu celu (np. przy zapisie konfiguracji),
cel może zawierać pełną kopię; należy go sprawdzić przed ponowną próbą, która odmówi
nadpisania niepustych tabel. Źródło pozostaje dostępne do odzyskania.

## Weryfikacja

`./gradlew buildAll` buduje cztery moduły i uruchamia testy Paper, Spigot oraz Velocity.
Wspólne testy komend i funkcji serwerowych są w `tests/server/kotlin` i wykonują się
oddzielnie na obu platformach. Izolowany classloader sprawdza kompletność runtime
bazodanowego, a kontrola JAR-ów odrzuca ponowne dołączenie pobieranych bibliotek.
Szczegółowy zakres i wyniki opisuje `docs/SYNTAXDB_POST_MIGRATION_AUDIT.md`.
Testy używają rzeczywistego SQLite do sprawdzenia zgodności starej bazy, ID,
raportów, transakcyjnego odtwarzania, cofania kar, cache i cyklu życia puli.
Test parametrów stronicowania sprawdza wariant SQL Server bez połączenia z serwerem.

Opcjonalny `MySqlMigrationContractTest` uruchamia test sterownika MySQL/MariaDB:

```bash
PUNISHERX_TEST_MYSQL_URL=jdbc:mysql://127.0.0.1:33367/punisherx_syntaxdb_test \
  ./gradlew :punisherx-paper:test --tests '*MySqlMigrationContractTest'
```

Używaj wyłącznie pustej, jednorazowej bazy testowej z użytkownikiem `root` bez hasła
na izolowanej instancji lokalnej. Test przenosi dane w obie strony między MySQL/MariaDB
oraz SQLite i czyści swoje dane. Bez zmiennej środowiskowej jest pomijany.
PostgreSQL oraz SQL Server wymagają dodatkowego sprawdzenia na rzeczywistych serwerach
przed wdrożeniem w środowisku korzystającym z tych silników.
