Plugin oferuje rozbudowany zestaw permisji do zarządzania dostępem do różnych funkcji. Poniżej znajdziesz pogrupowaną listę dostępnych permisji wraz z opisami.
Rekomendowany plugin do uprawnień: [LuckPerms](https://luckperms.net/)

### Permisje komend

| Permisja | Opis |
| --- | --- |
| `punisherx.cmd.ban` | Pozwala banować gracza, uniemożliwiając mu wejście na serwer. |
| `punisherx.cmd.banip` | Pozwala banować adres IP gracza, blokując dostęp z tego adresu. |
| `punisherx.cmd.unban` | Pozwala odbanować gracza lub adres IP. |
| `punisherx.cmd.jail` | Pozwala zamknąć gracza w więzieniu na określony czas i lokalizację. |
| `punisherx.cmd.unjail` | Pozwala zwolnić gracza z więzienia. |
| `punisherx.cmd.mute` | Pozwala zmutować gracza, blokując wysyłanie wiadomości. |
| `punisherx.cmd.unmute` | Pozwala odmutować gracza i przywrócić możliwość pisania. |
| `punisherx.cmd.warn` | Pozwala ostrzec gracza z podanym powodem. |
| `punisherx.cmd.unwarn` | Pozwala usunąć ostrzeżenie gracza. |
| `punisherx.cmd.kick` | Pozwala wyrzucić gracza z serwera z podanym powodem. |
| `punisherx.cmd.punish` | Pozwala używać szablonów przez `/punish` oraz szybkich kar z szablonów w GUI gracza. |
| `punisherx.cmd.change_reason` | Pozwala zmienić powód kary. |
| `punisherx.cmd.banlist` | Wyświetla listę wszystkich zbanowanych graczy. |
| `punisherx.cmd.check` | Sprawdza kary gracza. Nie jest wymagane, gdy gracz sprawdza samego siebie. |
| `punisherx.cmd.history` | Pozwala sprawdzić pełną historię kar wybranego gracza. Nie jest wymagane przy sprawdzaniu własnej historii. |
| `punisherx.view_ip` | Wyświetla IP graczy w komendach sprawdzających i GUI graczy. |
| `punisherx.cmd.clear_all` | Pozwala wyczyścić wszystkie aktywne kary wskazanego gracza. |
| `punisherx.cmd.prx` | Dostęp do administracji przez `/prx` / `/punisherx`, w tym walidacji, reloadu, diagnostyki i operacji na bazie; także `/langfix`. |
| `punisherx.cmd.panel` | Otwiera `/panel`. Dostęp mogą też zapewniać uprawnienia zarządzania; akcje panelu sprawdzają własne permisje. |
### Uprawnienia zarządzania

| Permisja | Opis |
| --- | --- |
| `punisherx.manage` | Dostęp do zarządzania, w tym edycji presetów czasu/powodu w GUI oraz zarządzania zgłoszeniami. |
| `punisherx.manage.set_jail` | Pozwala ustawić lokalizację więzienia. |
| `punisherx.manage.set_spawn` | Zapisuje miejsce zwolnienia przez `/setunjail` (permisja zachowuje historyczną nazwę). |

### Dostęp do zgłoszeń

| Permisja | Opis |
| --- | --- |
| `punisherx.see.reports` | Odczyt zgłoszeń i powiadomienia o nowych zgłoszeniach. Nie pozwala rozpatrywać ani odrzucać. |
| `punisherx.manage.reports` | Odczyt, rozpatrywanie i odrzucanie zgłoszeń przez `/reports` oraz GUI/dialog administratora. |

`/report` jest dostępne graczom bez osobnej permisji. Zarządzający mogą także czytać zgłoszenia. Nadrzędne uprawnienia `punisherx.see` / `punisherx.see.*` zapewniają odczyt, a `punisherx.manage` / `punisherx.manage.*` — zarządzanie zgłoszeniami. Powiadomienia korzystają ze sprawdzania uprawnień do podglądu.

### Wildcard

| Permisja | Opis |
| --- | --- |
| `punisherx.owner` | Pozwala używać wszystkich komend PunisherX. |
| `punisherx.cmd.*` | Daje dostęp do wszystkich komend PunisherX. |
| `punisherx.manage.*` | Daje dostęp do wszystkich komend zarządzania. |
| `punisherx.see.*` | Zapewnia podgląd powiadomień o karach oraz odczyt zgłoszeń. |
| `punisherx.bypass.*` | Zapobiega nakładaniu kar na użytkownika. |


### Permisje bypass

| Permisja | Opis |
| --- | --- |
| `punisherx.bypass` | Pozwala omijać wszystkie kary. |
| `punisherx.bypass.warn` | Pozwala omijać ostrzeżenia. |
| `punisherx.bypass.mute` | Pozwala omijać muty. |
| `punisherx.bypass.ban` | Pozwala omijać bany. |
| `punisherx.bypass.banip` | Pozwala omijać bany IP. |
| `punisherx.bypass.jail` | Pozwala omijać karę więzienia. |
| `punisherx.bypass.kick` | Pozwala omijać kicki. |

### Permisje do podglądu wiadomości

| Permisja | Opis |
| --- | --- |
| `punisherx.see` | Pozwala widzieć wszystkie kary. |
| `punisherx.see.ban` | Pozwala widzieć bany. |
| `punisherx.see.banip` | Pozwala widzieć bany IP. |
| `punisherx.see.unban` | Pozwala widzieć odbanowania. |
| `punisherx.see.jail` | Pozwala widzieć kary więzienia. |
| `punisherx.see.unjail` | Pozwala widzieć zwolnienia z więzienia. |
| `punisherx.see.mute` | Pozwala widzieć muty. |
| `punisherx.see.unmute` | Pozwala widzieć odmutowania. |
| `punisherx.see.warn` | Pozwala widzieć warny. |
| `punisherx.see.unwarn` | Pozwala widzieć usunięcia warnów. |
| `punisherx.see.kick` | Pozwala widzieć kicki. |
| `punisherx.see.update` | Pozwala widzieć powiadomienia o aktualizacjach. |

### Zgodność i różnice platform

Checker uprawnień dopuszcza konsolę i OP oraz `punisherx.owner`, `punisherx.*` i globalne `*`. Starsze węzły nadal działają w komendach korzystających z checkera zgodności; w nowych konfiguracjach używaj aktualnych nazw z tabel.

Spigot przed uruchomieniem komendy dodatkowo sprawdza permisję zadeklarowaną w `plugin.yml`. Obecny manifest używa `punisherx.cmd.setjail` dla `/setjail`, `punisherx.cmd.clearall` dla `/clearall` i `punisherx.check` dla `/check`. Przy nadawaniu pojedynczych komend na Spigot dodaj te węzły rejestracji obok, odpowiednio, `punisherx.manage.set_jail`, `punisherx.cmd.clear_all` i `punisherx.cmd.check`. Logika `/check` i `/history` pozwala sprawdzać własne dane, ale bramka uprawnień Spigot może nadal wymagać dostępu do samej komendy.

Edycja presetów GUI używa `punisherx.manage` / `punisherx.manage.*`; nie ma osobnej permisji edycji presetów. Szybkie kary z szablonów używają `punisherx.cmd.punish`. `/cache` jest dostępne tylko z konsoli — permisja nie udostępni go graczom.
