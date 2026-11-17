Ten dokument opisuje wszystkie placeholdery wspierane przez `PlaceholderHandler`, ich działanie, wymagania i przykłady użycia. Identyfikator ekspansji to `prx`, więc każdy placeholder powinien mieć format `%prx_<nazwa>%`.

## Zasady ogólne
- Placeholdery używają ustawienia `placeholders.message_format` do wyboru formatu wiadomości stosowanego przez `MessageHandler` (config.yml).
- Większość placeholderów wymaga kontekstu gracza. Jeśli `Player` jest `null`, zwracany jest pusty string i placeholdery zależne od gracza nie są rozwiązywane (wyjątek: listy kar, bo można podać gracza docelowego w parametrze).
- Wszystkie placeholdery działają w przestrzeni nazw `prx` (expansion identifier).

## Listy kar
Placeholdery z tej grupy mogą być wywołane dla aktualnego gracza **albo** przez wskazanie innego gracza w nazwie placeholdera.

### Aktywne kary
- **Placeholdery:**
    - `%prx_active_punishments_list%` – lista aktywnych kar bieżącego gracza.
    - `%prx_active_punishments_list_<player>%` – lista aktywnych kar wskazanego gracza (np. `%prx_active_punishments_list_Notch%`).
- **Zakres:** pobiera do `placeholders.punishment_list_limit` wpisów (domyślnie 5; jeśli w configu jest ≤ 0, używana jest wartość z config.yml).
- **Format (plik językowy):**
    - Pusta lista zwraca wiadomość `punishment_list_empty` z `messages_xx.yml`.
    - Każdy wpis używa szablonu `punishment_list_entry`, gdzie podstawiane są `<type>`, `<reason>`, `<operator>`, `<start>` i `<end>`. Daty są formatowane jako `yyyy-MM-dd HH:mm:ss`; dla kar permanentnych `<end>` jest zastępowane wiadomością `punishment_list_permanent`.
    - Cała lista jest wstrzykiwana do wiadomości `active_punishments_list`, gdzie `<limit>` zastępuje limit, a `<list>` sformatowaną listę wierszy.

### Historia kar
- **Placeholdery:**
    - `%prx_punishment_history_list%` – historia kar bieżącego gracza.
    - `%prx_punishment_history_list_<player>%` – historia kar wskazanego gracza.
- **Zakres i format:** działa na tych samych zasadach co aktywne kary, z tym że dane pochodzą z historii i lista jest opakowana wiadomością `punishment_history_list` (`messages_xx.yml`).

## Pozostałe placeholdery tylko dla bieżącego gracza
Te placeholdery działają wyłącznie dla bieżącego gracza (nie można podać innego gracza w nazwie).

### Pozostały czas kar czasowych
- **Placeholdery:** `%prx_mute_remaining_time%`, `%prx_warn_remaining_time%`, `%prx_jail_remaining_time%`.
- **Działanie:**
    - Szukają aktywnej kary danego typu (`MUTE`, `WARN`, `JAIL`) z czasem końca w przyszłości.
    - Zwracają wiadomość dla odpowiedniego klucza (`mute_remaining_time`, `warn_remaining_time`, `jail_remaining_time`) połączoną z wynikiem `timeHandler.formatTime` dla liczby sekund pozostałych do końca kary.
    - Jeśli kara nie istnieje, wygasła lub pozostały czas jest ≤ 0, placeholder zwraca `null`, co daje pusty string w PlaceholderAPI.

### Globalne liczniki kar
- **Placeholdery:** `%prx_total_active_punishments%`, `%prx_total_punishments%`.
- **Działanie:**
    - Pierwszy zwraca liczbę wszystkich aktywnych kar (`countAllPunishments`) z prefiksem `total_active_punishments` z `messages_xx.yml`.
    - Drugi zwraca liczbę wszystkich kar w historii (`countAllPunishmentHistory`) z prefiksem `total_punishments`.
    - Jeśli licznik to 0, placeholder zwraca `null` (co daje pusty wynik).

## Wskazówki użycia
- Placeholdery list kar pozwalają budować własne menu/boardy bezpośrednio na bazie konfiguracji pliku językowego przez klucze: `active_punishments_list`, `punishment_history_list`, `punishment_list_entry`, `punishment_list_permanent`, `punishment_list_empty`.
- Aby uniknąć pustych wyników, upewnij się, że klucze wiadomości są uzupełnione i ustaw sensowne `placeholders.punishment_list_limit`.
- Placeholdery czasowe zwracają wartość tylko przy aktywnej karze; łącz je z warunkami lub formatowaniem PlaceholderAPI, aby ukrywać nieaktywne etykiety.

## Podsumowanie:
### Pełny zestaw dostępnych placeholderów:
* `%prx_warn_remaining_time%` - pozostały czas ostatniego `warn` gracza.
* `%prx_mute_remaining_time%` - pozostały czas ostatniego `mute` gracza.
* `%prx_jail_remaining_time%` - pozostały czas ostatniego `jail` gracza.
* `%prx_total_active_punishments%` - aktualna liczba wszystkich aktywnych kar na serwerze.
* `%prx_total_punishments%` - całkowita liczba wszystkich kar wystawionych kiedykolwiek na serwerze.
* `%prx_active_punishments_list%` – lista aktywnych kar bieżącego gracza.
* `%prx_active_punishments_list_<player>%` – lista aktywnych kar wskazanego gracza (np. `%prx_active_punishments_list_Notch%`).
* `%prx_punishment_history_list%` – historia kar bieżącego gracza.
* `%prx_punishment_history_list_<player>%` – historia kar wskazanego gracza.

### Konfiguracja
`config.yml`:
```YAML
placeholders:
  # Maximum number of punishments displayed by PlaceholderAPI list placeholders.
  punishment_list_limit: 5
  # Force MessageHandler to format placeholder results using the selected format.
  # Available options include: MINI_MESSAGE, LEGACY_AMPERSAND (recommended for most plugins), LEGACY_SECTION, PLAIN_TEXT.
  message_format: "LEGACY_AMPERSAND"
```
### Formatowanie:
`messages_en.yml`:
```YAML
placeholders:
  mute_remaining_time: "<gray>Muted until: <gold>"
  warn_remaining_time: "<gray>Warning valid until: <gold>"
  jail_remaining_time: "<gray>In jail until: <gold>"
  total_punishments: "<gray>Total punishments: <gold>"
  total_active_punishments: "<gray>Total active punishments: <gold>"
  active_punishments_list: "<gray>Active punishments (last <gold><limit></gold>):</gray><newline><list>"
  punishment_history_list: "<gray>Punishment history (last <gold><limit></gold>):</gray><newline><list>"
  punishment_list_entry: "<gray>- <gold><type></gold> <dark_gray>|</dark_gray> <gray><reason></gray> <dark_gray>|</dark_gray> <yellow><start></yellow> → <yellow><end></yellow> <dark_gray>|</dark_gray> <gray><operator></gray></gray>"
  punishment_list_empty: "<gray>No punishments found.</gray>"
  punishment_list_permanent: "undefined"
```

## Nazwy LuckPerms w wiadomościach

Paper i Spigot udostępniają dodatkowe placeholdery w komunikatach PunisherX, gdy wiadomość zawiera kontekst gracza lub operatora:

| Placeholder | Wartość |
| --- | --- |
| `<operator_display>` | Prefix, pokolorowany nick i suffix administratora wykonującego akcję |
| `<player_display>` | Prefix, pokolorowany nick i suffix gracza |
| `<operator_prefix>`, `<player_prefix>` | Sam prefix |
| `<operator_suffix>`, `<player_suffix>` | Sam suffix |

`<operator>` i `<player>` zachowują zwykłe nazwy. Odczyt odbywa się przez API LuckPerms; PlaceholderAPI nie jest wymagane. Wartości są pobierane dla osoby wskazanej w komunikacie, niezależnie od jego odbiorcy. Zmiana prefixu w LuckPerms wpływa na kolejne renderowane wiadomości.

Przykład w `messages_pl.yml`:

```yaml
mute:
  broadcast: "<player_display> <gray>został wyciszony przez</gray> <operator_display><gray>. Powód: <reason></gray>"
```

Prefix i suffix przyjmują formatowanie MiniMessage oraz legacy (`&`, `§`, `&#RRGGBB`, `§x…`). Spację po prefixie należy uwzględnić w jego wartości w LuckPerms. Bez osobnego koloru nick dziedziczy końcowy kolor prefixu.

Układ obu nazw można ustawić niezależnie w `config.yml`:

```yaml
name-formatting:
  operator-display: "$prefix $name $suffix"
  player-display: "$name <gray>($prefix)</gray>"
  name-color-meta: name-color
```

Tokeny: `$prefix`, `$name`, `$suffix`. `$player` i `$operator` są aliasami `$name` — zawsze oznaczają osobę, której nazwę formatuje dany szablon. Można zmieniać kolejność, pomijać elementy i dodawać tekst oraz formatowanie MiniMessage lub legacy. Kolor z metadanych jest stosowany w miejscu nicku. Tokeny wstawione z prefixu, suffixu lub nicku nie są ponownie rozwijane. `$$` oznacza dosłowny znak dolara.

Odstępy są zachowywane dokładnie. Domyślne `$prefix$name$suffix` zachowuje odstępy z LuckPerms. Jeśli dodasz spacje w szablonie, usuń zbędne spacje z wartości prefixu/suffixu, aby ich nie dublować. Bez danych LuckPerms prefix i suffix są puste, a szablon nadal jest stosowany. Migracja konfiguracji do wersji 168 dodaje domyślne szablony.

Opcjonalny klucz metadanych dla koloru nicku:

```yaml
name-formatting:
  name-color-meta: name-color
```

Przykładowe ustawienie w LuckPerms:

```text
/lp group admin meta set name-color "#ff5555"
```

Wartością może być też `red`, `&c` lub `<red>`. Sam LuckPerms nie określa jednego uniwersalnego klucza koloru nicku, dlatego jego nazwę można zmienić w konfiguracji.

Bez LuckPerms lub bez dostępnych danych użytkownika nowe placeholdery wyświetlają zwykły nick, a prefix i suffix są puste. Plugin używa wyłącznie już załadowanych danych LuckPerms: nie blokuje obsługi wiadomości oczekiwaniem na dane gracza offline. W komunikatach historii jest używany dostępny aktualny prefix, nie historyczny prefix z chwili nadania kary. Konsola zwykle nie ma danych LuckPerms i wyświetla swoją zwykłą nazwę.

Po wymianie JAR uruchom ponownie serwer. Sprawdź wiadomość po akcji wykonanej przez gracza z prefixem oraz z konsoli, kolor legacy i hex, brak LuckPerms i zmianę prefixu bez restartu.
