# Welcome to PunisherX

To wszechstronny system kar, który oferuje ostrzeżenia, wyciszenia, bany, ipbany (w tym tymczasowe) oraz kicki.
Dzięki funkcji Check możesz szybko sprawdzić aktualne kary nałożone na gracza. Wtyczka zawiera konfigurowalne szablony powodów kar, co ułatwia ich szybkie wymierzanie.
PunisherX oferuje pełny plik wiadomości, który umożliwia modyfikację i tłumaczenie wszystkich komunikatów z pełnym wsparciem dla Minimessage. Dodatkowo, wtyczka posiada szczegółowy plik konfiguracyjny z wieloma przydatnymi ustawieniami.
Wtyczka jest zoptymalizowana dla serwerów Paper i ich rozwidleń, takich jak Pufferfish czy Purpur. Obsługuje zarówno MySQL/MariaDB umożliwiając synchornizację miedzy kilkoma serwerami, jak i lokalną bazę danych SQLite zapewniając wysoką wydajność.

## Informacje
* [x] PunisherX został zaprojektowany specjalnie pod najnowszą wersję Minecraft od 1.20.6 do 1.21+
* [x] Napisany i zoptymalizowano pod silnik Paper a zatem tez pod jego modyfikacje tj. Pufferfish i Purpur
* [x] Wymaga Javy 21 lub nowszej, aby działać poprawnie
* [x] Napisany w nowoczesnym języku programowania Kotlin, który jest bardziej ekspresyjny i bezpieczny niż Java.
* [x] Plik językowy z pełnym wspraciem formatowania [Minimessage](https://docs.advntr.dev/minimessage/format.html) oraz Legacy (Minecraft)
* [x] Rozbudowany plik konfiguracyjny dla elastyczności ustawień
* [x] System powiadomień o aktualizacjach z możliwością ustawienia autoamtycznego uaktualniania pluginu.


Jeśli masz jakieś pytania, być może znajdziesz na nie rozwiązanie na naszym [discordzie](https://discord.gg/KNstae3UEV)

## Pobierz
* Zawsze aktualna warsja stabilna do pobrania na Hangar [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* Możesz także samodzielnie zbudować wersję developerską z [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX)


## Instalacja
* Pobierz najnowszą wersję pluginu z sekcji wydań.
* Umieść plik JAR w folderze plugins na swoim serwerze.
* Uruchom serwer Minecraft.
* Konfiguracja
  W pliku config.yml znajdziesz opcje konfiguracyjne, takie jak listę zakazanych słów czy tryb pełnej cenzury.

## Komendy

Argumenty określone w <> są wymagane a te w nawiasach okrągłych () są opcjonalne. Używając czasu (kara na czas określony) musisz określić w dostepnych formatach czyli Xs|Xm|Xh|Xd określając X czasu w sekundach|minutach|godzinach|dniach. Pozostawienie pustego pola w miejscu czasu automatycznie nadaje kare na czas nieokreślony i nigdy ona nie wygaśnie chyba że zostanie użyta komenda kasująca daną karę.

* `/ban <gracz> (czas) <powód>` - Komenda banująca gracza. Natychmiast banuje i wyrzuca gracza jeśli ten jest online. Komenda jest dodatkowo zabezpieczona na wypadek zerwania połączenia z bazą danych przez użycie wbudowanej w silniku Paper - BanList.
* `/banip <ip|gracz> (czas) <powód>` - Komenda banująca ip gracza. Natychmiast banuje i wyrzuca gracza, jeśli ten jest online, na podstawie jego IP lub jego nicku. Komenda jest dodatkowo zabezpieczona na wypadek zerwania połączenia z bazą danych przez użycie wbudowanej w silniku Paper komendy "/ban-ip".
* `/unban <ip|gracz>` - Kasuje bana dla danego gracza w zależności od sposobu użycia po jego nicku lub IP.
* `/mute <gracz> (czas) <powód>` - Komenda wyciszająca gracza. Gracz nie może publicznie na czacie nic napisać. W pliku confgig możesz ustawić także czy mają być blokowane próby porozumiewania się w wiadomościach prywatnych oraz zestaw blokowanych komend tych wiadomości.
* `/unmute <gracz>` - Kasuje wyciszenie dla gracza.
* `/warn <gracz> (czas) <powód>` - Komenda dająca ostrzeżenie dla gracza. Możesz upomnieć gracza poprzez nadanie mu ostrzeżenia za lżejsze przewinienia. W pliku config możesz ustawić rekację jakie mają nastąpić po określonej ilości ostrzeżeń np. czy ma wyrzucić lub zbanować na dany czas. Można ustawić dowolną komendę dostępną na Twoim serwerze z pominięciem znaku "/".
 ```YAML
WarnActions:
  3: "kick {player} You have received your 3rd warning!"
  4: "ban {player} 30m You have received your 4th warning!"
  5: "ban {player} 5h You have received your 5th warning!"
  6: "ban {player} 7d You have received your 6th warning!"
  7: "ban {player} 30d You have received your {warn_no}th warning!"
  10: "ban {player} You have received your 10th warning!"
 ```
* `/unwarn <gracz>` - Kasuje kązdorazowo ostatnie ostrzeżenie jakie jest w bazie danych
* `/kick <gracz> <powód>` - Komenda która natychmiastowo wyrzuca gracza z serwera z określonym powodem.
* `/change-reason <id_kary> <nowy_powód>` - Zmienia powód kary o danym ID
* `/check <gracz> <warn|mute|ban|all>` - Sprawdza aktywne kare danego gracza. Dostępne opcje do sprawdzenia tylko dla warn|mute|ban lub wszystkie rodzaje przez "all"
* `/history <gracz> (X)` - Sprawdza całą historie kar danego gracza od początku posortowane od najnowsej do najstarszej z podziałem na strony. Można nawigować w oknie czatu w grze klikajac w odpowiednie pole [NEXT]|[Previous] lub dodając numer strony jako argument do komendy.
* `/punisherx|prx <help|version|reload>` - Domyślna komenda wyświetlająca w zależności od argumentu wyświetla ekran pomoc z dostępnymi komendami, aktualną wersję pluginu wraz z informacjami o nim oraz przeładowującą plik config.

## Uprawnienia


## Kontakt
Jeśli masz pytania lub potrzebujesz pomocy, śmiało skontaktuj się z nami na naszym [discordzie](https://discord.gg/KNstae3UEV) lub napisz bezpośrednio na PM

## Licencja
Ten plugin jest dostępny na licencji MIT. Szczegóły znajdziesz w pliku LICENSE.

Dziękuję za korzystanie z PunisherX! Mam nadzieję, że spełni twoje oczekiwania. 😊

![syntaxdevteam_logo.png](assets/syntaxdevteam_logo.png)
