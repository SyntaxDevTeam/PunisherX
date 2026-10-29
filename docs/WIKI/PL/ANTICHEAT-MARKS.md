# Integracja z Anti-cheatami i oznaczanie kar

[Strona główna](HOME.md) · [Komendy](COMMANDS.md) · [English](../EN/ANTICHEAT-MARKS.md)

PunisherX na Paper/Folia i Spigot pozwala powiązać karę z konkretną detekcją Anti-cheata. W konfiguracji akcji swojego Anti-cheata ustaw zwykłe komendy PunisherX z `--mark` lub `--mark=<detekcja>`. Oznaczona komenda nakłada taką samą karę jak zwykła, dopóki dana detekcja nie jest wstrzymana. Nazwa oznaczenia jest wybierana przez administratora; nie wymaga dedykowanego adaptera danego Anti-cheata.

```text
ban Steve 7d Cheating --mark=grim.speed --silent
warn Steve Suspicious movement --mark=grim.speed
punish Steve "Movement cheats" --mark=grim.speed
```

Komendy w konfiguracji Anti-cheata są zwykle wpisywane bez początkowego `/`; zastosuj składnię wymaganą przez ten plugin. W przykładzie szablon `Movement cheats` musi istnieć w `punish-templates.yml`.

Obsługiwane komendy: `/ban`, `/banip`, `/mute`, `/warn`, `/jail`, `/kick` (również `all`) i `/punish`. Oznaczenie jest niezależne od wyświetlanego powodu. Samo `--mark` używa nazwy `default`. Nazwy nie rozróżniają wielkości liter, mają 1–64 znaki ASCII: litery, cyfry, `_`, `.`, `:` lub `-`, a pierwszy znak musi być literą lub cyfrą. Podaj dokładnie jedno oznaczenie; błędne lub powtórzone przerywa komendę. `--force` omija ochronę gracza, lecz nie omija wstrzymanej detekcji.

## Cofanie kar i wstrzymywanie błędnej detekcji

Wymagane jest `punisherx.cmd.unmark` (domyślnie OP; działają też standardowe reguły właściciela i uprawnień zgodności). Nakładanie oznaczonych kar wymaga uprawnień odpowiedniej komendy kary.

| Komenda | Wynik |
| --- | --- |
| `/unmark Steve` | Cofnięcie wszystkich oznaczonych kar gracza. |
| `/unmark all` | Cofnięcie wszystkich oznaczonych kar w dostępnych zakresach serwerowych. |
| `/unmark Steve grim.speed` | Cofnięcie tylko kar Steve'a oznaczonych `grim.speed`. |
| `/unmark all grim.speed` | Cofnięcie kar tej detekcji wszystkim graczom. |
| `/unmark pause grim.speed` | Trwałe wstrzymanie detekcji i cofnięcie jej dotychczasowych oznaczonych kar w jednej transakcji. |
| `/unmark resume grim.speed` | Usunięcie wstrzymania w bieżącym zakresie serwera; kolejne oznaczone kary znów są dozwolone. |

Pierwsze cztery warianty cofają dotychczasowe kary. Aby blokować także przyszłe komendy błędnej detekcji, użyj `pause`. Dodaj `--silent`, aby otrzymać prywatne potwierdzenie oznaczone jako ciche; `/unmark` nie rozsyła ogłoszeń ani nie uruchamia skonfigurowanych akcji kar.

Przykładowa obsługa fałszywych detekcji:

1. Wykonaj `/unmark pause grim.speed`.
2. Popraw konfigurację lub zaktualizuj Anti-cheat.
3. Wykonaj `/unmark resume grim.speed`.

Wstrzymanie przetrwa restart i przeładowanie pluginu. Aktualizacja Anti-cheata nie wznawia detekcji automatycznie. Wznowienie nie przywraca cofniętych kar.

## Zakresy serwerowe w sieci

Lokalny zakres `server` może cofać swoją historię i historię `network`; zakres `network` ma dostęp do całej historii. Wstrzymanie `network` obejmuje wszystkie serwery. Wstrzymanie lokalne obejmuje ten serwer oraz komendy wykonywane w zakresie `network`. `resume` usuwa wyłącznie wstrzymanie utworzone w bieżącym zakresie — wstrzymanie z innego zakresu trzeba wznowić tam, gdzie je utworzono. Używaj różnych nazw detekcji, gdy instalacje Anti-cheatów mają być zarządzane niezależnie.

## Co zostaje cofnięte?

- Usuwana jest dokładna oznaczona aktywna kara. Ręczne kary bez oznaczenia i kary innych detekcji pozostają aktywne.
- Oryginalna historia zostaje zachowana z informacją, kto i kiedy cofnął karę. Cofnięte wpisy szablonów przestają podnosić eskalację `/punish`.
- Oznaczony ban IP gracza obejmuje wszystkie zapisane dla niego wiersze IP; powiązanie używa UUID i przetrwa zmianę nicku.
- Lokalny cache jaila jest czyszczony, a gracz zwalniany, jeśli nie ma innego aktywnego jaila. Dla natychmiastowego zwolnienia wykonuj cofnięcie na serwerze, na którym przebywa gracz; cache innych instancji korzystających ze wspólnej bazy ma własny cykl aktualizacji.
- Oczekujące zdarzenia bana proxy są usuwane, jeśli nie ma już innego odpowiadającego bana. Wykonanego rozłączenia nie można cofnąć; gracz może połączyć się ponownie, jeśli nie blokuje go inny ban.

Oznaczone kicki zapisują się nawet przy wyłączonym `kick.history`. Wynik liczy wpisy historii, więc ban wielu IP może dać kilka wpisów. Wygasłe i wcześniej zdjęte kary także mogą otrzymać zapis cofnięcia. Ponowienie komendy nie cofa drugi raz tej samej historii.

Automatyczne komendy kar PunisherX w `actions` i starszych progach ostrzeżeń dziedziczą oznaczenie, także przez aliasy i przestrzeń nazw `punisherx:`. Oznaczenie źródłowe zastępuje oznaczenie akcji następczej. Komendy innych pluginów i komendy zdejmowania kar wykonują się zgodnie z ich konfiguracją. Efekty zewnętrzne, np. potrącenie waluty, nie są cofane przez `/unmark`.

## Kopie i niezawodność

Eksport, import i migracja silnika bazy zachowują oznaczenia i wstrzymania. Nieudany zapis oznaczonej kary nie tworzy nieśledzonego bana platformy. Przed pracami administracyjnymi zachowaj kopię bazy.
