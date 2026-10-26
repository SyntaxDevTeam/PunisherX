# Plan testów akceptacyjnych PunisherX — Spigot

## 1. Uruchomienie i diagnostyka

### 1.1 Pierwsze uruchomienie

- [ ] Uruchom serwer od zera — nie używaj `/reload` Spigota.
- [ ] W konsoli znajdź informację o uruchomieniu PunisherX.
- [ ] Sprawdź, czy nie pojawiają się wyjątki, długie czerwone komunikaty ani słowa `Could not load`, `NoClassDefFoundError`, `ClassNotFoundException` lub `Failed to enable`.
- [ ] W konsoli wpisz `plugins`.

Oczekiwany wynik: `PunisherX` znajduje się na liście aktywnych pluginów i jest oznaczony jako włączony.

### 1.2 Podstawowe komendy

Jako Moderator wykonaj:

```text
/prx version
/prx diag
```

- [ ] `/prx version` pokazuje nazwę i wersję pluginu.
- [ ] `/prx diag` po krótkiej chwili pokazuje raport diagnostyczny.
- [ ] W raporcie baza danych ma wynik `OK`.
- [ ] Raport mówi, że serwerem jest Spigot, a nie Paper.
- [ ] W konsoli nie pojawia się wyjątek.

Brak połączenia z internetem w części „External connectivity” nie musi oznaczać błędu pluginu, jeżeli serwer celowo nie ma dostępu do sieci.

### 1.3 Przeładowanie konfiguracji

Jako Moderator wykonaj:

```text
/prx reload
```

- [ ] Pojawia się jeden komunikat o poprawnym przeładowaniu.
- [ ] Plugin nadal odpowiada na `/prx version`.
- [ ] Gracze pozostają na serwerze.
- [ ] Konsola nie pokazuje wyjątku.

---

## 2. MiniMessage i wygląd komunikatów na Spigot

Ta sekcja jest obowiązkowa. Sprawdza problem charakterystyczny dla wersji Spigot.

### 2.1 Pomoc i przyciski

Jako Moderator wykonaj:

```text
/prx help
```

- [ ] Ramka, tytuł i komendy mają różne kolory.
- [ ] W tekście nie widać surowych znaczników, np. `<gold>`, `<click:...>` lub `<hover:...>`.
- [ ] Na dole widoczne są przyciski `[Previous]` oraz `[Next]`.
- [ ] Kliknięcie `[Next]` otwiera następną stronę pomocy.
- [ ] Kliknięcie `[Previous]` wraca na poprzednią stronę.
- [ ] Każde kliknięcie wykonuje komendę tylko raz.

### 2.2 Klikalny adres strony

Jako Moderator wykonaj:

```text
/prx version
```

- [ ] Adres strony jest kolorowy i klikalny.
- [ ] Kliknięcie powoduje wyświetlenie przez Minecraft pytania o otwarcie strony.

### 2.3 Tekst po najechaniu

Ten test wykonaj po utworzeniu bana w sekcji 6.

```text
/banlist
```

- [ ] Najechanie kursorem na nick pokazuje UUID gracza.
- [ ] Najechanie na typ kary pokazuje ID, operatora, powód i inne szczegóły.
- [ ] W tekście nie widać znaczników MiniMessage.

---

## 3. Uprawnienia

### 3.1 Gracz bez uprawnień

Jako GraczTestowy spróbuj wykonać:

```text
/ban Moderator 10s test
/mute Moderator 10s test
/jail Moderator 10s test
/panel
```

- [ ] Żadna kara nie zostaje nałożona.
- [ ] Gracz otrzymuje informację o braku uprawnień albo Spigot blokuje komendę.
- [ ] Konsola nie wykonuje komendy w imieniu gracza.

### 3.2 Operator

Jako Moderator wykonaj:

```text
/check GraczTestowy all
```

- [ ] Komenda działa i pokazuje brak kar albo listę aktywnych kar.

### 3.3 Bypass

Test wymaga pluginu do zarządzania uprawnieniami, np. LuckPerms. Jeżeli nie jest zainstalowany, zaznacz `[-]`.

Nadaj GraczowiTestowemu `punisherx.bypass.ban`, a następnie jako Moderator wykonaj:

```text
/ban GraczTestowy 10m Test bypassu
```

- [ ] Ban zostaje odrzucony z informacją o bypassie.

Następnie wykonaj:

```text
/ban GraczTestowy 10m Test force --force
```

- [ ] Parametr `--force` omija bypass i ban zostaje nałożony.
- [ ] Po teście usuń uprawnienie bypass i wykonaj `/unban GraczTestowy`.

---

## 4. Podpowiadanie komend

Wpisuj komendy, ale zamiast Enter naciskaj Tab.

- [ ] `/ban Gra<Tab>` podpowiada nick GraczaTestowego.
- [ ] `/ban GraczTestowy <Tab>` podpowiada przykładowe czasy.
- [ ] `/mute GraczTestowy <Tab>` podpowiada przykładowe czasy.
- [ ] `/warn GraczTestowy 10m <Tab>` podpowiada powody.
- [ ] `/punish GraczTestowy <Tab>` podpowiada nazwy szablonów, np. `Spam` lub `Griefing`.
- [ ] `/prx <Tab>` pokazuje dostępne podkomendy.
- [ ] Podpowiedzi nie pojawiają się graczowi, który nie ma wymaganych uprawnień.

### 4.1 Aliasy komend

Wykonaj po jednej bezpiecznej próbie każdego aliasu:

```text
/cx GraczTestowy all
/hx GraczTestowy
/px
/prx version
```

- [ ] `/cx` działa tak samo jak `/check`.
- [ ] `/hx` działa tak samo jak `/history`.
- [ ] `/px` działa tak samo jak `/panel`.
- [ ] Podstawowa komenda `/prx` działa niezależnie od pełnej nazwy `/punisherx`.

### 4.2 Komendy z konsoli

W konsoli komendy wpisuje się bez początkowego `/`:

```text
check GraczTestowy all
warn GraczTestowy 10s Test konsoli
unwarn GraczTestowy
```

- [ ] Konsola może sprawdzić i nałożyć karę bez błędu uprawnień.
- [ ] Nazwa operatora w historii wskazuje konsolę.
- [ ] Gracz otrzymuje informację o karze nałożonej z konsoli.
- [ ] `unwarn` usuwa testowe ostrzeżenie.

---

## 5. Ban gracza

Przed testem GraczTestowy musi być online.

Jako Moderator wykonaj:

```text
/ban GraczTestowy 10m Test bana
```

- [ ] GraczTestowy zostaje rozłączony.
- [ ] Ekran rozłączenia zawiera powód `Test bana` i czas kary.
- [ ] Moderator otrzymuje **dokładnie jeden** komunikat potwierdzający bana.
- [ ] Moderator nie otrzymuje dodatkowej kopii broadcastu.
- [ ] Obserwator otrzymuje dokładnie jeden broadcast.
- [ ] Komunikaty mają poprawne kolory i nie pokazują znaczników MiniMessage.
- [ ] Ponowna próba wejścia GraczaTestowego zostaje zablokowana z czytelnym powodem.

Jako Moderator wykonaj:

```text
/check GraczTestowy ban
/history GraczTestowy
/banlist
```

- [ ] `/check` pokazuje aktywny ban, jego ID, powód i pozostały czas.
- [ ] `/history` zawiera utworzony ban.
- [ ] `/banlist` zawiera GraczaTestowego.
- [ ] Zapisz ID kary — będzie użyte w sekcji 11.

## 6. Odbanowanie gracza

Jako Moderator wykonaj:

```text
/unban GraczTestowy
```

- [ ] Pojawia się dokładnie jeden komunikat o odbanowaniu.
- [ ] Nie pojawia się jednocześnie komunikat „gracz nie ma kary”.
- [ ] Obserwator otrzymuje jeden broadcast o odbanowaniu.
- [ ] GraczTestowy może ponownie wejść na serwer.
- [ ] `/check GraczTestowy ban` nie pokazuje aktywnego bana.
- [ ] Ban nadal znajduje się w historii.

Ponownie wykonaj:

```text
/unban GraczTestowy
```

- [ ] Pojawia się tylko jeden komunikat informujący, że gracz nie ma takiej kary.
- [ ] Nie pojawia się fałszywy komunikat o udanym odbanowaniu.

### 6.1 Ban gracza offline i ban stały

Po wyjściu GraczaTestowego z serwera wykonaj:

```text
/ban GraczTestowy Test stalego bana offline
```

- [ ] Plugin potrafi nałożyć karę na gracza offline.
- [ ] Brak czasu w komendzie jest pokazany jako kara stała lub bezterminowa.
- [ ] Próba wejścia GraczaTestowego zostaje zablokowana.
- [ ] `/banlist` i `/history GraczTestowy` pokazują karę.
- [ ] `/unban GraczTestowy` usuwa karę i umożliwia wejście.

---

## 7. Ban IP i odbanowanie po nicku

> Ten test wykonuj tylko wtedy, gdy GraczTestowy korzysta z innego IP niż Moderator. GraczTestowy musi wcześniej przynajmniej raz wejść na serwer.

Jako Moderator wykonaj:

```text
/banip GraczTestowy 10m Test IP
```

- [ ] GraczTestowy zostaje rozłączony.
- [ ] Moderator otrzymuje **dokładnie jeden** komunikat potwierdzający ban IP.
- [ ] Moderator nie otrzymuje drugiej kopii tego samego komunikatu.
- [ ] Obserwator otrzymuje jeden broadcast.
- [ ] GraczTestowy nie może ponownie wejść z tego samego IP.

Następnie wykonaj odbanowanie **po nicku**, nie po adresie IP:

```text
/unban GraczTestowy
```

- [ ] Pojawia się komunikat o poprawnym odbanowaniu.
- [ ] Nie pojawia się wcześniej komunikat „gracz nie ma kary”.
- [ ] GraczTestowy może ponownie wejść.

Opcjonalnie powtórz test, używając bezpośrednio testowego adresu:

```text
/banip ADRES_IP 10m Test IP
/unban ADRES_IP
```

- [ ] Obie komendy działają, a komunikaty zawierają tylko jeden wynik końcowy.

---

## 8. Wyciszenie

Jako Moderator wykonaj:

```text
/mute GraczTestowy 30s Test wyciszenia
```

- [ ] Moderator otrzymuje jeden komunikat potwierdzający.
- [ ] GraczTestowy otrzymuje informację o wyciszeniu.
- [ ] Obserwator otrzymuje jeden broadcast.
- [ ] Na ekranie GraczaTestowego pojawia się pasek z pozostałym czasem kary, jeśli jest włączony w konfiguracji.

Jako GraczTestowy napisz zwykłą wiadomość na czacie.

- [ ] Wiadomość nie trafia do innych graczy.
- [ ] GraczTestowy otrzymuje informację o aktywnym wyciszeniu.

Jako Moderator wykonaj:

```text
/unmute GraczTestowy
```

- [ ] GraczTestowy może ponownie pisać.
- [ ] Informacja o aktywnym wyciszeniu znika.

### 8.1 Automatyczne wygaśnięcie

```text
/mute GraczTestowy 10s Test wygasania
```

- [ ] Przez około 10 sekund gracz nie może pisać.
- [ ] Po upływie czasu może pisać bez ręcznego `/unmute`.
- [ ] Pasek kary znika po jej zakończeniu.

---

## 9. Ostrzeżenia

Jako Moderator wykonaj:

```text
/warn GraczTestowy 10m Test ostrzeżenia
```

- [ ] Moderator otrzymuje jeden komunikat potwierdzający.
- [ ] GraczTestowy otrzymuje informację o ostrzeżeniu.
- [ ] `/check GraczTestowy warn` pokazuje ostrzeżenie.
- [ ] `/history GraczTestowy` zawiera ostrzeżenie.

Następnie wykonaj:

```text
/unwarn GraczTestowy
```

- [ ] Ostatnie aktywne ostrzeżenie zostaje usunięte.
- [ ] Historia nadal zawiera informację o wcześniejszym ostrzeżeniu.

---

## 10. Zmiana powodu kary

Utwórz nową karę i odczytaj jej ID:

```text
/warn GraczTestowy 10m Stary powod
/check GraczTestowy warn
```

Następnie, podstawiając prawdziwe ID, wykonaj:

```text
/change-reason ID Nowy powod testowy
```

- [ ] Pojawia się komunikat o zmianie powodu.
- [ ] `/check GraczTestowy warn` pokazuje `Nowy powod testowy`.
- [ ] `/history GraczTestowy` pokazuje zaktualizowany powód.

Sprawdź błędne dane:

```text
/change-reason 999999 x
```

- [ ] Plugin wyświetla czytelny błąd i nie pokazuje wyjątku w konsoli.

Po teście wykonaj `/unwarn GraczTestowy`.

---

## 11. Więzienie

Ten test wymaga Moderatora obecnego w grze.

### 11.1 Ustawienie lokalizacji

Stań jako Moderator w bezpiecznym, zamkniętym miejscu i wykonaj:

```text
/setjail 5
```

Następnie stań w miejscu, do którego gracz ma wrócić po karze:

```text
/setunjail
```

- [ ] Obie komendy potwierdzają zapis lokalizacji.
- [ ] W konsoli nie pojawia się błąd świata lub teleportacji.

### 11.2 Egzekwowanie więzienia

```text
/jail GraczTestowy 1m Test wiezienia
```

- [ ] Gracz zostaje przeniesiony do więzienia.
- [ ] Jego tryb gry zmienia się na Adventure.
- [ ] Gracz otrzymuje informację o powodzie i czasie.
- [ ] Próba odejścia dalej niż ustawiony promień kończy się cofnięciem do więzienia.
- [ ] Próba napisania na czacie jest blokowana.
- [ ] Próba użycia niedozwolonej komendy jest blokowana.
- [ ] Pasek na ekranie pokazuje pozostały czas kary.
- [ ] `/check GraczTestowy jail` pokazuje aktywne więzienie.

Następnie wykonaj:

```text
/unjail GraczTestowy
```

- [ ] Gracz zostaje przeniesiony do punktu ustawionego przez `/setunjail`.
- [ ] Tryb gry wraca do Survival.
- [ ] Gracz może chodzić, pisać i wykonywać komendy.
- [ ] Kara znika z `/check`, ale pozostaje w `/history`.

---

## 12. Kick

Jako Moderator wykonaj:

```text
/kick GraczTestowy Test wyrzucenia
```

- [ ] GraczTestowy zostaje rozłączony.
- [ ] Ekran rozłączenia pokazuje `Test wyrzucenia`.
- [ ] Moderator otrzymuje jeden komunikat potwierdzający.
- [ ] Obserwator otrzymuje jeden broadcast.
- [ ] GraczTestowy może od razu wrócić na serwer.

Jeżeli w konfiguracji `kick.history` ma wartość `false`, kick nie musi być widoczny w historii.

---

## 13. Szablony `/punish`

Jako Moderator wykonaj:

```text
/punish GraczTestowy Spam 1
```

- [ ] Zostaje nałożone wyciszenie zgodne z pierwszym poziomem szablonu `Spam`.
- [ ] Powód pochodzi z pliku szablonów.
- [ ] Kara pojawia się w `/check` i `/history`.

Usuń karę:

```text
/unmute GraczTestowy
```

Sprawdź błędy:

```text
/punish GraczTestowy NieIstnieje 1
/punish GraczTestowy Spam abc
```

- [ ] Pierwsza komenda informuje o braku szablonu.
- [ ] Druga informuje o nieprawidłowym poziomie.
- [ ] Żadna nie powoduje wyjątku w konsoli.

---

## 14. GUI `/panel`

Jako Moderator wykonaj:

```text
/panel
```

- [ ] Otwiera się menu PunisherX.
- [ ] Tytuł, nazwy przedmiotów i opisy mają poprawne kolory.
- [ ] W GUI nie widać tekstów typu `<yellow>` albo `&c`.
- [ ] Przyciski listy graczy online i offline otwierają właściwe widoki.
- [ ] Można otworzyć menu GraczaTestowego.
- [ ] Działają widoki aktywnych kar, historii, listy zbanowanych i więźniów.
- [ ] Przycisk powrotu prowadzi do poprzedniego menu.
- [ ] Sortowanie listy offline zmienia kolejność graczy.
- [ ] Zamknięcie GUI nie wykonuje żadnej akcji.

### 14.1 Nakładanie kary przez GUI

- [ ] Wybierz GraczaTestowego.
- [ ] Wybierz karę `Warn` albo `Mute`.
- [ ] Wybierz czas i powód.
- [ ] Sprawdź, czy kara pojawia się w `/check`.
- [ ] Usuń ją odpowiednią komendą.

### 14.2 Czyszczenie danych przez GUI

> Ten test usuwa dane konta. Wykonuj go wyłącznie na jednorazowym koncie testowym i na końcu całej sesji.

- [ ] Kliknięcie usunięcia danych najpierw pokazuje ekran potwierdzenia.
- [ ] `Anuluj` wraca bez usuwania danych.
- [ ] Po ponownym wejściu i wybraniu `Potwierdź` dane testowego konta zostają wyczyszczone.
- [ ] Nie zostają usunięte dane innych graczy.

---

## 15. Zgłoszenia graczy

Jako GraczTestowy wykonaj:

```text
/report
```

- [ ] Otwiera się GUI wyboru gracza.
- [ ] GraczTestowy nie może wybrać samego siebie.
- [ ] Można wybrać Moderatora i powód zgłoszenia.
- [ ] Po zatwierdzeniu pojawia się potwierdzenie.
- [ ] Obserwator lub Moderator otrzymuje powiadomienie o zgłoszeniu.

Następnie spróbuj od razu wysłać kolejne zgłoszenie:

- [ ] Plugin blokuje duplikat i wyświetla czytelną informację.

Sprawdź bezpośredni wybór celu:

```text
/report Moderator
```

- [ ] Otwiera się wybór powodu dla Moderatora.

---

## 16. Czyszczenie wszystkich aktywnych kar

Nałóż dwie różne kary:

```text
/warn GraczTestowy 10m Test clearall
/mute GraczTestowy 10m Test clearall
/check GraczTestowy all
```

- [ ] `/check` pokazuje obie kary.

Następnie wykonaj:

```text
/clearall GraczTestowy
```

- [ ] Pojawia się jedno potwierdzenie.
- [ ] GraczTestowy otrzymuje informację o anulowaniu kar.
- [ ] `/check GraczTestowy all` nie pokazuje aktywnych kar.
- [ ] Gracz może pisać na czacie.
- [ ] Historia wcześniejszych kar pozostaje dostępna.

---

## 17. Restart i trwałość danych

Nałóż karę, której czas nie wygaśnie podczas restartu:

```text
/mute GraczTestowy 10m Test restartu
```

W konsoli wykonaj bezpieczne zatrzymanie:

```text
stop
```

Uruchom serwer ponownie.

- [ ] PunisherX uruchamia się bez błędów.
- [ ] `/check GraczTestowy mute` nadal pokazuje karę.
- [ ] GraczTestowy nadal nie może pisać.
- [ ] Pozostały czas jest mniejszy niż przed restartem.
- [ ] `/history GraczTestowy` nadal zawiera wcześniejsze testy.

Po teście wykonaj:

```text
/unmute GraczTestowy
```

Powtórz próbę restartu z aktywnym banem, jeżeli jest na to czas:

- [ ] Zbanowany gracz nadal nie może wejść po restarcie.
- [ ] Po `/unban GraczTestowy` może wejść ponownie.

---

## 18. Identyfikacja autora bez specjalnych uprawnień

Ten test wymaga wejścia na serwer z konta autora rozpoznawanego przez plugin. Jeżeli konto nie jest dostępne, zaznacz całą sekcję `[-]`.

- [ ] Po wejściu autor otrzymuje jedynie wiadomość informującą, że serwer używa PunisherX.
- [ ] Po `deop` konto autora nie może wykonywać `/ban`, `/kick`, `/jail`, `/punish` ani komend administracyjnych bez nadanych uprawnień.
- [ ] Konto autora nie widzi broadcastów administracyjnych bez odpowiedniego uprawnienia.
- [ ] Konto autora nie omija mute ani jail bez zwykłego uprawnienia bypass.
- [ ] Moderator może zbanować konto autora.
- [ ] Moderator może wyrzucić, wyciszyć i umieścić konto autora w więzieniu.
- [ ] Nie pojawia się komunikat typu „nie możesz ukarać autora pluginu”.

Po teście usuń wszystkie kary z konta autora.

---

## 19. Zachowanie przy błędnych komendach

Jako Moderator wykonaj kolejno:

```text
/ban
/ban GraczTestowy
/banip
/mute
/warn
/jail
/unjail
/unban
/check GraczTestowy zlytyp
/history NieIstniejacyGracz
/change-reason abc powod
/punish GraczTestowy NieIstnieje
```

- [ ] Każda komenda pokazuje krótką instrukcję użycia albo czytelny błąd.
- [ ] Żadna komenda nie powoduje rozłączenia innych graczy.
- [ ] Żadna komenda nie nakłada przypadkowej kary.
- [ ] W konsoli nie pojawia się wyjątek.

---

## 20. Testy opcjonalnych integracji

Wykonuj wyłącznie te sekcje, które administrator świadomie skonfigurował.

### 20.1 PlaceholderAPI

- [ ] Placeholdery pozostałego czasu mute, warn i jail pokazują prawdziwe wartości.
- [ ] Placeholder łącznej liczby kar zgadza się z `/history`.
- [ ] Brak aktywnej kary daje czytelną wartość, a nie błąd lub surową nazwę placeholdera.

### 20.2 Discord webhook

- [ ] Ban wysyła na wybrany kanał Discord pojedynczy embed.
- [ ] Embed zawiera gracza, moderatora, rodzaj, powód, czas i ID.
- [ ] Pozostałe rodzaje kar również wysyłają poprawny embed.
- [ ] Nie są wysyłane dwa identyczne webhooki.

### 20.3 BungeeCord lub Velocity

- [ ] Kara nałożona na jednym serwerze jest respektowana na drugim zgodnie z ustawieniem `server`.
- [ ] Ban gracza online na innym serwerze sieci powoduje jego rozłączenie.
- [ ] Odbanowanie jest widoczne na pozostałych serwerach bez ręcznego restartu.

### 20.4 Zewnętrzna baza danych

- [ ] `/prx diag` pokazuje połączenie z wybraną bazą jako `OK`.
- [ ] Kara utworzona przed restartem nadal istnieje po restarcie.
- [ ] Utrata połączenia daje czytelny błąd i nie wyłącza całego serwera.

### 20.5 Eksport i import danych

> Wykonuj tylko na świeżym serwerze testowym i po utworzeniu kopii folderu PunisherX.

- [ ] Nałóż jedno testowe ostrzeżenie i wykonaj `/prx export`.
- [ ] Konsola nie pokazuje błędu, a administrator potwierdza utworzenie eksportu.
- [ ] Po uzgodnieniu z administratorem wykonaj `/prx import` na osobnej kopii serwera.
- [ ] Zaimportowana kara jest widoczna w `/history`.
- [ ] Import nie tworzy niekontrolowanych duplikatów.

### 20.6 Inny język komunikatów

Jeśli wydanie ma oficjalnie wspierać więcej języków, administrator powinien kolejno ustawić wybrane języki i użyć `/prx reload`.

- [ ] Komunikaty podstawowych kar po przełączeniu języka pochodzą z wybranego pliku językowego.
- [ ] Polskie znaki oraz znaki innych alfabetów wyświetlają się prawidłowo.
- [ ] W żadnym języku nie widać surowych tagów MiniMessage ani nazw brakujących kluczy.

---

## 21. Końcowy przegląd konsoli

Po wykonaniu wszystkich testów przejrzyj konsolę od momentu uruchomienia serwera.

- [ ] Nie ma wyjątków ani pełnych śladów błędów związanych z PunisherX.
- [ ] Nie ma `NoClassDefFoundError`, zwłaszcza dla Adventure, MiniMessage lub BungeeComponentSerializer.
- [ ] Nie ma powtarzających się co sekundę ostrzeżeń.
- [ ] Plugin poprawnie zamyka bazę po komendzie `stop`.
- [ ] Po ponownym uruchomieniu nie ma informacji o uszkodzonej bazie lub konfiguracji.
