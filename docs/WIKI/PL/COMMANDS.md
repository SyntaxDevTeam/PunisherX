## Podstawowe komendy
* `/ban <gracz> (czas) <powód> [--force]` — banuje i natychmiast wyrzuca gracza, jeśli jest online; przy problemie z bazą używa listy banów Paper `PROFILE` jako fallback.
* `/banip <ip|gracz|uuid> (czas) <powód> [--force]` — banuje wszystkie IP powiązane z celem, a przy błędach bazy uruchamia komendę Paper `ban-ip`; dodatkowo wyrzuca cele online.
* `/unban <ip|gracz>` — zdejmuje bany po nazwie gracza, IP lub UUID, łącznie z powiązanymi IP w bazie.
* `/jail <gracz> (czas) <powód> [--force]` — teleportuje gracza do skonfigurowanego więzienia (z uwzględnieniem bypassa, chyba że użyto `--force`) i zapisuje poprzednią pozycję do zwolnienia.
* `/unjail <gracz>` — usuwa aktywną karę więzienia z cache i teleportuje gracza do lokalizacji wyjścia z więzienia, jeśli jest ustawiona.
* `/setjail <radius>` — zapisuje lokalizację nadawcy i promień jako obszar więzienia w `config.yml`.
* `/setunjail` — zapisuje lokalizację nadawcy jako punkt po wyjściu z więzienia używany przez `/unjail`.
* `/mute <gracz> (czas) <powód> [--force]` / `/unmute <gracz>` — nakłada/zdejmuje muta na czat, z opcjonalnym `--force`, który ignoruje bypass dla graczy online.
* `/warn <gracz> (czas) <powód>` / `/unwarn <gracz>` — dodaje lub usuwa ostrzeżenia, obsługując ostrzeżenia czasowe wyzwalające akcje z konfiguracji.
* `/kick <gracz> <powód>` — natychmiast wyrzuca gracza z serwera z podanym powodem.
* `/clearall <gracz>` — usuwa aktywne bany, muty i warny gracza oraz powiadamia go, jeśli jest online.
* `/change-reason <penalty_id> <new_reason>` — aktualizuje zapisany powód istniejącej kary.
* `/check <gracz> <all|warn|mute|ban|jail>` — wyświetla aktywne kary (lub dane własne gracza), filtrowane po typie.
* `/history <gracz> (strona)` — stronicowana historia wszystkich kar danego gracza.
* `/banlist (strona) [--h]` — stronicowana lista aktualnie zbanowanych graczy; `--h` pokazuje bany historyczne.

## Narzędzia administracyjne

* `/punisherx|/prx help|version|reload|export|import|migrate <from> <to> [--force]` — główna komenda admina do pomocy, reloadu konfiguracji oraz eksportu/importu/migracji bazy (wymaga potwierdzenia, chyba że użyto `--force`).
* `/panel` — otwiera dashboard GUI PunisherX, jeśli jest włączony.
* `/langfix` — konwertuje legacy placeholdery `{}` w plikach językowych do formatu MiniMessage `<>`.
* `/cache` — komenda debugująca, która wypisuje wpisy kar z cache do nadawcy.
* Konfigurowalne aliasy z `config.yml` mogą odwzorować powyższe komendy (np. alternatywne nazwy dla `/ban`, `/history`, `/kick` itd.).

> Argumenty w nawiasach ostrych <> są wymagane, a w nawiasach okrągłych () opcjonalne. Argumenty w nawiasach kwadratowych [] są dodatkowymi opcjami i używa się ich w sytuacjach niestandardowych. Jeśli używasz czasu (kara na określony okres), podawaj go w formatach: Xs|Xm|Xh|Xd, gdzie X oznacza sekundy|minuty|godziny|dni. Pozostawienie pola czasu pustego oznacza karę bezterminową, która nie wygaśnie bez komendy cofającej karę.
