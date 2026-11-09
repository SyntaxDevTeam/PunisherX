# Weryfikacja komend eksportu i importu — 2026-10-26

`/prx export` i `/prx import` w modułach Paper/Folia i Spigot korzystają teraz
z adaptera schedulera. Odczyt bazy, odczyt/zapis pliku i transakcja importu
wykonują się poza wątkiem komendy. Wykonujący otrzymuje komunikaty rozpoczęcia,
sukcesu lub błędu. Wynik jest wysyłany przez scheduler gracza albo serwera.

Jedna instancja komendy obsługuje jedną operację kopii naraz. Druga komenda
importu/eksportu otrzymuje komunikat o trwającej pracy, bez kolejkowania kolejnej
kopii w pamięci. Blokada jest zwalniana po sukcesie, błędzie danych/pliku oraz
odrzuceniu zadania przez scheduler. Nie jest to blokada zapisów innych komend,
migracji silnika ani innych instancji pluginu.

Komunikaty są dostępne w zasobach językowych obu modułów: po polsku i angielsku,
a w pozostałych językach nowe wpisy mają na razie treść angielską.

## Testy komend

Wspólny `DatabaseBackupCommandContract` jest wykonywany przez rzeczywisty
`PunisherRootCommand` każdej platformy. Baza i plik SQLite są rzeczywiste;
Bukkit, odbiorcy i scheduler są mockowane. Testy osobno opróżniają kolejkę pracy
na dodatkowym wątku oraz kolejkę odpowiedzi.

Dziesięć scenariuszy na platformę obejmuje:

- odłożone wykonanie eksportu i wysłanie sukcesu dopiero przez scheduler;
- odmowę drugiej operacji, także dla innego wykonującego;
- błąd zapisu pliku oraz skuteczną ponowną próbę;
- import dopisujący rekordy przy zachowaniu istniejących danych;
- konflikt klucza po wcześniejszym insercie i rzeczywisty rollback;
- brak pliku, błędną składnię i ponowną próbę po błędzie;
- odrzucenie zadania asynchronicznego przez scheduler;
- kierowanie odpowiedzi dla gracza przez scheduler gracza;
- pominięcie późnej odpowiedzi po wyłączeniu pluginu;
- brak uprawnień do importu i eksportu.

## Większa kopia SQLite

Dodany test `SyntaxDbBackupContractTest` zapisuje 100 000 rekordów w tabeli
`playercache`. Każda wartość zawiera numer rekordu, Unicode, apostrof, średnik,
nową linię i dodatkowe 128 znaków. Test eksportuje bazę przez handler, usuwa dane
z tabeli, importuje kopię i porównuje ID oraz całą treść każdego rekordu przez
niezależne połączenie JDBC. Kopia ma 22 977 790 bajtów (około 23 MB).

| Moduł | Eksport | Import |
| --- | ---: | ---: |
| Paper | 547 ms | 1383 ms |
| Spigot | 304 ms | 900 ms |

To pojedyncze pomiary z lokalnego zestawu testów, bez uruchomionego serwera
Minecraft. Nie są gwarancją wydajności produkcyjnej. Test większej kopii dotyczy
jednej tabeli; dotychczasowe testy odtwarzania sprawdzają komplet dziesięciu tabel.
Nie mierzono szczytowego zużycia pamięci.

## Pełny zestaw testów

```bash
./gradlew -Ppunisherx.build.modules=paper,spigot \
  :punisherx-paper:test :punisherx-spigot:test --offline
```

| Moduł | Testy | Błędy | Pominięte |
| --- | ---: | ---: | ---: |
| Paper | 210 | 0 | 1 |
| Spigot | 115 | 0 | 0 |

Pominięty test Paper wymaga `PUNISHERX_TEST_MYSQL_URL` wskazującego pustą,
jednorazową bazę MySQL/MariaDB. W tym przebiegu nie uruchamiano bazy zewnętrznej.

## Pozostałe ograniczenia przed stabilnym wydaniem

- Eksport nadal tworzy pełny snapshot, a import wczytuje cały plik i rekordy do
  pamięci. Przeniesienie do zadania asynchronicznego nie ogranicza tego kosztu.
- PostgreSQL i SQL Server wymagają testu rzeczywistego eksportu/importu.
- Potrzebny jest test komend i schedulerów na uruchomionym Paper/Folia i Spigot.
- Import dopisuje dane. Ponowny import może powodować konflikt i rollback;
  nie czyści bazy i nie nadpisuje istniejących rekordów.
- Parser obsługuje kopie PunisherX, a nie dowolne pliki mysqldump lub pg_dump.
