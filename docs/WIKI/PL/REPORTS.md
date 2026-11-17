# Zgłoszenia graczy

[Strona główna](HOME.md) · [Komendy](COMMANDS.md) · [Uprawnienia](PERMISSIONS.md) · [English](../EN/REPORTS.md)

## Dla gracza: wysłanie zgłoszenia

`/report` jest dostępne dla graczy bez osobnego uprawnienia. Komenda nie działa z konsoli.

1. Wpisz `/report`, wybierz gracza i powód. Menu obejmuje graczy online oraz tych, którzy rozłączyli się w ostatniej godzinie.
2. Użyj `/report Steve`, aby przejść od razu do wyboru powodu, albo `/report Steve Spam na czacie`, aby bezpośrednio wysłać własny powód.
3. Wyślij zgłoszenie zgodnie z wybranym interfejsem; w książce zatwierdź je na ostatniej stronie. Komunikat sukcesu potwierdza zapis, a administracja online z dostępem do powiadomień otrzyma informację.

Powód musi mieć 3–255 znaków, bez znaków sterujących. Nie możesz zgłosić siebie ani mieć dwóch otwartych zgłoszeń na tego samego gracza. Domyślnie możesz mieć **3 otwarte zgłoszenia** łącznie; administrator może zmienić ten limit. Rozpatrzenie lub odrzucenie zwalnia miejsce i umożliwia ponowne zgłoszenie tego gracza. Zgłoszenie nie nakłada automatycznie kary.

Komendą można też wskazać znanego gracza offline; na Paper nick musi być dostępny online lub w pamięci serwera, można też podać UUID. Brak osoby w menu ostatnio widzianych nie musi oznaczać, że nie da się jej zgłosić komendą.

## Dla administracji: weryfikacja i decyzja

Nadaj `punisherx.see.reports` do odczytu i powiadomień o nowych zgłoszeniach. `punisherx.manage.reports` pozwala czytać i podejmować decyzje. Samo uprawnienie zarządzania nie zapewnia powiadomień: nadaj również uprawnienie widoczności. Zasady OP, właściciela i uprawnień nadrzędnych opisuje strona [Uprawnienia](PERMISSIONS.md).

1. Otwórz `/reports` i wybierz otwarte zgłoszenie. Sprawdź ID, autora, zgłoszonego, powód i datę.
2. Zweryfikuj dowody; jeśli masz odpowiednie uprawnienia, użyj `/check Steve all` i `/history Steve`.
3. Uzasadnioną karę nałóż osobno, np. skonfigurowanym szablonem `/punish`.
4. Wpisz `/reports resolve 12 Potwierdzone po sprawdzeniu logów czatu` albo `/reports reject 12 Brak wystarczających dowodów`.
5. Wróć do decyzji przez `/reports history` lub `/reports view 12`.

Uzasadnienie decyzji musi mieć 3–255 znaków, bez znaków sterujących. Zamknięcie zapisuje status (`RESOLVED` — rozpatrzone, `REJECTED` — odrzucone), nazwę administratora, uzasadnienie i czas. Ponowne rozpatrzenie lub odrzucenie nie nadpisze zamkniętej decyzji. Zamknięcie zgłoszenia **nie nakłada kary**. Zgłoszenia są wspólne dla serwerów korzystających z tej samej bazy, niezależnie od zakresu serwerowego kar.

| Komenda | Działanie |
| --- | --- |
| `/reports` | Skonfigurowany interfejs administracji; w konsoli lista otwartych zgłoszeń. |
| `/reports gui (strona)` | Przeglądarka zgłoszeń uwzględniająca wybór książki lub dialogu. |
| `/reports book (strona)` | Książkowa skrzynka niezależnie od konfiguracji. |
| `/reports list (strona)` | Otwarte zgłoszenia; w grze może używać książki lub dialogu Paper. |
| `/reports view <id>` | Szczegóły zgłoszenia i jego decyzji. |
| `/reports history (strona)` | Rozpatrzone i odrzucone zgłoszenia. |
| `/reports resolve <id> <uzasadnienie>` | Zamknięcie jako rozpatrzone; wymaga zarządzania. |
| `/reports reject <id> <uzasadnienie>` | Odrzucenie otwartego zgłoszenia; wymaga zarządzania. |

Numeracja stron zaczyna się od `1`. Konsola obsługuje list, view, history, resolve i reject; gui i book wymagają gracza. Książkowa skrzynka mieści 4 zgłoszenia na stronę. Skrzynka ekwipunku mieści 45 wpisów na stronę; listy tekstowe korzystają z `reports.page-size` (domyślnie `10`, zakres 1–50).

W książce „Rozpatrz” lub „Odrzuć” wyświetla na czacie odnośnik wstawiający komendę. Dopisz uzasadnienie i wyślij ją, aby zapisać decyzję. Przyciski książki wygasają po pięciu minutach; otwarcie kolejnego menu książkowego unieważnia poprzednie. Jeśli przycisk wygasł, otwórz menu ponownie.

## Wybór interfejsu

Interfejs gracza i administracji wybierasz niezależnie w `config.yml`:

```yaml
reports:
  player-interface: AUTO
  admin-interface: BOOK
  page-size: 10
  max-open-per-reporter: 3
  use-dialogs: true
  admin-use-dialogs: true
  reasons:
    - "Cheating"
    - "Spamming"
```

| Wartość | Działanie |
| --- | --- |
| `AUTO` | Korzysta z `use-dialogs` dla graczy i `admin-use-dialogs` dla administracji; w pozostałych przypadkach ekwipunek/czat. |
| `GUI` | Menu ekwipunku i szczegóły na czacie. |
| `DIALOG` | Natywne dialogi na Paper 1.21.7+; ekwipunek/czat na starszym Paper i Spigot. |
| `BOOK` | Klikalne książki na Paper, Folia i Spigot. |

Książka otwiera się bez dodawania przedmiotu do ekwipunku. Teksty książek zmienisz w sekcji `report-book` pliku językowego, a komunikaty w `reports`. Długie powody i uzasadnienia zajmują kilka stron; własne szablony muszą uwzględniać rozmiar strony Minecraft. Systemowy napis „Page … of …” zależy od języka klienta.

Po edycji konfiguracji wykonaj `/prx validate` i `/prx reload`. Po wymianie JAR uruchom serwer ponownie. [Konfiguracja](CONFIGS.md) opisuje domyślne tłumaczenia i bezpieczne przeładowanie.
