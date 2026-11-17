# Dziennik zmian

## [1.9.0] — Jeszcze niewydane

### Zmiany funkcjonalne:

* **Dodano `/unpunish <gracz> <szablon> [--silent]` na Paper i Spigot.** Komenda cofa ostatnią karę oraz jej wpływ na poziom eskalacji, zachowując wpis historii umożliwiający sprawdzenie wykonanej operacji. Działa również dla kar wygasłych lub wcześniej zdjętych, a ban obejmujący wiele adresów IP jest traktowany jako jeden etap eskalacji.
* **Dodano `--silent` do komend nakładania i zdejmowania kar**, w tym `kick all`, `/punish`, `/unpunish` i `/clearall`. Wiadomości na czacie dla ukaranego oraz ogłoszenia dla administracji są wyciszane, natomiast wykonujący otrzymuje prywatne potwierdzenie z informacją o trybie cichym. Komunikaty błędów, ekrany rozłączenia, egzekwowanie kar, historia i logi pozostają aktywne.
* Dodano niezależne flagi `--force` i `--silent` do `/punish` dla wszystkich typów kar w szablonach. Obie flagi można podawać przed opcjonalnym poziomem lub po nim. Kolejne komendy kar PunisherX uruchamiane przez skonfigurowane akcje dziedziczą tryb cichy.
* Dodano opcjonalne placeholdery nazw wyświetlanych, prefiksów i sufiksów graczy oraz wykonujących komendy, pobierane z LuckPerms na Paper i Spigot. Umożliwiono konfigurację klucza metadanych koloru nicku oraz niezależnych układów nazw wyświetlanych.
* Dodano konfigurowalne interfejsy książkowe do składania i obsługi zgłoszeń, obejmujące stronicowane menu, potwierdzenia, historię oraz dotychczasowe komendy podejmowania decyzji. Dotychczasowe interfejsy GUI i okna dialogowe nadal są dostępne.

### Konfiguracja i bezpieczeństwo plików językowych:

* Dodano osobne, edytowalne komunikaty `silent_success` dla komend nakładania i zdejmowania kar we wszystkich dostarczanych językach. Prywatne potwierdzenia trybu cichego zawierają odpowiednio cel, powód, czas trwania i identyfikator kary, jeśli dotyczą danej operacji.
* Dodano migrację konfiguracji do wersji 168 oraz polskie i angielskie komunikaty dla książkowych interfejsów zgłoszeń.
* Przy przejściu warstwy bazodanowej na SyntaxDB zachowano dotychczasowe ustawienia `database.type`, `database.sql.*`, nazwy tabel i ścieżki plików SQLite. Dodano domyślne porty baz danych, alias MariaDB oraz opcjonalne ustawienie `database.sql.jdbc-url`.
* **Usunięto obsługę H2 z nowej warstwy bazodanowej.** Instalacje korzystające z H2 muszą przed aktualizacją wyeksportować i przenieść dane do obsługiwanego silnika. Sama zmiana typu bazy nie przenosi danych.
* Nowe tabele MySQL/MariaDB korzystają z `utf8mb4`, a pluginy serwerowe używają kodowania UTF-8 dla połączenia. Kodowanie istniejących tabel pozostaje bez zmian i może wymagać osobnej konwersji, aby zapisywać powody kar zawierające znaki Unicode.

### Zmiany techniczne:

* **Zastąpiono dostęp do baz danych przez SyntaxCore biblioteką SyntaxDB w modułach Paper, Spigot, Velocity i BungeeCord.** Zmiana obejmuje pule połączeń, zapytania, generowane identyfikatory, transakcje oraz inicjalizację schematu.
* Rozszerzono eksport bazy o wszystkie dziesięć tabel PunisherX, w tym zgłoszenia, decyzje, cache graczy, zdarzenia mostu i wpisy migracji tożsamości. Pliki kopii zapasowych są podmieniane atomowo. Import sprawdza obsługiwane instrukcje INSERT i odtwarza rekordy w jednej transakcji, bez wykonywania dowolnych poleceń SQL z pliku. Import dopisuje dane bez czyszczenia bazy i nadpisywania rekordów; ponowny import tej samej kopii może zakończyć się konfliktem klucza i wycofaniem całej transakcji. Obsługiwane są kopie PunisherX, a nie dowolne pliki mysqldump lub pg_dump.
* Przeniesiono `/prx export` i `/prx import` poza wątek serwera na Paper/Folia i Spigot. Wykonujący otrzymuje komunikat rozpoczęcia oraz potwierdzenie sukcesu albo obsłużony błąd. Kolejna komenda eksportu/importu jest odrzucana, gdy poprzednia jeszcze trwa. Dodano testy komend oraz odtworzenia kopii SQLite zawierającej 100 tys. rekordów. Eksport/import nadal przetwarzają dane w pamięci; rzeczywiste operacje na PostgreSQL i SQL Server wymagają osobnych testów przed stabilnym wydaniem.
* Usprawniono migrację między silnikami baz danych: dodano sprawdzanie źródła, kopię zapasową przenoszonych danych, odmowę nadpisania niepustych tabel docelowych, kontrolę liczby rekordów oraz przywracanie generowania identyfikatorów. Konfiguracja jest przełączana dopiero po udanym transferze. Migracja wymaga okna serwisowego.
* Paper i Spigot pobierają teraz wymagane biblioteki SyntaxDB oraz sterowniki JDBC przez swoje mechanizmy ładowania bibliotek, zamiast dołączać je do serwerowych plików JAR. Mosty proxy nadal zawierają zależności bazodanowe. Kontrole podczas budowania sprawdzają poprawność obu sposobów pakowania.
* Przeniesiono parsowanie nazw, serializację komponentów i obsługę dosłownych placeholderów książek do MessageHandler R0.3. Usunięto osobny mechanizm formatowania z PunisherX.
* Dodano wielokrotnego użytku BookManager z jednorazowymi, wygasającymi akcjami menu przypisanymi do konkretnego gracza.
* Historia kar szablonowych identyfikuje teraz szablon niezależnie od powodu kary. Wyrzucenia graczy wynikające z drabinek szablonów zawsze trafiają do historii. Starsze wpisy zachowują dopasowywanie na podstawie powodu.
* Rozszerzono testy automatyczne zgodności baz danych, eksportu i importu, działania komend, wycofywania transakcji, zależności uruchomieniowych, flag szablonów, prywatnych potwierdzeń trybu cichego oraz operacji bazodanowych Velocity. Dodano środowisko testów uruchomieniowych Velocity i kontrole wersji źródeł MessageHandler w CI.

### Poprawki błędów:

* **Naprawiono podpowiedzi `/punish` na Paper i Spigot.** Nazwy szablonów są dopasowywane do wpisanego początku bez rozróżniania wielkości liter. Nazwy wymagające cudzysłowów są poprawnie zapisywane, a podstawowe ścieżki obsługi komendy podpowiadają poziomy z konfiguracji oraz nieużyte flagi.
* **Naprawiono tworzenie stałych banów IP mechanizmem vanilla po nieudanym zapisie `/banip`.** Zarówno bezpośrednie bany IP, jak i bany z szablonów zapisują historię i publikują zdarzenia mostu wyłącznie dla adresów skutecznie zapisanych w bazie. Częściowe błędy wskazują zapisane i niezapisane adresy IP, bez ogólnego potwierdzenia sukcesu i uruchamiania kolejnej akcji.
* Naprawiono usuwanie nowszej, aktywnej kary tego samego typu podczas czyszczenia kar wygasłych. Usuwany jest teraz konkretny wygasły rekord wskazany identyfikatorem.
* Zapewniono jednoznaczną kolejność stronicowania kar i historii przy jednakowych znacznikach czasu. Poprawiono sprawdzanie liczby zmienionych wierszy oraz uwzględnianie zakresu serwera przy usuwaniu kar i zmianie powodów w historii.
* Usuwanie danych gracza oraz zgłoszeń wraz z decyzjami odbywa się teraz transakcyjnie, aby zapobiegać częściowemu usunięciu danych i pozostawianiu osieroconych rekordów.
* Usprawniono raportowanie błędów uruchamiania bazy oraz zwalnianie zasobów. Nieudana inicjalizacja schematu nie pozostawia bazy oznaczonej jako gotowa, a mechanizmy sprawdzania kar pomijają niedostępną bazę.
* Usprawniono usuwanie rejestracji listenerów, usług i cyklicznych zadań obsługi kar podczas przeładowania. Zapewniono korzystanie z aktywnej instancji obsługi bazy po ponownej inicjalizacji.
* Naprawiono egzekwowanie banów przy logowaniu do Velocity 4: logowanie jest odrzucane przez wynik zdarzenia, a bany są sprawdzane asynchronicznie przed połączeniem z serwerem docelowym. Metadane pluginu proxy korzystają teraz z deskryptora uzupełnianego przez Gradle.
* Naprawiono konwersję komponentów Spigot przy użyciu Adventure 5, zachowując tekst, kolory, podpowiedzi po najechaniu oraz klikalne komendy.
* Poprawiono formatowanie komunikatów rozłączenia przy wyrzuceniu i banie w dostarczanych tłumaczeniach.
