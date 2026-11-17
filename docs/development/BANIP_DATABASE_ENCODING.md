# Błędy kodowania przy `/banip`

`Incorrect string value` dla `reason`, np. `\xC5\x82` (polskie „ł”), oznacza,
że baza nie przyjmuje tekstu w użytym kodowaniu. Nie jest to komunikat o długości powodu.
Sprawdź kodowanie kolumn i połączenia MySQL/MariaDB.

## Zachowanie komendy po poprawce

- Nieudany zapis kary nie uruchamia bezterminowego `ban-ip` serwera.
- Historia i powiadomienie bridge są wykonywane tylko dla IP z zapisanym wpisem kary.
- Przy błędzie administrator otrzymuje listy IP zapisanych i niezapisanych.
  Komenda nie wysyła wtedy ogólnego komunikatu sukcesu ani nie wykonuje akcji `ip_banned`.
- Operacje dla wielu IP nie są jedną transakcją. Częściowy sukces oznacza, że
  poprawnie zapisane bany pozostają aktywne; pozostałe IP nie otrzymują nowego bana.
- Błędy samej historii lub kolejki bridge nadal są logowane oddzielnie;
  nie unieważniają poprawnie zapisanej kary.

Nowe kolumny `reason` w `punishments`, `punishmenthistory` i `bridge_events`
w MySQL/MariaDB mają jawne `CHARACTER SET utf8mb4`. Aktualizacja pluginu nie
zmienia kodowania istniejących kolumn ani konfiguracji połączenia w SyntaxCore.

## Sprawdzenie istniejącej bazy

Wybierz bazę używaną przez plugin i sprawdź rzeczywisty schemat:

```sql
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('punishments', 'punishmenthistory', 'bridge_events')
  AND COLUMN_NAME = 'reason';

SHOW CREATE TABLE punishments;
SHOW CREATE TABLE punishmenthistory;
SHOW CREATE TABLE bridge_events;
```

Administrator bazy powinien wykonać kopię zapasową i zaplanować konwersję
istniejących tabel do `utf8mb4`. Przykład dla standardowego schematu pluginu:

```sql
ALTER TABLE punishments CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE punishmenthistory CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE bridge_events CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Te polecenia zmieniają wszystkie kolumny tekstowe i mogą blokować tabele podczas
konwersji; należy najpierw sprawdzić własne indeksy i rozmiar tabel. Sama zmiana
domyślnego kodowania bazy nie konwertuje już istniejących kolumn.
Jeżeli kolumny są już poprawnie kodowane, sprawdź parametry połączenia JDBC
używanego przez SyntaxCore. Ustawienia sesji odczytane z osobnego klienta SQL
nie potwierdzają ustawień połączeń pluginu.

Po korekcie sprawdź zapis powodu z polskimi znakami i jego odczyt z historii.
Nowy komunikat `error.banip_db_error` jest w zasobach językowych; jeśli serwer
używa własnych kopii plików językowych, należy dodać ten klucz także do nich.

## Bany utworzone starym mechanizmem awaryjnym

Aktualizacja nie usuwa banów zapisanych wcześniej przez serwer do
`banned-ips.json`. Sprawdź je poleceniem `minecraft:banlist ips`.
Pluginowe `/unban` szuka wpisów w bazie, więc stare awaryjne bany należy
usunąć przez `minecraft:pardon-ip <adres>`, a następnie ponownie nałożyć
właściwą karę w PunisherX po naprawie bazy.
