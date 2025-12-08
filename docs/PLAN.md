# PunisherX v2.0.0 – Plan architektury i rozwoju

---
## 1. Główne cele projektu

1. Ujednolicony silnik kar (core) współdzielony przez:
    - serwery Paper/Spigot,
    - Folia (region/thread-per-entity),
    - proxy (Velocity).
2. Dwa poziomy API:
    - **API zewnętrzne** – dla innych pluginów oraz dodatków (addons),
    - **API wewnętrzne** – czytelny podział logiki wewnątrz PunisherX, bez overengineeringu.
3. Spójność techniczna:
    - jedna biblioteka cache – **Caffeine** – używana wszędzie,
    - **SyntaxCore** do logowania, aktualizacji, migracji konfiguracji,
    - wspólny **messageHandler** do wszystkich komunikatów.
4. Wysoka wydajność:
    - maksymalne przeniesienie IO (bazy, HTTP, pliki) poza główny wątek,
    - dostosowanie schedulera do Paper i Folii (region tasks).
5. Kompatybilność wersji:
    - obsługa **MC 1.20.6 – 1.21.11** poprzez cienką warstwę kompatybilności.
6. Rozszerzalność:
    - moduły addons (`punisherx-addons/*`) rozwijane na tym samym core,
    - możliwość tworzenia zewnętrznych pluginów integrujących się przez `punisherx-api`.
7. Transparentny proces rozwoju:
    - każdy istotny krok dokumentowany w devlogu z **datą, godziną i sugestią następnego kroku**.

---
## 2. Struktura projektu (multi-module)

Root projektu jako multi-module Gradle (Kotlin/JVM):

- **punisherx-api**
- **punisherx-core**
- **punisherx-platform-paper**
- **punisherx-platform-folia**
- **punisherx-platform-velocity**
- **punisherx-compat**
- **punisherx-addons/**
    - **punisherx-addons-discord** (webhooki, future: bot)
    - **punisherx-addons-gui-extra** (rozszerzone GUI)
    - **punisherx-addons-webpanel** (backend pod panel www / API HTTP)
    - (future) inne dodatki: np. `punisherx-addons-anticheat-bridge`, `punisherx-addons-analytics` itp.

Każdy moduł jest odseparowany, ale oparty o wspólny core i API.

---
## 3. Opis modułów

### 3.1. `punisherx-api`

**Cel:** stabilne, publiczne API dla innych pluginów i addonów.

- Interfejs główny: `PunisherXApi` (metody typu `isMuted`, `isBanned`, `getActivePunishments`, `punish(...)`).
- Dataclasses: `Punishment`, `PunishmentType`, `PunishmentRequest`, `PunishmentResult`.
- `PunisherXApiProvider` – statyczny provider instancji API (bez zależności od Bukkit/Velocity).
- Eventy API (wspólne kontrakty):
    - `PunisherPrePunishEvent` (Cancellable),
    - `PunisherPunishEvent`,
    - `PunisherUnpunishEvent`.

W module platformowym (Paper/Folia/Velocity) powstają konkretne implementacje eventów dostosowane do danej platformy.

---
### 3.2. `punisherx-core`

**Cel:** silnik logiki kar niezależny od platformy.

Główne elementy:

- Interfejsy wewnętrzne (proste, bez nadmiaru warstw):
    - `PunishmentService` – operacje `warn`, `mute`, `ban`, `kick`, `jail`, `unban`, `unmute` itd.
    - `PunishmentQueryService` – `isMuted`, `isBanned`, `getActivePunishments`, `getHistory`.
    - `Storage` – operacje na bazie danych (async / future / coroutine),
    - `SchedulerAdapter` – abstrakcja nad schedulerem (Paper/Folia/Velocity),
    - `PlatformAdapter` – podstawowe operacje na graczach/wiadomościach niezależne od konkretnego API,
    - `CacheProvider` – udostępnia instancje Caffeine cache.
- Implementacja cache:
    - Caffeine jako jedyna biblioteka cache w projekcie,
    - np. `CaffeineCacheProvider` tworzący cache dla kar, historii, limitów itd.
- Integracja z SyntaxCore:
    - logger (centralny logging w core),
    - system aktualizacji,
    - migracje configów (inicjowane z platform, logika może być w core),
    - messageHandler (ładowanie treści, formatowanie komunikatów).
- Brak zależności od Bukkit/Velocity – tylko od `punisherx-api`, SyntaxCore i bibliotek technicznych (Caffeine, JDBC itp.).

---
### 3.3. `punisherx-platform-paper`

**Cel:** plugin dla Paper (docelowo również działający na Spigot), łączący core z Paper API.

- Główny plugin: `PunisherXPaperPlugin : JavaPlugin`.
- Składanie zależności w `onEnable` (prosty manualny wiring):
    - tworzenie `SchedulerAdapter` dla Paper,
    - tworzenie `PlatformAdapter` (Paper/Bukkit specific),
    - inicjalizacja `PunishmentService` / `PunishmentQueryService` z core,
    - podłączanie `CacheProvider` i `messageHandler`.
- Rejestracja komend i listenerów Paper:
    - komendy: `/warn`, `/mute`, `/ban`, `/kick`, `/jail` itd.
    - eventy: join/quit, chat, itd.
- GUI:
    - istniejące klasy `GUI` przeniesione do dedykowanego pakietu w module Paper,
    - cała logika biznesowa (co się dzieje po kliknięciu) delegowana do core,
    - warstwa Paper odpowiada tylko za inventory, itemy, sloty.
- Integracja z SyntaxCore:
    - hooking update checkera,
    - migracje configów (`config.yml`, `messages.yml`, `gui.yml`),
    - centralne logowanie.

---
### 3.4. `punisherx-platform-folia`

**Cel:** wsparcie Folii z poprawnym użyciem region/thread-per-entity scheduler.

- Osobny moduł lub wariant Paper, ale z inną implementacją `SchedulerAdapter`.
- `SchedulerAdapter` wykorzystuje API Folii do zadań per region/gracz,
- Core jest ten sam – żadnej dodatkowej logiki zależnej od Folii w warstwie core.


---
### 3.5. `punisherx-platform-velocity`

**Cel:** plugin na proxy Velocity, korzystający z tego samego core.

- Główny plugin Velocity (klasa startowa z adnotacjami Velocity).
- Integracja komend proxy (globalne `/ban`, `/mute` itd. na poziomie proxy).
- Źródło danych: wspólna baza danych (zalecane) z serwerami backend.
- Core daje spójny model kar, Velocity zapewnia: globalne bany, synchronizację, hooki proxy.


---
### 3.6. `punisherx-compat`

**Cel:** warstwa kompatybilności dla różnych wersji MC (1.20.6 – 1.21.10).

- `VersionDetector` – wykrywa wersję serwera.
- `Version` / `ServerVersion` – enum opisujący wspierane wersje.
- `VersionAdapter` – interfejs na różnice wersji (np. specyficzne rzeczy wymagające innego podejścia w 1.21.x).
- Implementacje adapterów wykorzystywane tylko tam, gdzie Paper API nie wystarcza.


---
### 3.7. `punisherx-addons` – dodatki

#### 3.7.1. `punisherx-addons-discord`

**Cel:** obsługa webhooków Discord (oraz w przyszłości bota), oparta na core.

- Obecne klasy `DiscordWebHook` przeniesione i uporządkowane w tym module.
- Zasady:
    - całość HTTP → **zawsze async**,
    - treści komunikatów z `messageHandler` (format + język),
    - logowanie przez SyntaxCore.
- W przyszłości: osobny serwis bota, ale nadal oparty o `punisherx-api`/core.


#### 3.7.2. `punisherx-addons-gui-extra`

**Cel:** dodatkowe GUI ponad core’owy zestaw (np. statystyki, rozbudowane przeglądanie historii, masowe operacje).

- Moduł zależny od `punisherx-api` i `punisherx-platform-paper` (lub samego core + prosty adapter).
- W całym GUI:
    - dane logiki pobierane z core (`PunishmentService` / `PunishmentQueryService`),
    - żadnego bezpośredniego dostępu do bazy.


#### 3.7.3. `punisherx-addons-webpanel`

**Cel:** backend pod panel www (lub API HTTP) do zarządzania karami.

- Serwer HTTP (np. Ktor / inny lekki framework) odpalany jako część pluginu lub jako osobna aplikacja korzystająca z `punisherx-api`.
- Endpointy REST/JSON do:
    - podglądu kar,
    - nakładania/odwoływania kar,
    - pobierania statystyk.
- Autoryzacja po stronie panelu, wtyczka zapewnia tylko logikę kar.


#### 3.7.4. Przyszłe dodatki

Przykłady:

- `punisherx-addons-anticheat-bridge` – integracja z antycheatami przez zewnętrzne API,
- `punisherx-addons-analytics` – agregacja statystyk kar, dashboards.

Każdy addon:
- opiera się wyłącznie na `punisherx-api` (plus ewentualnie dostęp do SyntaxCore),
- nie dotyka bezpośrednio wewnętrznych klas core (brak tight-coupling).

---
## 4. Wymagania techniczne (globalne zasady)

1. **Prostota kodu:**
    - brak nadmiaru `Service`, `Provider`, `Manager` bez potrzeby,
    - małe, wyraźne interfejsy i klasy,
    - manualne składanie zależności w `onEnable`/konstruktorach.

2. **Spójne biblioteki:**
    - cache → **zawsze Caffeine**,
    - update/log/migracje → **zawsze SyntaxCore**,
    - komunikaty → **zawsze messageHandler** z SyntaxCore.

3. **Asynchroniczność:**
    - baza danych, HTTP (Discord/webpanel), cięższe obliczenia → zawsze poza main thread,
    - core udostępnia metody async (coroutines/Future), platformy używają `SchedulerAdapter`.

4. **Kompatybilność wersji:**
    - głównie używasz stabilnego API Paper,
    - jeśli trzeba wejść w szczegóły wersji – przez `punisherx-compat`.

5. **Integracja z SyntaxCore i Nexus:**
    - w root `build.gradle.kts` dodane repozytoria:
        - `https://nexus.syntaxdevteam.pl/repository/maven-releases/`,
        - `https://nexus.syntaxdevteam.pl/repository/maven-snapshots/`.
    - wszystkie moduły korzystają z tych samych wersji SyntaxCore/messageHandler.

---
## 5. API wewnętrzne vs zewnętrzne – podsumowanie

- **API zewnętrzne (`punisherx-api`):**
    - prosta fasada `PunisherXApi`,
    - eventy API,
    - dataclasses stabilne wydaniowo,
    - używane przez inne pluginy i addons.

- **API wewnętrzne (core):**
    - `PunishmentService`, `PunishmentQueryService`, `Storage`, `SchedulerAdapter`, `PlatformAdapter`, `CacheProvider`,
    - brak zależności od Bukkit/Velocity,
    - implementacje dostarczane przez moduły platformowe.

---
## 6. SchedulerAdapter i praca poza głównym wątkiem

- `SchedulerAdapter` jako jedyny punkt planowania zadań w core:
    - `runSync(block)`,
    - `runAsync(block)`,
    - dla Folii: `runRegion(player/loc, block)`.
- Zasada:
    - core korzysta tylko z `SchedulerAdapter`,
    - implementacje:
        - w `punisherx-platform-paper` – Paper scheduler,
        - w `punisherx-platform-folia` – Folia region scheduler,
        - w `punisherx-platform-velocity` – scheduler Proxy.

---
## 7. System devlog (dokumentacja kroków)

Struktura:

- katalog: `docs/devlog/` w repo,
- każdy krok = osobny plik `.md` z nazwą:
    - `YYYY-MM-DD_HH-mm_punisherx-<krotki-opis>.md`.

Zawartość (template):

```markdown
# [Data: 2025-11-20 18:42] Dodanie modułu punisherx-api

## Co zostało zrobione
- ...

## Uzasadnienie
- ...

## Sugerowany następny krok
- ...
```

Dzięki temu każdy element planu:
- jest odnotowany z datą i godziną,
- ma jasny opis,
- zawiera **sugerowany następny krok**.

---
## 8. Proponowana kolejność realizacji

1. **Inicjalny podział projektu na moduły**
    - utworzenie `punisherx-api`, `punisherx-core`, `punisherx-platform-paper`, `punisherx-compat`, `punisherx-addons` (puste szkielety),
    - konfiguracja wspólnych repozytoriów (Nexus SyntaxDevTeam, Paper itp.),
    - devlog: „Inicjalna struktura modułów PunisherX 2.0”.

2. **Migracja logiki kar do `punisherx-core`**
    - przeniesienie logiki warn/mute/ban/kick/jail do `PunishmentService` w core,
    - integracja z Caffeine (centralny `CacheProvider`),
    - devlog: „Migracja logiki kar do core i wprowadzenie CacheProvider (Caffeine)”.

3. **Integracja SyntaxCore w core i platformie Paper**
    - podłączenie loggera, systemu aktualizacji, migracji configów,
    - podłączenie messageHandler dla komunikatów,
    - devlog: „Integracja SyntaxCore (logging, update, config migrations, messageHandler)”.

4. **Publiczne API – `punisherx-api` + implementacja w Paper**
    - dodefiniowanie `PunisherXApi` i dataclasses,
    - implementacja w `punisherx-platform-paper` + rejestracja w `PunisherXApiProvider`,
    - devlog: „Publiczne API PunisherX dla innych pluginów”.

5. **Refaktoryzacja GUI w module Paper**
    - przeniesienie istniejących klas GUI do dedykowanego pakietu,
    - odseparowanie logiki od core (GUI tylko odpyta core),
    - devlog: „Refaktoryzacja GUI w kierunku core-driven UI”.

6. **Wprowadzenie `SchedulerAdapter`**
    - implementacja dla Paper, zastąpienie bezpośrednich wywołań schedulerów w core,
    - upewnienie się, że IO/DB/HTTP są wykonywane zawsze poza main thread,
    - devlog: „Wprowadzenie SchedulerAdapter, ujednolicenie async”.

7. **Warstwa kompatybilności `punisherx-compat`**
    - implementacja `VersionDetector` i ewentualnych `VersionAdapter`,
    - dostosowanie kodu tam, gdzie wymagana jest wersjo-zależność,
    - devlog: „Warstwa kompatybilności 1.20.6–1.21.10”.

8. **Moduł Folia – `punisherx-platform-folia`**
    - implementacja `SchedulerAdapter` dla Folii,
    - przetestowanie operacji dotykających świata/graczy pod Folia API,
    - devlog: „Wsparcie Folia (region scheduler)”.

9. **Moduł Velocity – `punisherx-platform-velocity`**
    - podstawowy plugin proxy korzystający z core,
    - globalne bany/muty, integracja z tą samą bazą danych,
    - devlog: „Wsparcie Velocity jako proxy dla PunisherX”.

10. **Addon `punisherx-addons-discord`**
    - migracja i refaktoryzacja istniejących webhooków do osobnego modułu,
    - pełna asynchroniczność, integracja z messageHandler i SyntaxCore,
    - devlog: „Addon Discord – webhooki oparte o core”.

11. **Addon `punisherx-addons-gui-extra`**
    - rozbudowane GUI (np. statystyki, masowe operacje),
    - wykorzystanie wyłącznie API/core, bez bezpośrednich dostepów do DB,
    - devlog: „Addon GUI Extra – rozszerzone interfejsy użytkownika”.

12. **Addon `punisherx-addons-webpanel`**
    - backend HTTP (np. Ktor) udostępniający REST/JSON dla panelu www,
    - logika oparta o `punisherx-api`, zabezpieczenia po stronie panelu,
    - devlog: „Addon Webpanel – backend HTTP oparty o PunisherX core”.

13. **Dalsze dodatki i integracje**
    - addons typu anticheat-bridge, analytics itd.,
    - wszystkie oparte o stabilne `punisherx-api`,
    - devlog per dodatek.

---

Ten dokument jest szkieletem, który możesz rozwijać, dopisując szczegóły implementacyjne w miarę realizacji kolejnych kroków – każdy krok opisany w devlogu z datą, godziną i sugerowanym następnym krokiem.

