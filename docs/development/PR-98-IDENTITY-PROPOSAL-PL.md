# Propozycja bezpiecznego rozwiązania PR #98

Cel: komendy na serwerze B mogą wskazać właściwe konto gracza znanego tylko serwerowi A. Nie pobieramy UUID z historii kar i nie generujemy UUID zastępczego po nieudanym rozpoznaniu tożsamości.

## Niezmienniki

- UUID identyfikuje konto w konkretnej domenie tożsamości. Nick jest zmiennym atrybutem, IP jest adresem sieciowym. Żadne z nich nie zastępuje UUID.
- Wynik musi obejmować domenę (realm), UUID, źródło potwierdzenia oraz kanoniczny nick, jeśli jest znany. Brak wyniku, konflikt i awaria to osobne wyniki, nigdy sztucznie wygenerowany UUID.
- Nie przenosimy automatycznie kar, notatek ani uprawnień między UUID na podstawie takiego samego nicku.
- Dane z przyjętego połączenia można uznać za zaufane wyłącznie zgodnie z konfiguracją uwierzytelnienia: bezpośredni serwer online albo właściwie zabezpieczony i zaufany forwarding proxy. Backend offline za proxy nie jest automatycznie dowodem tożsamości.

## Etap 1: ścisły resolver

Wprowadzić wynik rozpoznania: Resolved(identity), NotFound, Ambiguous, Unavailable. Każdy wynik różny od Resolved kończy komendę przed zapisem lub usunięciem kary, dostępem do notatek, teleportacją i komunikatami do proxy.

Jawny UUID: sprawdzić składnię i używać go wyłącznie w skonfigurowanej domenie. Sama znajomość UUID nie uwierzytelnia gracza ani nie daje moderatorowi dodatkowych uprawnień. Nieistniejący UUID pozostaje poprawnym identyfikatorem celu dla operacji administracyjnych, jeśli obecna semantyka komendy to dopuszcza.

Nick: dopuścić bieżącą, potwierdzoną sesję w tej samej domenie albo bieżące rozstrzygnięcie przez autorytatywne źródło właściwe dla tej domeny. Historyczna nazwa, lokalny cache offline i ostatnio widziane powiązanie w bazie mogą być podpowiedzią dla administratora, ale nie samodzielnym rozstrzygnięciem operacji. Jeśli nie da się potwierdzić aktualnego właściciela nicku, poprosić o jawny UUID.

Tryb offline: nie można dowieść tożsamości realnej osoby samym nickiem. Obsługiwać go jako osobną domenę zgodnie ze świadomie przyjętym modelem serwera, a nie fallback trybu online. Nie mieszać UUID z online, offline, innych proxy ani konfiguracji mixed-mode.

## Etap 2: wspólny rejestr

Rejestr indeksować kluczem (realm_id, uuid), z polami canonical_name, normalized_name, authority, last_confirmed_at oraz opcjonalnym numerem wersji. Oddzielna historia aliasów służy diagnostyce; nie służy automatycznemu rozwiązywaniu nazw. realm_id musi być jawnie uzgodniony dla serwerów tej samej sieci, niezależnie od nazwy serwera używanej do filtrowania kar.

Zapisy pochodzą tylko z zaufanej ścieżki logowania/uwierzytelnienia. Wspólną bazę i uprawnienia zapisu trzeba traktować jako część granicy zaufania; dowolny inny plugin mający pełny dostęp do tabel może naruszyć tę własność.

Rejestr pomaga odnaleźć konto między serwerami, ale data last_confirmed_at ani TTL nie dowodzą, że nick nie zmienił właściciela. Tryb ścisły nadal wymaga aktualnego potwierdzenia lub jawnego UUID. W razie równoczesnych zmian nicków/konfliktu wynik to Ambiguous; brak strategii „ostatni zapis wygrywa” dla wyboru tożsamości. Zastosować transakcje i kontrolę wersji zapisów.

## Osobna ścieżka IP

Cel komendy BANIP rozpoznawać jako PlayerTarget(identity) lub IpTarget(address), bez wspólnego pola uuid na poziomie logiki domenowej. Do IP nie uruchamiać resolvera gracza. Używać walidatora adresów bez DNS; ustalić i konsekwentnie stosować kanoniczny zapis IPv4/IPv6 dla nowych danych oraz migrację istniejących reprezentacji.

Historyczne BANIP w punishmenthistory.uuid nie są kandydatem do rejestru graczy. Nie importować historii kar jako potwierdzonych tożsamości. Jeśli potrzebne są propozycje podczas migracji, oznaczyć je jako niezweryfikowane i wymagać potwierdzenia przed użyciem.

## Wdrożenie

1. Dodać typowany resolver i komunikaty błędów; zinwentaryzować wszystkie wywołania resolvePlayerUuid, GUI, API i proxy.
2. Oddzielić IP od tożsamości gracza. Zachować bezpośrednie UUID jako drogę awaryjną administratora, bez zgadywania po nicku.
3. Dodać rejestr i zapis ze ścieżek zaufanych logowań; dane zbierać bez uruchamiania automatycznych migracji kar.
4. Włączyć rozpoznawanie między serwerami tylko przy zgodnej domenie oraz źródle uwierzytelnienia. Operacje SQL i sieciowe wykonywać poza wątkiem serwera; przed skutkami ponownie sprawdzić ważność żądania, wynik i uprawnienia nadawcy.
5. Przy zmianie schematu zachować dotychczasowe UUID kar. Audytować wybór celu: operator, wejście, realm, wynikowy UUID i źródło, bez publikowania prywatnych danych graczy.

## Testy akceptacyjne

- Gracz znany tylko A: na B działa jawny UUID; nick działa po aktualnym potwierdzeniu w tej samej domenie.
- Konto A zmienia nick, konto B przejmuje stary: bez dowodu aktualnego właściciela brak operacji; z potwierdzeniem wskazane B, bez przenoszenia danych A.
- Najnowszy wpis BANIP nigdy nie wpływa na wybór UUID.
- Konflikt nazw, opóźnione zdarzenia sesji i równoległe zapisy nie wybierają konta przez kolejność wpisów.
- Awaria DB/źródła uwierzytelnienia nie generuje UUID offline i nie nakłada ani nie usuwa kary.
- Sieci online/offline/proxy o różnych domenach nie współdzielą rozstrzygnięć.
- Komendy karania, cofania kar i notatek zachowują te same reguły i uprawnienia.

Propozycja projektowa, bez implementacji #98. Najbezpieczniejszy mały krok: zrezygnować z fallbacku historii i wymagać UUID tam, gdzie aktualnej nazwy nie można potwierdzić.
