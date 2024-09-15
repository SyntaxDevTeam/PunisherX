# Welcome to PunisherX
This is a comprehensive punishment system that offers warnings, mutes, bans, IP bans (including temporary ones), and kicks. With the Check function, you can quickly check the current penalties imposed on a player. The plugin includes customizable templates for punishment reasons, making it easier to issue them quickly. Our PunisherX plugin offers a complete message file that allows modification and translation of all messages with full support for Minimessage. Additionally, the plugin has a detailed configuration file with many useful settings. The plugin is optimized for Paper servers and their forks, such as Pufferfish and Purpur. It supports both MySQL/MariaDB for synchronization between multiple servers and a local SQLite database for high performance.

## Information
* [x] PunisherX is designed specifically for the latest version of Minecraft from 1.20.6 to 1.21+
* [x] Written and optimized for the Paper engine, and thus also for its modifications such as Pufferfish and Purpur* 
* [x] Requires Java 21 or newer to work properly
* [x] Written in the modern programming language Kotlin, which is more expressive and safer than Java.
* [x] Language file with full support for Minimessage and Legacy (Minecraft) formatting
* [x] Extensive configuration file for flexible settings. Check the default settings here
* [x] Update notification system with the option to set automatic plugin updates.
* [x] Geolocation for administration insight. However, you need to set the appropriate license key in the config.

If you have any questions, you might find the solution on our [Discord](https://discord.gg/Zk6mxv7eMh)

## Download
* Always up-to-date stable version available for download on Hangar [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* You can also build the development version yourself from [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX)

## Installation
* Download the latest version of the plugin from the releases section.
* Place the JAR file in the plugins folder on your server.
* Start the Minecraft server.
* Configuration: In the config.yml file, you will find a rich set of configuration options.

## Commands

* `/ban <player> (time) <reason>` - Command to ban a player. Immediately bans and kicks the player if they are online. The command is additionally secured against database connection loss by using the built-in Paper engine - BanList.
* `/banip <ip|player> (time) <reason>` - Command to ban a player’s IP. Immediately bans and kicks the player if they are online, based on their IP or nickname. The command is additionally secured against database connection loss by using the built-in Paper engine command “/ban-ip”.
* `/unban <ip|player>` - Cancels the ban for a given player depending on the usage by their nickname or IP.
* `/mute <player> (time) <reason>` - Command to mute a player. The player cannot write anything publicly in the chat. In the config file, you can also set whether attempts to communicate in private messages should be blocked and the set of blocked commands for those messages.
* `/unmute <player>` - Cancels the mute for a player.
* `/warn <player> (time) <reason>` - Command to give a warning to a player. You can warn a player by giving them a warning for minor offenses. In the config file, you can set the reaction that should occur after a certain number of warnings, e.g., whether to kick or ban for a certain time. You can set any command available on your server, omitting the “/” sign.
```YAML
WarnActions:
3: "kick {player} You have received your 3rd warning!"
4: "ban {player} 30m You have received your 4th warning!"
5: "ban {player} 5h You have received your 5th warning!"
6: "ban {player} 7d You have received your 6th warning!"
7: "ban {player} 30d You have received your {warn_no}th warning!"
10: "ban {player} You have received your 10th warning!"
```
* `/unwarn <player>` - Cancels the last warning each time, which is in the database
* `/kick <player> <reason>` - Command that immediately kicks the player from the server with a specified reason.
* `/change-reason <penalty_id> <new_reason>` - Changes the reason for the penalty with the given ID
* `/check <player> <warn|mute|ban|all>` - Checks the active penalties of a given player. Available options to check only for warn|mute|ban or all types through “all”
* `/history <player> (X)` - Checks the entire penalty history of a given player from the beginning sorted from newest to oldest with page division. You can navigate in the game chat window by clicking the appropriate field [NEXT]|[Previous] or adding the page number as an argument to the command.
* `/punisherx|prx <help|version|reload|export|import>` - Default command displaying depending on the argument shows the help screen with available commands, the current version of the plugin with information about it, and reloading the config file. Additionally, the export|import arguments allow you to make a database backup or import it in the plugin directory.
> Arguments specified in <> are required, and those in round brackets () are optional. When using time (temporary punishment), you must specify in the available formats, i.e., Xs|Xm|Xh|Xd specifying X time in seconds|minutes|hours|days. Leaving the time field empty automatically assigns an indefinite punishment that never expires unless a command is used to cancel the punishment.

## Permissions
### Commands:
* `punisherx.warn`
* `punisherx.unwarn`
* `punisherx.mute`
* `punisherx.unmute`
* `punisherx.ban`
* `punisherx.banip`
* `punisherx.unban`
* `punisherx.kick`
* `punisherx.check` - Not required if the player checks themselves.
* `punisherx.view_ip` - Additional permission allowing to view the player’s IP in the /check|history command
* `punisherx.history` - Not required if the player checks themselves.
* `punisherx.help`
* `punisherx.version`
* `punisherx.reload`
* `punisherx.export`
* `punisherx.import`
### Visibility of chat messages:
* `punisherx.see.ban`
* `punisherx.see.banip`
* `punisherx.see.unban`
* `punisherx.see.mute`
* `punisherx.see.warns`
* `punisherx.see.kick`
### Bypass permissions (do not allow the command to be executed on authorized users):
* `punisherx.bypass.warn`
* `punisherx.bypass.mute`
* `punisherx.bypass.ban`
* `punisherx.bypass.banip` - does not work when using the IP number directly

## Contact
If you have any questions or need help, feel free to contact us on our [Discord](https://discord.gg/Zk6mxv7eMh) or write directly on PM

## License
This plugin is available under the MIT license. Details can be found in the LICENSE file.


**Thank you for using PunisherX! I hope it meets your expectations. 😊**

![syntaxdevteam_logo.png](assets/syntaxdevteam_logo.png)
---
<details>
<summary>Polska wersja README?</summary>

# Witaj w PunisherX

To wszechstronny system kar, który oferuje ostrzeżenia, wyciszenia, bany, ipbany (w tym tymczasowe) oraz kicki.
Dzięki funkcji Check możesz szybko sprawdzić aktualne kary nałożone na gracza. Wtyczka zawiera konfigurowalne szablony powodów kar, co ułatwia ich szybkie wymierzanie.
Nasz plugin PunisherX oferuje pełny plik wiadomości, który umożliwia modyfikację i tłumaczenie wszystkich komunikatów z pełnym wsparciem dla Minimessage. Dodatkowo wtyczka posiada szczegółowy plik konfiguracyjny z wieloma przydatnymi ustawieniami.
Wtyczka jest zoptymalizowana dla serwerów Paper i ich rozwidleń, takich jak Pufferfish czy Purpur. Obsługuje zarówno MySQL/MariaDB umożliwiając synchronizację między kilkoma serwerami, jak i lokalną bazę danych SQLite zapewniając wysoką wydajność.

## Informacje
* [x] PunisherX został zaprojektowany specjalnie pod najnowszą wersję Minecraft od 1.20.6 do 1.21+
* [x] Napisany i zoptymalizowano pod silnik Paper, a zatem tez pod jego modyfikacje tj. Pufferfish i Purpur
* [x] Wymaga Javy 21 lub nowszej, aby działać poprawnie
* [x] Napisany w nowoczesnym języku programowania Kotlin, który jest bardziej ekspresyjny i bezpieczny niż Java.
* [x] Plik językowy z pełnym wsparciem formatowania [Minimessage](https://docs.advntr.dev/minimessage/format.html) oraz Legacy (Minecraft)
* [x] Rozbudowany plik konfiguracyjny dla elastyczności ustawień. Sprawdź domyślne ustawienia [tutaj]()
* [x] System powiadomień o aktualizacjach z możliwością ustawienia automatycznego uaktualniania pluginu.
* [x] Geolokalizacja do wglądu dla administracji. Należy jednak ustawić w configu odpowiedni klucz licencji.

Jeśli masz jakieś pytania, być może znajdziesz na nie rozwiązanie na naszym [discordzie](https://discord.gg/Zk6mxv7eMh)

## Pobierz
* Zawsze aktualna wersja stabilna do pobrania na Hangar [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* Możesz także samodzielnie zbudować wersję developerską z [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX)

## Instalacja
* Pobierz najnowszą wersję pluginu z sekcji wydań.
* Umieść plik JAR w folderze plugins na swoim serwerze.
* Uruchom serwer Minecraft.
* Konfiguracja:
W pliku [config.yml](https://github.com/SyntaxDevTeam/PunisherX/blob/main/src/main/resources/config.yml) znajdziesz bogaty zasób opcji konfiguracyjnych.

## Komendy

* `/ban <gracz> (czas) <powód>` - Komenda banująca gracza. Natychmiast banuje i wyrzuca gracza, jeśli ten jest online. Komenda jest dodatkowo zabezpieczona na wypadek zerwania połączenia z bazą danych przez użycie wbudowanej w silniku Paper - BanList.
* `/banip <ip|gracz> (czas) <powód>` - Komenda banująca ip gracza. Natychmiast banuje i wyrzuca gracza, jeśli ten jest online, na podstawie jego IP lub jego nicku. Komenda jest dodatkowo zabezpieczona na wypadek zerwania połączenia z bazą danych przez użycie wbudowanej w silniku Paper komendy "/ban-ip".
* `/unban <ip|gracz>` - Kasuje bana dla danego gracza w zależności od sposobu użycia po jego nicku lub IP.
* `/mute <gracz> (czas) <powód>` - Komenda wyciszająca gracza. Gracz nie może publicznie na czacie nic napisać. W pliku config możesz ustawić także czy mają być blokowane próby porozumiewania się w wiadomościach prywatnych oraz zestaw blokowanych komend tych wiadomości.
* `/unmute <gracz>` - Kasuje wyciszenie dla gracza.
* `/warn <gracz> (czas) <powód>` - Komenda dająca ostrzeżenie dla gracza. Możesz upomnieć gracza poprzez nadanie mu ostrzeżenia za lżejsze przewinienia. W pliku config możesz ustawić rekację, jakie mają nastąpić po określonej ilości ostrzeżeń np. czy ma wyrzucić lub zbanować na dany czas. Można ustawić dowolną komendę dostępną na Twoim serwerze z pominięciem znaku "/".
```YAML
WarnActions:
3: "kick {player} You have received your 3rd warning!"
4: "ban {player} 30m You have received your 4th warning!"
5: "ban {player} 5h You have received your 5th warning!"
6: "ban {player} 7d You have received your 6th warning!"
7: "ban {player} 30d You have received your {warn_no}th warning!"
10: "ban {player} You have received your 10th warning!"
```
* `/unwarn <gracz>` - Kasuje każdorazowo ostatnie ostrzeżenie, jakie jest w bazie danych
* `/kick <gracz> <powód>` - Komenda, która natychmiastowo wyrzuca gracza z serwera z określonym powodem.
* `/change-reason <id_kary> <nowy_powód>` - Zmienia powód kary o danym ID
* `/check <gracz> <warn|mute|ban|all>` - Sprawdza aktywne kare danego gracza. Dostępne opcje do sprawdzenia tylko dla warn|mute|ban lub wszystkie rodzaje przez "all"
* `/history <gracz> (X)` - Sprawdza całą historię kar danego gracza od początku posortowane od najnowszej do najstarszej z podziałem na strony. Można nawigować w oknie czatu w grze klikając w odpowiednie pole [NEXT]|[Previous] lub dodając numer strony jako argument do komendy.
* `/punisherx|prx <help|version|reload|export|import>` - Domyślna komenda wyświetlająca w zależności od argumentu wyświetla ekran pomoc z dostępnymi komendami, aktualną wersję pluginu wraz z informacjami o nim oraz przeładowującą plik config. Dodatkowo argumenty export|import pozwalają na wykonanie zrzutu (kopii zapasowej) bazy danych lub jej importu w katalogu pluginu.
> Argumenty określone w <> są wymagane, a te w nawiasach okrągłych () są opcjonalne. Używając czasu (kara na czas określony) musisz określić w dostępnych formatach, czyli Xs|Xm|Xh|Xd określając X czasu w sekundach|minutach|godzinach|dniach. Pozostawienie pustego pola w miejscu czasu automatycznie nadaje kare na czas nieokreślony i nigdy ona nie wygaśnie, chyba że zostanie użyta komenda kasująca daną karę.

## Uprawnienia

### Komendy:
* `punisherx.warn`
* `punisherx.unwarn`
* `punisherx.mute`
* `punisherx.unmute`
* `punisherx.ban`
* `punisherx.banip`
* `punisherx.unban`
* `punisherx.kick`
* `punisherx.check` - Nie jest wymagane, jeśli gracz sprawdza samego siebie.
  * `punisherx.view_ip` - Dodatkowe uprawnienie pozwalające na wgląd w IP gracza w komendzie `/check|history`
* `punisherx.history` - Nie jest wymagane, jeśli gracz sprawdza samego siebie.
* `punisherx.help`
* `punisherx.version`
* `punisherx.reload`
* `punisherx.export`
* `punisherx.import`

### Widoczność komunikatów na czacie:
* `punisherx.see.ban`
* `punisherx.see.banip`
* `punisherx.see.unban`
* `punisherx.see.mute`
* `punisherx.see.warns`
* `punisherx.see.kick`

### Uprawnienia bypass (nie pozwalają na wykonanie na uprawnionych danej komendy):
* `punisherx.bypass.warn`
* `punisherx.bypass.mute`
* `punisherx.bypass.ban`
* `punisherx.bypass.banip` - nie działa w przypadku użycia bezpośrednio numeru IP

## Kontakt
Jeśli masz pytania lub potrzebujesz pomocy, śmiało skontaktuj się z nami na naszym [discordzie](https://discord.gg/Zk6mxv7eMh) lub napisz bezpośrednio na PM

## Licencja
Ten plugin jest dostępny na licencji MIT. Szczegóły znajdziesz w pliku LICENSE.

Dziękuję za korzystanie z PunisherX! Mam nadzieję, że spełni twoje oczekiwania. 😊

![syntaxdevteam_logo.png](assets/syntaxdevteam_logo.png)
</details>
