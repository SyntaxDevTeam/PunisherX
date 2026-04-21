Plugin oferuje rozbudowany zestaw permisji do zarządzania dostępem do różnych funkcji. Poniżej znajdziesz pogrupowaną listę dostępnych permisji wraz z opisami.


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
| `punisherx.cmd.change_reason` | Pozwala zmienić powód kary. |
| `punisherx.cmd.banlist` | Wyświetla listę wszystkich zbanowanych graczy. |
| `punisherx.cmd.check` | Sprawdza kary gracza. Nie jest wymagane, gdy gracz sprawdza samego siebie. |
| `punisherx.cmd.history` | Pozwala sprawdzić pełną historię kar wybranego gracza. Nie jest wymagane przy sprawdzaniu własnej historii. |
| `punisherx.view_ip` | Pozwala zobaczyć IP gracza w komendach `/check` i `/history`. |
| `punisherx.cmd.clear_all` | Pozwala wyczyścić wszystkie aktywne kary wskazanego gracza. |
| `punisherx.cmd.prx` | Pozwala używać `/punisherx` (alias `/prx`) i narzędzi jak `/langfix`, `/panel`, reload, export/import i migrate. |
| `punisherx.cmd.panel` | Zarezerwowane dla komendy GUI `/panel` (obecnie kontrolowane przez `punisherx.cmd.prx`). |
| --- | --- |
| `punisherx.manage` | Pozwala zarządzać pluginem (akcje na poziomie konfiguracji). |
| `punisherx.manage.set_jail` | Pozwala ustawić lokalizację więzienia. |
| `punisherx.manage.set_spawn` | Pozwala ustawić lokalizację odrodzenia po jailu. |

### Wildcard
| Permisja | Opis |
| --- | --- |
| `punisherx.owner` | Pozwala używać wszystkich komend PunisherX. |
| `punisherx.cmd.*` | Daje dostęp do wszystkich komend PunisherX. |
| `punisherx.manage.*` | Daje dostęp do wszystkich komend zarządzania. |
| `punisherx.see.*` | Pozwala widzieć wszystkie powiadomienia czatu o karach. |
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
