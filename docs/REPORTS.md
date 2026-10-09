# [PL] Zgłoszenia graczy (Paper i Spigot)

- `/report` — wybór gracza i powodu w formularzu Paper lub GUI.
- `/report gracz` — wybór powodu dla wskazanego gracza.
- `/report gracz powód` — wysłanie własnego powodu (3–255 znaków).
- `/reports` — natywny dialog na Paper 1.21.7+; panel ekwipunku na starszym Paper i Spigot; lista tekstowa w konsoli.
- `/reports gui [strona]` — panel, 45 zgłoszeń na stronę.
- `/reports list [strona]` — lista tekstowa; wielkość strony ustawia `reports.page-size` (1–50).
- `/reports view id` — autor, zgłoszony gracz, powód, data i decyzja. W otwartym zgłoszeniu kliknięcie komendy wstawia ją do czatu; należy dopisać uzasadnienie i wysłać.
- `/reports resolve id uzasadnienie` — zamknięcie jako rozpatrzone.
- `/reports reject id uzasadnienie` — zamknięcie jako odrzucone.
- `/reports history [strona]` — historia decyzji, dostępna również przez przycisk w panelu.

Odczyt i powiadomienia: `punisherx.see.reports`. Rozpatrywanie i odrzucanie: `punisherx.manage.reports` (domyślnie OP). Zarządzający może również przeglądać zgłoszenia. Obowiązują istniejące uprawnienia nadrzędne pluginu.

Limit otwartych zgłoszeń jednego gracza określa `reports.max-open-per-reporter` (domyślnie 3). Jeden autor może mieć tylko jedno otwarte zgłoszenie na tego samego gracza. Rozpatrzenie lub odrzucenie zwalnia miejsce w limicie. Zamknięcie zapisuje status, nazwę administratora, uzasadnienie i czas; nie nakłada automatycznie kary. Istniejące zgłoszenia pozostają otwarte. Nowa tabela `report_resolutions` jest tworzona podczas uruchomienia pluginu, bez usuwania danych. Zgłoszenia są wspólne dla serwerów korzystających z tej samej bazy, tak jak przed zmianą.

Natywny panel administratora można wyłączyć przez `reports.admin-use-dialogs: false`. Na wersjach bez API dialogów plugin automatycznie wybiera starszy interfejs.

Po zainstalowaniu nowego JAR uruchom ponownie serwer. Sprawdź, czy używane tłumaczenie zawiera nowe klucze sekcji `reports` (dołączono polski i angielski).

## Interfejs książkowy

Książki działają na Paper, Folia i Spigot jako dodatkowy sposób obsługi istniejących zgłoszeń. W `config.yml` wybierz niezależnie interfejs gracza i administracji:

```yaml
reports:
  player-interface: BOOK
  admin-interface: BOOK
```

Dostępne wartości: `AUTO`, `GUI`, `DIALOG`, `BOOK`. `AUTO` zachowuje dotychczasowe ustawienia `use-dialogs` i `admin-use-dialogs`. `DIALOG` korzysta z GUI na serwerach bez API dialogów. Aktualizacja konfiguracji do wersji 168 dodaje nowe ustawienia jako `AUTO`.

`/report` w trybie BOOK pozwala wybrać gracza online lub rozłączonego w ostatniej godzinie, kategorię i potwierdzić zgłoszenie. `/report gracz` otwiera wybór kategorii. Własny powód nadal można podać przez `/report gracz powód`.

`/reports book [strona]` otwiera książkową skrzynkę niezależnie od konfiguracji. W trybie administracji BOOK również `/reports`, `/reports list`, `/reports history` i `/reports view id` używają książek. Panel zawiera szczegóły, powód, datę, historię i decyzję. Kliknięcie „Rozpatrz” lub „Odrzuć” pokazuje na czacie odnośnik wstawiający komendę; administrator dopisuje uzasadnienie i wysyła ją. Uprawnienia są ponownie sprawdzane przy zapisie decyzji.

Sekcja `report-book` w pliku językowym zawiera tytuł, autora, nagłówki, treść stron, wpisy i przyciski. Polskie i angielskie teksty są gotowe; pozostałe dołączone języki mają dla nowych kluczy tekst angielski. Systemowego napisu klienta „Page … of …” nie można zmienić konfiguracją pluginu. Długie powody i uzasadnienia są dzielone na strony; przy własnych szablonach trzeba uwzględnić rozmiar strony Minecraft.

Książka otwiera się bez przekazywania przedmiotu do ekwipunku. Akcje są przypisane do gracza, jednorazowe i ważne przez pięć minut. Otwarcie kolejnego menu unieważnia poprzednie przyciski. `BookManager` stanowi wspólną podstawę dla kolejnych menu książkowych.

## Weryfikacja

Automatycznie: `./gradlew :punisherx-paper:test :punisherx-spigot:test :punisherx-paper:shadowJar :punisherx-spigot:shadowJar`. Testy automatyczne nie zastępują sprawdzenia całego przepływu na uruchomionym serwerze ani testów wszystkich obsługiwanych silników baz danych.

Test na serwerze z kontem gracza i administratora:

1. Wyślij zgłoszenie przez GUI oraz, w osobnej próbie, własny powód komendą. Sprawdź powiadomienie admina i blokadę drugiego otwartego zgłoszenia.
2. Sprawdź odmowę zgłoszenia samego siebie i powodu krótszego niż 3 lub dłuższego niż 255 znaków.
3. Otwórz `/reports`, kliknij zgłoszenie, rozpatrz je z uzasadnieniem. Sprawdź historię i możliwość ponownego zgłoszenia przez autora.
4. Powtórz dla odrzucenia. Spróbuj ponownie zamknąć ten sam ID — decyzja nie może zostać nadpisana.
5. Sprawdź konto bez uprawnień i konto z samym `punisherx.see.reports`: drugie może czytać, ale nie rozpatrywać.
6. Sprawdź `/reports list`, `/reports view` i zamknięcie w konsoli; przy większej liczbie zgłoszeń sprawdź kolejne strony.
7. Uruchom serwer ponownie i sprawdź zachowanie otwartych zgłoszeń oraz historii.

8. Ustaw oba interfejsy na BOOK i powtórz wysłanie, przeglądanie, historię oraz decyzje. Sprawdź nawigację przy wielu graczach/kategoriach/zgłoszeniach i długie powody.
9. Sprawdź klienta po upływie pięciu minut oraz po ponownym otwarciu menu; stare przyciski nie mogą ponownie wysłać zgłoszenia.
