# PunisherX 2.0 — plan architektury i przebudowy

Status: **plan analityczny do dalszego rozwoju; żadna opisana tu przebudowa nie została jeszcze zaimplementowana**.  
Data analizy: **30 października 2026 r.**  
Punkt odniesienia PunisherX: main, commit **5c421b5ac43dfcbfc077c9e7112fd12d52638551**.  
Punkt odniesienia SyntaxDB: main, commit **8287b3235e35f12230f07de3c5d4f97e93e0277e**.  
Zakres tego etapu: dokumentacja; bez zmian kodu, zależności, konfiguracji wykonawczej i numeru wersji.

## 1. Cel i zasady obowiązujące w 2.0

PunisherX 2.0 projektujemy jako nową architekturę. Układ klas, publiczne API, składnia, konfiguracja i schemat bazy 1.x nie są ograniczeniem projektowym. Nie tworzymy warstw zgodności z 1.x ani automatycznej migracji danych przy uruchomieniu.

Wymagania właściciela projektu:

1. Pełne interfejsy, abstrakcje i kontrakty dla każdej rodziny komend.
2. Jeden kompletny szablon obsługi kar: KICK, JAIL, WARN, MUTE, BAN oraz wyspecjalizowany BAN_IP.
3. Model wejścia: /&lt;komenda&gt; &lt;cel&gt; [czas] &lt;powód&gt; [flaga [wartość]]...
4. Na Paper bezwzględnie pełny Paper Brigadier Command API: drzewo, typowane argumenty, walidacja, uprawnienia i podpowiedzi.
5. Plik komendy wyłącznie definiuje interfejs i deleguje żądanie. Logika wykonawcza znajduje się w usługach.
6. Caffeine **3.3.1**, praca asynchroniczna dla I/O, atomowe decyzje i prawdziwe potwierdzenia etapów.
7. BAN_IP przyjmuje IP, UUID lub nick; ustala znane adresy i ich powiązania.
8. **SyntaxDB jest jedyną warstwą dostępu do baz danych**.

Pozostałe szczegóły w tym dokumencie są propozycją projektową: konkretnym punktem startowym do implementacji, a nie opisem obecnego działania. Przykłady 2.0 nie stanowią instrukcji obsługi wydania 1.x.

Nie dokładamy w tym etapie podziału Light/Standard/Premium. Architektura modułowa umożliwi osobną decyzję produktową później.

## 2. Co rzeczywiście znajduje się w obecnym projekcie

Analiza obejmuje strukturę repozytorium oraz ścieżki komend, nakładania i cofania kar, cache, odczytów egzekwujących kary, IP, API, schematu bazy i mostu Velocity. Nie jest pełnym audytem bezpieczeństwa ani potwierdzeniem zachowania na uruchomionym serwerze.

Ścieżki w tabeli odnoszą się do wskazanego wyżej commitu. Skrót P oznacza katalog
punisherx-paper/src/main/kotlin/pl/syntaxdevteam/punisher/.

| Obszar / dowód w repozytorium | Stan zastany | Konsekwencja dla 2.0 |
| --- | --- | --- |
| settings.gradle.kts | Moduły paper, spigot, velocity-bridge, bungee-bridge | Wspólny rdzeń należy wydzielić ponad platformy. |
| P/commands/CommandManager.kt | Rejestracja przez LifecycleEvents.COMMANDS; większość komend jako BasicCommand; osobne drzewa m.in. punish i unpunish | Obecna rejestracja Paper nie jest jeszcze pełnym typowanym interfejsem wszystkich komend. |
| P/commands/BanCommand.kt, MuteCommand.kt, WarnCommand.kt, JailCommand.kt, KickCommand.kt | Parsowanie, wyszukiwanie celu, decyzje, zapis, efekty i komunikaty w klasach komend | Przenieść wykonanie do wspólnej warstwy aplikacyjnej i strategii kar. |
| P/commands/PunishCommand.kt | Własne applyBan, applyBanIp, applyMute, applyWarn, applyKick, applyJail | Szablony muszą używać tych samych przypadków użycia co komendy bezpośrednie. |
| P/commands/PunishmentCommandOptions.kt | Wspólne flagi force/silent/mark, ale również sprawdzenie blokady znacznika i zapis do DB | Rozdzielić czystą składnię, polityki i persystencję. |
| P/commands/PunishmentCommandUtils.kt | Wspólne parsowanie czasu/powodu i komunikaty | Zastąpić kontraktami argumentów, walidacji i prezentacji. |
| P/basic/PunishmentCache.kt | Caffeine dla JAIL; limit 1000, wygaszanie po godzinie dostępu, jail_cache.json; również teleportacja i usuwanie kary | Cache nie powinien być magazynem stanu więzienia ani usługą wykonawczą. |
| P/basic/PunishmentChecker.kt | Zapytania DB w obsłudze czatu i komend gracza; JAIL częściowo z cache | Odczyt egzekwujący karę musi korzystać z przygotowanego obrazu pamięciowego. |
| P/players/PlayerIPManager.kt | Historia par UUID–IP; zaszyfrowane linie w pliku lub tabeli playercache; odczyty skanują i odszyfrowują rekordy | Relacyjne, indeksowane obserwacje IP zamiast przeszukiwania całego zbioru. |
| P/commands/BanIpCommand.kt | Nick/UUID: zapisane IP konta; literal IP: tylko podany adres; rozpoznanie IPv4 regexem; zapis kolejnych IP w pętli | Wspólny resolver IPv4/IPv6, jawna semantyka relacji i jedna transakcja całej operacji. |
| P/databases/DatabaseHandler.kt | SyntaxDB już używane; duża klasa łącząca CRUD, historię, zgłoszenia, migracje i webhook przy zapisie | Nie planować „pierwszego wdrożenia SyntaxDB”; przebudować odpowiedzialności i transakcje. |
| P/databases/PunisherDatabaseSchema.kt | Zachowanie nazw tabel 1.x, zakresów i wcześniejszych danych | Nowy schemat 2.0 bez warunków zgodności. |
| gradle/libs.versions.toml | Caffeine już 3.3.1; SyntaxDB już 0.1.0-R0.1-SNAPSHOT | Główny problem dotyczy architektury użycia, nie samego numeru zależności. |
| punisherx-paper/build.gradle.kts | compileOnly syntaxdb-core i pozostałe zależności rozdzielone | Wybrać jeden sposób dostarczania publicznego artefaktu SyntaxDB. |
| P/compatibility/platform/SchedulerAdapter.kt | Istnieją runAsync, runSync, runRegionally i runForPlayer | Zachować ideę adaptera; zastąpić niejednoznaczne „sync” kontraktami własności wątku. |
| P/api/PunisherXApi.kt | Publiczne odczyty CompletableFuture | Nowe API obejmuje również typowane operacje zapisu i ich wyniki. |
| punisherx-velocity-bridge/.../BridgeDatabase.kt | SyntaxDB, MySQL, bridge_events z globalnym processed i sprawdzanie bana | Dla 2.0 potrzebne odbiory per odbiorca, wersje i rozdzielenie migracji schematu. |
| punisherx-paper/src/test oraz tests/server | Testy m.in. flag, ciszy, BAN_IP, SyntaxDB, cofania, migracji i schedulerów | Wykorzystać scenariusze i doświadczenia; nie zachowywać starych API tylko dla testów. |

Podstawowe źródła repozytorium:

- [Drzewo PunisherX w analizowanym commicie](https://github.com/SyntaxDevTeam/PunisherX/tree/5c421b5ac43dfcbfc077c9e7112fd12d52638551).
- [Obecna dokumentacja komend](https://github.com/SyntaxDevTeam/PunisherX/blob/5c421b5ac43dfcbfc077c9e7112fd12d52638551/docs/WIKI/PL/COMMANDS.md).
- [Obecny katalog zależności](https://github.com/SyntaxDevTeam/PunisherX/blob/5c421b5ac43dfcbfc077c9e7112fd12d52638551/gradle/libs.versions.toml).

## 3. Granice warstw i proponowane moduły

| Moduł docelowy | Odpowiedzialność | Niedozwolone zależności |
| --- | --- | --- |
| punisherx-domain | Typy kar i celów, reguły czasu, polityki, niezmienne modele i zdarzenia | Paper, Bukkit, proxy, SQL, SyntaxDB, Caffeine |
| punisherx-application | Przypadki użycia, pipeline kar, cofanie, szablony, potwierdzenia, porty repozytoriów i efektów | Konkretne klasy platform i implementacje DB |
| punisherx-api | Publiczne DTO, wyniki, asynchroniczna fasada dla integracji | Player, CommandSender, Row, Connection, wewnętrzne klasy cache |
| punisherx-storage-syntaxdb | Repozytoria, transakcje, migracje, blokady DB, outbox | Komendy, GUI, teleportacja, wysyłka webhooków |
| punisherx-runtime | Caffeine, zarządzane executory, zegar, harmonogram wygasania, metryki | Implementacje reguł platformowych w rdzeniu |
| punisherx-paper | Bootstrap, pełny Brigadier, listenery, dialogi/GUI, efekty Paper/Folia | Własna alternatywna logika nakładania kar |
| punisherx-spigot | Osobny adapter platformy, jeżeli utrzymujemy produkt Spigot | Kopiowanie domeny i usług Paper |
| punisherx-velocity-bridge / punisherx-bungee-bridge | Egzekwowanie i synchronizacja w sieci, adaptery proxy | Tworzenie schematu przez konto tylko do odczytu |

To proponowany podział Gradle. Nie trzeba tworzyć dodatkowego modułu dla każdej klasy. Interfejsy portów należą do warstwy, która ich potrzebuje; implementacje do adapterów.

Paper/Folia stanowi pierwszy cel 2.0. Utrzymanie Spigota i Bungee jest osobną decyzją o zakresie wydania. Ich obecność w 1.x nie może wymusić BasicCommand ani ograniczenia Brigadiera na Paper. Minimalną wersję Paper i Javy należy zamrozić w etapie 0 na podstawie wybranego API, bez automatycznego przenoszenia macierzy 1.x.

SyntaxCore pozostaje wsparciem lifecycle i usług platformowych, MessageHandler odpowiada za renderowanie komunikatów. Żadna z tych bibliotek nie powinna ukrywać dodatkowego magazynu kar obok SyntaxDB.

## 4. Kompletny model kontraktów komend

### 4.1. Kontrakty wspólne

| Kontrakt | Docelowe operacje / dane | Odpowiedzialność |
| --- | --- | --- |
| CommandDefinition&lt;R&gt; | id, nazwa, aliasy, opis, argumenty, flagi, wymagania nadawcy, permission, useCaseId | Jedno źródło definicji dla drzewa, pomocy i dokumentacji. |
| CommandTreeFactory | build(definition), registerAll(registrar) | Wyłącznie budowa interfejsu platformy. |
| CommandRequestFactory&lt;R&gt; | fromParsed(contextSnapshot) | Tworzy typowane żądanie; bez SQL i bez nakładania kary. |
| AsyncCommandGateway | submit(request, replyTarget), getOperation(operationId) | Uruchamia właściwy przypadek użycia i przekazuje wynik prezenterowi. |
| CommandUseCase&lt;R, O&gt; | execute(request) → asynchroniczny wynik O | Wspólny punkt wejścia dla CLI, GUI, dialogu i API. |
| CommandPresenter&lt;O&gt; | accepted(receipt), completed(result), rejected(error) | Tłumaczenia i bezpieczne dostarczenie odpowiedzi. |
| ActorContext | actorId, rodzaj PLAYER/CONSOLE/SYSTEM/INTEGRATION, nazwa, capabilitySnapshot, origin | Jawna tożsamość wykonawcy; konsola nie jest UUID gracza. |
| RequestContext | operationId, correlationId, idempotencyKey, receivedAt, configVersion, source | Kontekst audytu i ponawiania. |
| CommandError | kod, klucz tłumaczenia, bezpieczne parametry, retryable | Jeden katalog błędów zamiast null/false jako każdej awarii. |
| PlatformExecutionContext | forEntity, forRegion, global, io oraz wyniki zakończenia | Właściwy wątek i kontrolowany lifecycle zadań. |

Klasa np. BanCommandDefinition nie zna DatabaseHandler, repozytorium, cache, PlayerIPManager ani teleportacji. Może budować węzły i przypisać wywołanie gateway.submit dla gotowego żądania. Walidacja reguł, retry, decyzje i efekty są poza nią. Dotyczy to także lambd executes w pliku rejestracji.

### 4.2. Rodziny komend i ich interfejsy

| Rodzina | Interfejs przypadku użycia | Żądanie / wynik | Docelowe funkcje |
| --- | --- | --- | --- |
| Nakładanie kary | IssuePunishmentUseCase | IssuePunishmentRequest → PunishmentResult | kick, jail, warn, mute, ban, banIp |
| Cofanie / zakończenie | RevokePunishmentUseCase | RevokeRequest → RevokeResult | revokeById, revokeByTargetAndType, clearActive, undoTemplateEvent |
| Szablony | ApplyTemplateUseCase | ApplyTemplateRequest → PunishmentResult / BatchResult | resolveTemplate, selectEscalationLevel, applyPlan |
| Znaczniki antycheata | MarkControlUseCase | MarkControlRequest → MarkControlResult | undoMarked, pauseAndUndo, resume |
| Odczyt kar i historii | PunishmentQueryService | TypedQuery → Page&lt;T&gt; / Detail&lt;T&gt; | check, history, banList, details, playerSummary |
| Zmiana danych kary | AmendPunishmentUseCase | AmendReasonRequest → AmendmentResult | changeReason z wersją oczekiwaną i audytem |
| Konfiguracja więzienia | JailConfigurationUseCase | SetJailRequest / SetReleaseRequest → ConfigUpdateResult | setJail, setReleasePoint, validateLocation |
| Zgłoszenia | ReportUseCase / ReportQueryService | SubmitReport / ResolveReport / ReportQuery | submit, list, view, history, resolve, reject |
| Otwarcie interfejsu | OpenModerationViewUseCase | OpenViewRequest → ViewResult | panel, GUI, dialog, książka; akcje zapisujące wracają do usług |
| Utrzymanie pluginu | AdministrationUseCase | AdminRequest → AdminTaskResult | validate, reloadConfig, diagnostics, version |
| Dane i prace długie | DataMaintenanceUseCase | DataJobRequest → JobReceipt / JobResult | export, import, migrateEngine, backup |
| Tożsamość i relacje | IdentityQueryService / IdentityMigrationUseCase | IdentityQuery / MigrationRequest | resolveIdentity, findRelations, planIdentityMerge, executeIdentityMerge |
| Informacja i zadania | HelpQueryService / OperationQueryService | HelpQuery / OperationQuery | help, status, confirm, cancelQueuedOperation |

Nie projektujemy jednego interfejsu z dziesiątkami opcjonalnych metod dla wszystkich rodzin. Każda rodzina ma kontrakt wejścia, wyników, uprawnień, atomowości i wykonania.

## 5. Jeden szablon wykonania kar

### 5.1. Główne interfejsy usług

| Kontrakt | Docelowa metoda | Reguła |
| --- | --- | --- |
| PunishmentService | issue(request), revoke(request), amendReason(request), getOperation(id) | Publiczna orkiestracja; brak zależności od interfejsu komendy. |
| PunishmentPipeline | execute(normalizedRequest) | Niezmienna kolejność etapów dla wszystkich kar. |
| PunishmentTypeHandler | capabilities, validateSpecific, buildMutationPlan, buildEffectPlan | Strategia danego typu; bez samodzielnego commitu i komunikatów. |
| TargetResolver | resolve(reference) | Nick/UUID → tożsamość; nie wymyśla UUID dla nieznanego konta. |
| IpRelationResolver | plan(reference, relationPolicy) | Kanonizacja i wyliczenie zbioru IP/kont z dowodami relacji. |
| PunishmentPolicy | authorize(actor, plan), validateDuration, validateScope, validateImmunity | Identyczne reguły dla wszystkich kanałów wejścia. |
| PunishmentUnitOfWork | decideAndCommit(plan, operationId, expectedVersions) | Stan, historia, audyt, wersje i outbox na jednym połączeniu. |
| PunishmentReadModel | findPlayerState, findIpState, publishCommitted, invalidateVersion | Jedna ścieżka dostępu do obrazów pamięciowych. |
| PlatformPunishmentPort | disconnectSession, deliverNotice, applyJail, releaseJail | Efekty na właściwym schedulerze, z potwierdzeniem wyniku. |
| OperationStore / ConfirmationService | create, transition, lookup / prepare, consume | Trwała idempotencja i jednorazowe potwierdzenia. |
| OutboxDispatcher | claimDelivery, dispatch, acknowledge, retry | Dostarczenie zdarzeń po commicie; bez ponawiania całej kary. |

Żądanie kary zawiera: kontekst, typ, TargetReference, opcjonalny czas, wymagany powód, typowane opcje, zakres i pochodzenie szablonu/znacznika. Po rozwiązaniu celu powstaje ResolvedPunishmentPlan zawierający konkretne UUID/IP, zaobserwowane wersje i plan efektów.

### 5.2. Kolejność etapów

1. Parser tworzy żądanie bez wykonywania I/O. Gateway nadaje operationId i przyjmuje lub odrzuca pracę z powodu limitów.
2. Usługi rozwiązują cel, odczytują konfigurację i cache; brakujące dane pobierają asynchronicznie.
3. Polityki walidują uprawnienia, odporność, czas, powód, zakres, znacznik i możliwości typu.
4. Operacja wymagająca zgody tworzy podgląd i token. Bez potwierdzenia nie nakłada kary.
5. Przy wykonaniu UnitOfWork ponownie weryfikuje wersje, blokadę znacznika i stan istotny dla decyzji. W jednej transakcji zapisuje karę lub grupę kar, audyt, historię, operationId, wersje oraz outbox.
6. Po commicie publikowany jest nowy obraz cache. Nie wysyłamy wcześniej komunikatu „ukarano”.
7. Adapter platformy wykonuje efekty: wyrzucenie, teleportację, tryb gry, wiadomość. Wynik efektu jest zapisany oddzielnie.
8. Dispatcher dostarcza powiadomienia sieciowe, webhooki i akcje integracyjne. Prezenter pokazuje faktyczny etap operacji.

Błąd przed commitem oznacza brak nowej kary i brak efektu. Błąd po commicie nie jest rollbackiem już zapisanej kary. Usługa zapisuje oczekujący lub nieudany efekt i przekazuje tę informację wykonawcy.

### 5.3. Specjalizacja dla każdego typu

| Typ | Cel | Czas | Stan i decyzja | Efekt oraz potwierdzenie |
| --- | --- | --- | --- | --- |
| KICK | Konkretna sesja online zidentyfikowana przez UUID i sessionId | Nieobsługiwany: wspólny model ma duration=null; podanie czasu daje TIME_NOT_SUPPORTED | Audyt i zadanie efektu; nie powstaje aktywna blokada ponownego wejścia | Rozłączenie wyłącznie tej sesji. Offline przed wykonaniem → TARGET_OFFLINE / SESSION_ENDED, nigdy wyrzucenie późniejszej sesji. |
| JAIL | Znane konto online lub offline | Dodatni czas albo bezterminowo | Aktywna kara i trwały JailSession: lokalizacja powrotu, tryb gry, id kary, stan zastosowania | Online: bezpieczny teleport i zmiana trybu po sukcesie; offline: zastosowanie po wejściu. Osobne APPLIED, PENDING_LOGIN, TELEPORT_FAILED. |
| WARN | Znane konto online lub offline | Czas ważności ostrzeżenia albo bezterminowo | Osobne ostrzeżenia sumują się; licznik i przekroczenie progu wyliczane atomowo | Powiadomienie i ewentualny plan eskalacji; każdy próg ma klucz zapobiegający podwójnemu wykonaniu. |
| MUTE | Znane konto online lub offline | Dodatni czas albo bezterminowo | Aktywne ograniczenie w obrazie stanu gracza | Egzekwowanie na czacie i skonfigurowanych kanałach/komendach bez SQL w zdarzeniu. |
| BAN | Znane konto online lub offline | Dodatni czas albo bezterminowo | Aktywna blokada UUID w podanym zakresie | Odmowa logowania i wyrzucenie obecnej sesji; zapis kary może zakończyć się przed efektem sieciowym. |
| BAN_IP | Zbiór kanonicznych IP wyliczony z IP/UUID/nicku | Dodatni czas albo bezterminowo | Jedna grupa operacji, osobne cele IP, jeden atomowy zapis całości | Odmowa logowania i rozłączenie wszystkich pasujących sesji w zakresie; receipt pokazuje liczbę IP i wyniki efektów. |

KICK jest celowym wyjątkiem semantycznym od czasu: „kick na 10 minut” byłby banem, więc nie tworzymy pozornego parametru.

Wiele niezależnych kar tego samego typu może współistnieć, np. kara ręczna i kara oznaczona przez antycheat. Ograniczenie działa, dopóki pozostaje przynajmniej jedna aktywna kara. Cofnięcie jednej nie usuwa drugiej. Dla prezentacji wybieramy karę bezterminową lub najpóźniej wygasającą, a w szczegółach pokazujemy wszystkie. WARN ma licznik aktywnych zdarzeń; KICK nie tworzy trwałego ograniczenia.

JAIL przechowuje pierwotną lokalizację i tryb z pierwszego aktywnego uwięzienia. Kolejna kara JAIL nie nadpisuje miejsca powrotu pozycją wewnątrz więzienia. Zwolnienie następuje dopiero po zakończeniu ostatniej aktywnej kary; błędna lokalizacja ma kontrolowany punkt awaryjny.

### 5.4. Szablon wykonania a szablon kar

To dwa różne pojęcia:

- PunishmentPipeline to programistyczny szablon wykonania, wspólny dla każdej kary.
- PunishmentTemplate to konfiguracja: stabilne templateId, wersja, poziomy, czas, powód i zestaw akcji.

ApplyTemplateUseCase rozwiązuje templateId i poziom, po czym przekazuje plan do tego samego pipeline. GUI i antycheat robią to samo. Nie ma drugiego applyBan w PunishCommand ani wywoływania własnych komend przez składanie tekstu.

Wybór kolejnego poziomu i zapis historii są jedną decyzją transakcyjną. Undo jest powiązane z eventId/templateId, a nie tekstem powodu lub nazwą szablonu. Wznowienie/zmiana nazwy nie przepisuje historycznych zdarzeń.

## 6. Składnia i pełny Paper Brigadier API

### 6.1. Składnia kanoniczna

| Komenda 2.0 | Postać |
| --- | --- |
| kick | /kick &lt;gracz&gt; &lt;powód&gt; [opcje] |
| jail / warn / mute / ban | /&lt;typ&gt; &lt;gracz&gt; [czas] &lt;powód&gt; [opcje] |
| banip | /banip &lt;ip\|uuid\|nick&gt; [czas] &lt;powód&gt; [opcje] |
| punish | /punish &lt;gracz&gt; &lt;szablon&gt; [poziom] [opcje] |

Powód jest wymagany. Proponowany limit to 1–1024 znaków po przycięciu; odrzucamy pusty tekst i znaki sterujące. Powód jest danymi, a nie zaufanym kodem MiniMessage.

**Powód wielowyrazowy ujmujemy w cudzysłów.** To jawna decyzja 2.0 umożliwiająca pełne, odrębne węzły flag po powodzie. Nie używamy greedyString jako powodu, po którym miałyby wystąpić dalsze argumenty. Pojedynczy powód może być bez cudzysłowu.

Przykłady:

- /kick Steve "Obraźliwe zachowanie" --silent
- /warn Steve 7d "Spam na czacie" --mark grim.spam
- /mute Steve 30m "Powtarzający się spam" --silent --scope survival
- /ban Steve "Poważne naruszenie zasad" --scope network
- /jail Steve 2h "Niszczenie cudzej budowli" --jail central
- /banip Steve 7d "Obchodzenie blokady" --related-depth 1
- /banip 550e8400-e29b-41d4-a716-446655440000 7d "Obchodzenie blokady"
- /banip 203.0.113.10 1d "Nadużycie z tego adresu"
- /banip "2001:db8::10" 1d "Nadużycie z tego adresu"
- /warn Steve "7d" — bezterminowe ostrzeżenie z dosłownym powodem 7d.

Czas: uporządkowany zapis np. 30s, 10m, 2h, 7d, 1d2h30m. Jednostki s/m/h/d/w; bez miesięcy i lat o niejednoznacznej długości. Zero, wartości ujemne, przepełnienie i nieprawidłowa jednostka są błędem. Pominięcie czasu oznacza bezterminowo, z wyjątkiem KICK.

Niezacytowany pierwszy token wyglądający jak czas jest zarezerwowany dla czasu. Błędne 10x daje INVALID_DURATION, nie staje się po cichu powodem. Jeśli powód sam wygląda jak czas, trzeba go zacytować. Parser musi rozróżniać obie gałęzie również dla liczb na początku powodu.

### 6.2. Argumenty i konstrukcja drzewa

| Argument / element | Implementacja planowana | Zakres walidacji |
| --- | --- | --- |
| player | TargetReferenceArgument, natywny string | Nick/UUID, dokładnie jeden cel; bez selektorów masowych. Rozwiązanie konta poza parserem. |
| ip-or-player | IpOrIdentityArgument, natywny string | Literal IPv4/IPv6, UUID lub nick; bez DNS, portów i CIDR. IPv6 w cudzysłowie przy bazowym string(). |
| duration | DurationArgument | Czyste parsowanie i zakres techniczny; limit uprawnień w usłudze. |
| reason | ReasonArgument oparty o string() | Słowo lub cytowany tekst, escape i limit długości. |
| template / jail / mark / scope | Własne typy z katalogiem podpowiedzi | Składnia synchroniczna; istnienie i prawa potwierdza usługa. |
| id / page / level | Typy liczbowe z dolnym limitem | Dodatnie wartości; wersja zasobu dla zmian. |
| opcje | Typowane literały i argumenty potomne | Dozwolone flagi dla danego CommandDefinition, bez ponownego split args. |

Rejestracja przez LifecycleEvents.COMMANDS i Commands.literal / Commands.argument; przekazujemy LiteralCommandNode&lt;CommandSourceStack&gt;. W 2.0 na Paper nie używamy BasicCommand, Bukkit CommandExecutor ani refleksji do command map jako alternatywy.

Drzewo ma oddzielne gałęzie z czasem i bez czasu. DurationArgument i ReasonArgument mają spójne reguły rozstrzygania niejednoznaczności. Za każdym powodem generator buduje dozwolone poddrzewa opcji. Zbiór dostępnych flag jest skończony; generator usuwa już użyte flagi z kolejnych węzłów. Wszystkie dopuszczone kolejności muszą mieć testy; nie zastępujemy ich końcowym greedyString.

Przykładowa gałąź: ban → target → duration → reason → --mark → mark → --silent. Druga gałąź pomija duration. Węzeł może kończyć komendę, jeśli pozostałe opcje są nieobowiązkowe.

Podpowiedzi są kontekstowe, filtrowane uprawnieniami i oparte na cache. Nie wykonują blokującego SQL ani pobierania profilu. CustomArgumentType zwraca obiekt wartości, nie Player wymagającego niebezpiecznego użycia w async. Klient otrzymuje obsługiwany typ natywny; autorytatywna walidacja domenowa pozostaje po stronie serwera.

Alias rejestrujemy dla tego samego drzewa i przypadku użycia. Walidujemy kolizje nazw, namespace i aliasów podczas startu. Pomoc, usage i dokumentację generujemy z CommandDefinition.

### 6.3. Rejestr flag

| Opcja | Parametr | Działanie / uprawnienie |
| --- | --- | --- |
| --silent | Brak | Wycisza powiadomienia celu i broadcast; nie wyłącza ekranu rozłączenia, audytu i odpowiedzi wykonawcy. Osobne punisherx.flag.silent. |
| --force | Brak | Omija wyłącznie określoną odporność celu. Osobne punisherx.flag.force; nie omija praw do komendy, potwierdzeń, błędów DB i blokady znacznika. |
| --mark | Opcjonalny mark; brak → default | Pochodzenie antycheata i możliwość zbiorczego cofania; punisherx.flag.mark. |
| --scope | Wymagany zakres | Konkretny serverId lub network. Prawo do wskazanego zakresu, osobne prawo network. |
| --jail | Wymagane jailId | Tylko JAIL; bez flagi skonfigurowane więzienie domyślne. |
| --related-depth | Wymagana liczba 0–3 | Tylko BAN_IP; domyślnie 1; rozszerzenie ponad 1 wymaga osobnego prawa. |

MarkArgument odrzuca tokeny zaczynające się od --. Dzięki temu --mark --silent oznacza mark=default oraz silent, a --mark grim.speed --silent ma wartość znacznika. Nazwy rezerwujące kolizje nie są dozwolone. Proponowany zapis 2.0 używa osobnych tokenów; forma 1.x --mark=wartość nie musi być utrzymywana.

Nieznana flaga, duplikat, brak wymaganego parametru i flaga niewłaściwa dla typu to błąd składni. Opcje są na końcu; nie wycinamy tokenów z dowolnego miejsca powodu.

## 7. BAN_IP: rozwiązywanie i zakres relacji

### 7.1. Dane, które rzeczywiście możemy ustalić

Plugin zna wyłącznie obserwacje połączeń, które zapisał, oraz dane dostarczone przez zaufaną integrację. Nie odkryje nieznanego IP na podstawie samego UUID i nie odtworzy dawnej historii z Mojanga.

Obserwacja zawiera UUID, kanoniczny klucz IP, firstSeen, lastSeen, źródło i poziom zaufania. Bierzemy faktyczny adres klienta ze zweryfikowanego mechanizmu forwarding, nie dowolne pole przekazane przez klienta ani IP proxy bez rozróżnienia. Retencja obserwacji jest konfigurowalna i jawna w podglądzie.

UUID jest tożsamością, nick tylko indeksem wyszukiwania. Zmiana nicku nie rozdziela historii IP tego samego UUID. Przy niejednoznacznej dawnej nazwie wymagamy wskazania UUID. Nie konstruujemy offline UUID jako zastępstwa za nieudane wyszukanie.

### 7.2. Deterministyczny algorytm

Proponowany domyślny tryb to **konto oraz bezpośrednie powiązania**, depth=1:

1. Nick/UUID: ustal zbiór kont startowych U0 i wszystkie zachowane IP tych kont I0.
2. Literal IP: I0 zawiera podany adres, a U0 wszystkie znane konta zaobserwowane na tym adresie. Literal bez historii nadal jest poprawnym samodzielnym celem.
3. Dla każdej rundy rozszerzenia znajdź konta współdzielące IP aktualnego zbioru, a następnie ich znane adresy. Przy depth=1 wykonaj dokładnie jedną taką rundę.
4. depth=0 dla nicku/UUID obejmuje własną historię IP konta; dla literalnego IP tylko ten adres. depth=2 lub 3 rozszerza wynik o dalsze rundy, nigdy bez ograniczeń.
5. Zdeduplikuj kanoniczne adresy. Zachowaj dla każdego adresu ścieżkę dowodową: konto, adres łączący, źródło i daty.
6. Zwróć niezmienny BanIpPlan z fingerprintem zbioru, wersjami grafu i listą dotkniętych kont/sesji. Nie wykonuj jeszcze bana.

Zatem zarówno IP, UUID, jak i nick prowadzą do tego samego modelu relacji. Wspólny IP wskazuje powiązanie techniczne, nie dowód, że konta należą do jednej osoby.

Proponowane początkowe limity jednego planu: 64 IP, 256 kont, depth maksymalnie 3, limit czasu wyszukiwania 3 s. Przekroczenie zwraca PLAN_TOO_LARGE lub RESOLUTION_TIMEOUT; nie nakładamy po cichu kary na ucięty zbiór.

### 7.3. Podgląd i potwierdzenie

BAN_IP zawsze przechodzi przez podgląd: liczba adresów i kont, własne IP konta, powiązane IP, daty, zakres oraz potencjalne współdzielenie NAT/VPN. Surowe IP widoczne wyłącznie z punisherx.data.ip; bez tego pokazujemy maskowanie i liczby. Podpowiedzi nie ujawniają adresów bez tego prawa.

ConfirmationService wydaje losowy token ważny proponowane 60 s, przypisany do actorId, operationId, fingerprintu, zakresu, czasu i powodu. Potwierdzenie: /prx confirm &lt;token&gt;. Token jest jednorazowy i zużywany transakcyjnie. --force nie zastępuje potwierdzenia.

Przed commitem ponownie sprawdzamy uprawnienia i wersje relacji. Zmieniony zbiór oznacza PLAN_CHANGED oraz nowy podgląd; nigdy automatycznie nie dokładamy kolejnych adresów do potwierdzonego planu. Atomowy snapshot grafu wymaga wersjonowania obserwacji i protokołu blokad wspólnego z ich zapisem; sam hash wykonany w pamięci nie wystarczy.

Wszystkie IP planu zapisujemy w jednej transakcji i przypisujemy do jednego punishmentGroupId. Błąd przy adresie N wycofuje cały nowy zapis. Efekty na wielu sesjach mogą wykonać się w różnym czasie; wynik oddziela atomowy zapis od częściowo zakończonych rozłączeń.

BAN_IP nie tworzy automatycznie BAN na UUID. Obejmuje również inne konta łączące się z zablokowanego IP, co podgląd musi jasno pokazywać. Nowe, wcześniej nieznane IP nie są automatycznie dopisywane do zakończonej grupy.

### 7.4. Normalizacja adresów

- IPv4: dokładnie cztery oktety 0–255, jedna reprezentacja; nie używamy samego regexu cyfr.
- IPv6: zapis binarny/kanoniczny; warianty skrótu i wielkość liter nie tworzą różnych kluczy.
- IPv4-mapped IPv6 mapujemy do tego samego klucza IPv4.
- Bez rozwiązywania DNS; odrzucamy host:port, CIDR i zone-id, jeżeli nie zostaną osobno zaprojektowane.
- Główne klucze wyszukiwawcze muszą pozwalać na indeksowany lookup; ewentualne szyfrowanie używa zarządzanego klucza i stabilnego klucza wyszukiwawczego, nie pełnego odszyfrowania tabeli przy komendzie.

## 8. Cache Caffeine 3.3.1 i gorące ścieżki

**DB jest źródłem trwałej prawdy; cache jest podstawowym modelem odczytu w runtime.** To nie sprzeczność: komenda zapisująca podejmuje trwałą decyzję w transakcji, a zdarzenia gry czytają gotowy stan z pamięci.

| Cache / obraz | Klucz → wartość | Polityka |
| --- | --- | --- |
| ResidentPlayerState | UUID → niezmienny stan aktywnych kar i revision | Caffeine bez wygaszania i eksmisji aktywnej sesji; usuwanie po wyjściu i zakończeniu trwających odczytów. Rozmiar ogranicza liczba przyjętych sesji. |
| ActivePlayerLookup | UUID + scope → VersionedPlayerState | AsyncLoadingCache dla kont offline; limit wielkości, TTL i krótkie cache pustego wyniku. |
| ActiveIpLookup | canonicalIp + scope → VersionedIpState | AsyncLoadingCache; ładowanie w fazie logowania, odświeżenie i inwalidacja po commicie. |
| IdentityLookup | nick/UUID → wynik identyfikacji | Limity, negatywny cache z krótkim TTL, czyszczenie po obserwacji zmiany nicku. |
| IpRelationsLookup | UUID/IP + policyVersion → fragment relacji | Ograniczone zbiory; wersje obserwacji unieważniają wynik. Podgląd nie zastępuje rewalidacji transakcyjnej. |
| TemplateCatalog | templateId → niezmienna definicja | Atomowa wymiana całej wersji po poprawnej walidacji konfiguracji. |
| QueryCache | filtr + zakres + uprawnienia → strona odczytu | Krótki TTL dla panelu; bez współdzielenia widoku IP z osobą bez praw. |

Proponowane ustawienia startowe cache offline: do 50 000 kont, 50 000 IP i 20 000 indeksów tożsamości; TTL pozytywny do 30 s, negatywny do 2 s. To parametry do pomiarów, nie obietnica zużycia RAM. Zbiory relacji wymagają również limitu wagi; liczba wpisów nie ogranicza rozmiaru pojedynczego grafu.

Zasady poprawności:

1. Cache miss to UNKNOWN, nie „brak kary”. Awaria DB nie jest zapisywana jako pusty wynik.
2. Wygaśnięcie wpisu Caffeine nie oznacza wygaśnięcia kary. expiresAt należy do modelu domenowego.
3. Bezterminowa kara nigdy nie znika logicznie z powodu TTL lub maximumSize.
4. Publikujemy wyłącznie stan po commicie. Ładowanie starego stanu nie może nadpisać nowszej revision.
5. Load i mutation dla tego samego agregatu przechodzą przez koordynator; spóźniony load z niższą wersją jest odrzucany. Nie wystarcza samo invalidate.
6. Local single-flight ogranicza równoległe pobieranie tej samej wartości. Executor loadera jest jawny i ograniczony.
7. Czasy expiresAt sprawdzamy przy odczycie; task wygaszający jedynie utrwala porządek i zdarzenia.
8. Dla graczy online resident snapshot musi być gotowy przed dopuszczeniem do normalnej gry.
9. Czat, preprocess komend i ruch nie wykonują SQL, plików, HTTP ani oczekiwania na Future.
10. Brak resident snapshot w zdarzeniu daje bezpieczny stan ograniczony i uruchamia odtworzenie; nie próbujemy zmieniać zakończonego eventu z callbacku async.

Nie przenosimy jail_cache.json jako drugiego trwałego źródła. Lokalizacja powrotu, tryb gry i postęp efektu JAIL są w DB przez SyntaxDB. Cache przechowuje ich projekcję.

## 9. Async, schedulery i lifecycle

„Async” dotyczy kosztownego I/O i pracy poza wątkiem gry. Nie oznacza dowolnego dostępu do obiektów Bukkit z puli roboczej.

| Rodzaj pracy | Wykonanie |
| --- | --- |
| JDBC / transakcje / migracje / pliki eksportu | Zarządzany dispatcher I/O z ograniczoną równoległością i kolejką. |
| Rozwiązanie profilu wymagające sieci / GeoIP / webhook | Osobny ograniczony executor I/O, timeout i limity. |
| Reguły domenowe na niezmiennych danych | W bieżącym kontekście roboczym; bez zbędnego przełączania wątku. |
| Player, teleport, tryb gry, GUI | Scheduler encji; operacje miejsca na schedulerze regionu. |
| Czynności globalne platformy | Global scheduler, jeśli API rzeczywiście wymaga tego kontekstu. |
| Sugestie i odczyty zdarzeń gry | Natychmiastowe snapshoty pamięciowe; bez blokowania. |

Wewnętrznie proponowane są suspend use cases i strukturalna współbieżność. Publiczne API JVM może oferować CompletionStage, ale jest adapterem tych samych usług. Nie mieszamy niezarządzanych GlobalScope, commonPool i zagnieżdżonych fire-and-forget.

SyntaxDB korzysta z synchronicznego JDBC. Jego fasada coroutine używa dispatchera dostarczonego przez aplikację. Blok transakcji pozostaje synchroniczny, na jednym połączeniu i wątku; w jego wnętrzu nie ma suspend, teleportacji, HTTP, przełączania dispatchera ani wywołania innego db zamiast transakcyjnego klienta.

Ograniczamy liczbę równoległych prac do realnej pojemności puli DB. Pełna kolejka daje BUSY, a nie uruchomienie JDBC na wątku wywołującym. Każde zadanie ma termin, operationId i wynik.

W fazie logowania korzystamy z API pozwalającego bezpiecznie poczekać na rozstrzygnięcie poza wątkiem gry. Jeśli wybrane zdarzenie nie obsługuje odroczenia, bounded wait może dotyczyć wyłącznie przeznaczonego do tego wątku async logowania. Nie zakładamy, że każdy event może zwrócić Future.

Start: walidacja konfiguracji → start SyntaxDB → kontrolowane migracje przez uprawnionego właściciela → przygotowanie read model → READY. Do tego momentu nowe operacje zapisujące są odrzucane, a logowanie podlega jawnej polityce dostępności.

Stop: wstrzymanie przyjęć → zakończenie lub zapis statusu prac → zatrzymanie dispatcherów dostarczeń → zamknięcie DB po zakończeniu użycia → zamknięcie executorów. Twarde przerwanie po commicie jest rozpoznawane przez operationId i wznowienie.

Reload 2.0 oznacza walidację i atomową wymianę konfiguracji, nie wyłączenie i ponowne uruchomienie całego pluginu. Zmiana silnika DB, kluczowych bibliotek lub schematu wymaga osobnej pracy utrzymaniowej i kontrolowanego restartu.

## 10. Atomowe decyzje i współbieżność

Caffeine i synchronized nie zapewniają atomowości pomiędzy cache, DB, kilkoma IP i kilkoma serwerami.

### 10.1. Gwarancje lokalne i bazodanowe

- Agregat gracza/IP ma revision. Lokalny koordynator porządkuje prace na tych samych kluczach.
- DB jest końcowym arbitrem: blokady rekordów lub compare-and-set wersji, ograniczenia unikalności i transakcje.
- Blokady zasobów obejmują UUID/IP niezależnie od scope; kara network i lokalna nie mogą ominąć wspólnej reguły przez użycie różnych kluczy.
- Wielocelowe plany blokują klucze w stałej kolejności, z ograniczonym retry deadlocków.
- PostgreSQL/MySQL/SQL Server mają osobno zweryfikowaną strategię blokad; SQLite krótko serializuje zapis. Jeden przenośny SELECT nie jest dowodem tych samych gwarancji.
- Sprawdzenie pauzy mark, wybór poziomu szablonu, zapis kary i oznaczenie progu WARN należą do jednej transakcji lub wspólnego protokołu blokad.
- pauseAndUndo konkuruje z nakładaniem kary na tym samym kluczu znacznika i zakresu: po zatwierdzeniu pauzy nowe kary tego checku nie przechodzą.
- Cofanie i wygaszanie są idempotentnymi przejściami. Usunięcie jednej z wielu aktywnych kar nie czyści całej projekcji typu.

### 10.2. Idempotencja i niepewny wynik

Każda operacja ma unikalne operationId/idempotencyKey i hash żądania. Ta sama wartość z inną treścią jest błędem. Ponowienie tej samej pracy zwraca zapisany wynik; świadomie ponownie wpisana komenda jest nową operacją.

Timeout lub anulowanie coroutine nie dowodzi rollbacku JDBC. Przy utracie odpowiedzi po commicie zwracamy UNKNOWN/PENDING_RECONCILIATION i odczytujemy stan po operationId. Nie składamy automatycznie nowej kary z nowym identyfikatorem.

Jedna transakcja zapisuje jednocześnie rekord operacji, karę/grupę, historię, audyt, wersje i outbox. Nie ma okna „kara jest, ale historii nie ma” w ramach nowej operacji.

### 10.3. Granica atomowości

DB może zapewnić jednorazową decyzję i zapis. DB oraz rozłączenie gracza, teleport lub webhook nie tworzą jednej transakcji ACID.

Dostarczenia są co najmniej jednokrotne, z deduplikacją tam, gdzie odbiorca ją obsługuje. Dla efektu sesji używamy operationId + effectId + sessionId; przed zastosowaniem sprawdzamy aktualny stan i revision. Zewnętrzna komenda innego pluginu lub webhook bez klucza idempotencji może powtórzyć się po awarii. Takiego wyniku nie opisujemy jako exactly-once.

KICK jest związany z sesją i nigdy nie czeka na przyszłe wejście. JAIL jest stanem docelowym, więc może być odtworzony po restarcie. Operacja z bazą już zatwierdzoną i nieskuteczną teleportacją zwraca zapisano karę, efekt oczekuje/nie powiódł się, a nie pełny sukces.

## 11. Potwierdzenia i statusy

### 11.1. Status operacji a wynik efektu

| Stan operacji | Znaczenie dla wykonawcy |
| --- | --- |
| ACCEPTED | Żądanie przyjęte do obsługi; jeszcze bez gwarancji zapisu. |
| AWAITING_CONFIRMATION | Gotowy podgląd, brak nałożonej kary. |
| REJECTED / FAILED_PRECOMMIT | Walidacja lub zapis nie powiódł się; brak nowego efektu. |
| COMMITTED | Kara/grupa i audyt trwale zapisane. |
| APPLYING | Trwa aktualizacja lokalnej projekcji i wykonanie efektów. |
| COMPLETED | Wymagane dla danej operacji efekty zakończone; receipt podaje zakres. |
| COMMITTED_WITH_PENDING_EFFECTS | Zapisano, lecz część efektów czeka, jest offline lub wymaga retry. |
| UNKNOWN / PENDING_RECONCILIATION | Niepewny wynik I/O; trzeba sprawdzić operationId. |
| CANCELLED | Anulowano przed rozpoczęciem trwałej decyzji. |

Efekt ma osobny status: PENDING, APPLIED, PENDING_LOGIN, SKIPPED_SESSION_ENDED, FAILED_RETRYABLE albo FAILED_FINAL. Nie łączymy automatycznie APPLIED_LOCAL z zakończeniem synchronizacji wszystkich serwerów.

PunishmentReceipt zawiera operationId, groupId, punishmentIds, zakres, cele/liczby celów, committedAt, appliedAt, revision, stan dostarczeń i bezpieczny kod błędu. Typowy komunikat: „Zapisano bana; rozłączenie na proxy oczekuje” jest poprawnym wynikiem.

Brigadier zwraca synchroniczny int. Dla pracy async oznacza on przyjęcie zlecenia, a nie dowód nałożenia kary. Funkcje/datapacki wymagające ostatecznego wyniku muszą korzystać z osobnego mechanizmu zakończenia lub statusu; sam sukces drzewa tego nie zapewnia.

Proponowane narzędzia: /prx status &lt;operationId&gt;, /prx confirm &lt;token&gt;, /prx cancel &lt;operationId&gt;. Anulowanie po commicie jest niedozwolone; cofnięcie kary to nowa audytowana operacja.

### 11.2. Potwierdzenia operacji szerokich

Potwierdzenia wymagają BAN_IP, masowe cofanie, kick all, undo wszystkich kar znacznika, import nadpisujący dane i zmiana silnika DB. Zwykłe pojedyncze warn/mute/ban nie wymagają dodatkowego pytania.

Nie używamy „wpisz ponownie taką samą komendę” bez identyfikatora planu. Token wiąże konkretny podgląd, wykonawcę i wersje danych. Dialog GUI jest tylko innym interfejsem tego samego kontraktu potwierdzenia.

## 12. SyntaxDB jako jedyny adapter baz danych

### 12.1. Dystrybucja i silniki

Konsument deklaruje publiczny artefakt pl.syntaxdevteam:syntaxdb, zgodnie z kontraktem tej biblioteki. Nie składa sam core, HikariCP i sterowników. Przed implementacją zamrażamy konkretną dostępną wersję artefaktu; obecny numer SNAPSHOT nie dowodzi, że publikacja zawiera dokładnie kod wskazanego commitu.

Wybieramy jeden model dostarczenia: pakowanie zależności lub kontrolowane ładowanie przez loader. Nie mieszamy dwóch kopii SyntaxDB/Hikari/driverów w classloaderach. Weryfikacja dotyczy finalnego JAR i startu na czystej instalacji.

| Silnik | Decyzja planu 2.0 | Uwagi |
| --- | --- | --- |
| SQLite | Podstawowy backend lokalny | Jeden writer, obsłużone busy/timeout i spójny backup. |
| MySQL | Backend sieciowy | Transakcje na InnoDB, utf8mb4, testy blokad. |
| MariaDB | Obsługa przez ścieżkę MYSQL w SyntaxDB | Jawny alias na granicy konfiguracji PunisherX; testy na MariaDB, nie tylko MySQL. |
| PostgreSQL | Backend sieciowy | Parametryzacja, migracje, blokady i paginacja przez adapter. |
| Microsoft SQL Server | Backend sieciowy | Dedykowane testy dialect, paginacji i blokad. |
| H2 | Poza zakresem | Brak w wybranym kontrakcie SyntaxDB; bez emulatora zgodności. |
| DuckDB | Poza magazynem operacyjnym 2.0 | W kodzie SyntaxDB występuje DUCKDB, ale driver jest opcjonalny i nie należy do domyślnego bundle. Ewentualna analityka to osobny temat. |

Źródła SyntaxDB sprawdzono bezpośrednio: README, Database, DatabaseType, CoroutineDatabase i root build.gradle.kts. README opisuje cztery podstawowe silniki; kod zawiera dodatkowo opt-in DuckDB. Nie utożsamiamy listy enum z przetestowanym wsparciem PunisherX.

### 12.2. Repozytoria i UnitOfWork

Porty: PunishmentRepository, IdentityRepository, IpObservationRepository, JailSessionRepository, TemplateProgressRepository, MarkRepository, ReportRepository, OperationRepository i OutboxRepository.

Usługa aplikacyjna nie otrzymuje Row, Connection ani SQL. Adapter mapuje dane na typy domenowe. Złożony zapis korzysta ze wspólnego UnitOfWork i transakcyjnego klienta SyntaxDB; wywołanie kilku niezależnych metod db poza tą transakcją nie jest dopuszczalne.

Parametry wartości zawsze wiążemy; dynamiczne nazwy tabel/kolumn pochodzą tylko ze statycznego modelu. Różnice dialect pozostają w storage-syntaxdb. Nie rozrzucamy warunków DatabaseType po komendach i listenerach.

### 12.3. Proponowany schemat 2.0

Prefiks tabel: px2_. Nazwy są propozycją; implementacja ma utrzymać opisane relacje i ograniczenia.

| Tabela | Najważniejsze dane i ograniczenia |
| --- | --- |
| players | UUID PK, bieżący nick i revision. |
| player_names | UUID, znormalizowany nick, przedział obserwacji; indeks nicku do rozstrzygania nazw. |
| ip_addresses | Kanoniczny unikalny ipKey, rodzina adresu, reprezentacja do prezentacji lub dane chronione. |
| player_ip_observations | UUID + ipKey + source, firstSeen, lastSeen, revision; indeksy UUID→IP i IP→UUID. |
| relation_revisions | Wersje zasobów grafu/protokół snapshotu współdzielony z zapisem obserwacji. |
| operations | operationId PK, unikalny idempotencyKey, requestHash, actor, status i wynik. |
| punishment_groups | groupId, operationId, rodzaj planu, fingerprint celu i scope. |
| punishments | punishmentId, groupId, targetKind, playerUuid lub ipKey, type, startsAt, expiresAt nullable, status, revision, templateId/mark. |
| punishment_events | Niezmienny dziennik ISSUE/REVOKE/EXPIRE/AMEND/UNDO z eventId, actorId, operationId i czasem. |
| target_revisions | targetKey PK, revision oraz punkt serializacji decyzji na celu niezależnie od scope. |
| jail_sessions | UUID, odniesienie do aktywnej sesji więzienia, pierwotna pozycja/tryb, stan zastosowania i zwolnienia. |
| template_progress | UUID + templateId + scope, wersja; decyzje o poziomie powiązane z konkretnymi zdarzeniami. |
| mark_controls | mark + scope, pause state i revision; wspólna blokada dla issue/pause/undo. |
| reports / report_events | Zgłoszenie, reporter/suspect, stan, audyt rozpatrzenia; atomowe ograniczenie otwartych zgłoszeń. |
| outbox / deliveries | eventId, aggregateKey, revision, payload i stan per consumerId; unikalność eventId+consumerId. |
| confirmations | tokenHash, actorId, planFingerprint, wersje, expiresAt, consumedAt. |

expiresAt=null oznacza bezterminowo, bez magicznego -1 w nowym API. UUID i IP są różnymi typami celu i polami, bez przeciążania kolumny uuid adresem. Wszystkie czasy zapisujemy jako UTC z jednoznaczną rozdzielczością; domena korzysta z Instant/Duration i wstrzykiwanego Clock.

Indeksy egzekwowania: target + scope + type + status, expiresAt; historia: target + czas + id; outbox: odbiorca/status i retryAt. Dla stronicowania historii preferujemy stabilny cursor po czasie i id.

Trwały audyt każdej decyzji nie jest wyłączany przez --silent ani ustawienie historii GUI. Retencja audytu, IP i dostarczeń ma osobne polityki; czyszczenie nie może osierocić aktywnych kar ani niedostarczonych zdarzeń.

### 12.4. Migracje, backup i przejście z 1.x

Nowa instalacja używa osobnego schematu/prefiksu 2.0. Istniejące tabele 1.x nie są zmieniane przy starcie nowej wersji. Wykrycie nieoczekiwanego magazynu kończy się jawnym komunikatem, bez samoczynnego wyboru pustej bazy.

Migracje wewnątrz 2.x są wersjonowane i kontrolowane przez SyntaxDB. Prawo DDL ma uprawniony migrator/backend, nie bridge proxy tylko do odczytu. W sieci migracje wykonuje jeden koordynator; pozostałe procesy czekają na oczekiwaną wersję.

Import z 1.x nie jest warunkiem architektury. Jeśli zostanie zamówiony, powstaje osobny, jawny importer z podglądem mapowania, raportem odrzuceń i kopią źródła.

Backup jest spójnym snapshotem lub eksportem transakcyjnym. Nie kopiujemy aktywnego pliku SQLite jako zwykłego pliku z pominięciem WAL. SyntaxDB jest drogą odczytu/zapisu danych, a mechanizm backupu musi spełniać wymagania konkretnego silnika.

## 13. Sieć, spójność i awarie

### 13.1. Synchronizacja

Zmiana stanu i zdarzenie outbox powstają razem. Każdy backend i proxy ma consumerId, lastAppliedRevision per agregat oraz stan odbioru. Nie ma jednego processed dla całej sieci.

Konto proxy może mieć SELECT-only do współdzielonej bazy. Wtedy nie tworzy tabel i nie aktualizuje deliveries; własne potwierdzenia/cursory zapisuje w lokalnym SQLite przez SyntaxDB lub zgłasza autoryzowanemu właścicielowi zapisu. To decyzja implementacyjna etapu sieci, nie ukryty wymóg nadania proxy UPDATE.

Odbiorca odrzuca duplikaty i starsze wersje. Luka wersji wymaga przeładowania agregatu. Nie można bezwarunkowo używać rosnącego ID outbox jako kolejności commitów: transakcje mogą zatwierdzić się odwrotnie. Protokół musi bazować na jawnym stanie dostarczeń albo na uporządkowanym strumieniu zapewnionym przez koordynatora; samo WHERE id &gt; lastId jest niewystarczające.

Po restarcie odbiorca pobiera snapshot i wznawia zdarzenia z ustalonej bariery. Retencja outbox wymaga potwierdzeń lub jawnego wymuszenia nowego snapshotu dla nieaktywnego odbiorcy.

### 13.2. Realne gwarancje

Decyzje zapisujące w DB są atomowe; cache na wielu procesach ma spójność opóźnioną. Nie obiecujemy globalnej liniowości odczytów z niezależnych cache.

Proponowany cel pomiarowy: propagacja zdrowej sieci do 1 s, z metryką wieku ostatniej synchronizacji i osobnym statusem DISTRIBUTION_PENDING. COMMITTED nie oznacza, że każdy serwer odebrał zmianę.

Jeżeli wymagamy braku okna dla nowego bana na logowaniu, punkt dopuszczający gracza musi potwierdzić świeżą wersję w autorytatywnym magazynie/koordynatorze. Sam TTL cache tego nie zapewnia. Ten odczyt odbywa się w fazie logowania poza wątkiem gry.

### 13.3. Polityka niedostępności

| Sytuacja | Zachowanie docelowe |
| --- | --- |
| DB niedostępna przed zapisem | Brak nowej kary; błąd dla wykonawcy. Bez awaryjnego bana w drugiej bazie platformy. |
| Utrata odpowiedzi na commit | Ustalenie wyniku po operationId; bez ślepego ponowienia. |
| Znana aktywna kara w cache podczas awarii DB | Nadal egzekwować do expiresAt; nie usuwać przez upływ TTL cache. |
| Logowanie bez wiarygodnie świeżego stanu | Domyślnie tymczasowo odmówić z komunikatem niedostępności. |
| Online bez resident state | Ograniczyć czynność wymagającą decyzji, odtworzyć stan async, zgłosić metrykę. |
| Błąd webhooka | Kara pozostaje; retry samego dostarczenia. |
| Nieudany teleport JAIL | Zapisany stan i jawny błąd/oczekujący efekt; kontrolowany retry. |
| Proxy nie odpowiada | Lokalny wynik i pending dla sieci; brak komunikatu o pełnym zakończeniu. |
| Błędna nowa konfiguracja | Zachować poprzedni niezmienny snapshot konfiguracji. |

## 14. Pozostałe komendy i ich docelowe zachowanie

| Komendy / funkcja z 1.x | Plan 2.0 |
| --- | --- |
| unban, unmute, unwarn, unjail | RevokePunishmentUseCase z jawnym typem i zakresem. Preferowany id kary; po celu podgląd jednoznacznego zbioru. |
| unban dla IP | Nie łączyć automatycznie cofnięcia BAN_UUID i BAN_IP. Osobna gałąź /unban ip &lt;ip&gt; lub cofnięcie całej grupy po groupId. |
| clearall | Jeden plan grupowego cofania i potwierdzenie; audyt zostaje. |
| change-reason | AmendReasonRequest z expectedRevision; zdarzenie zmiany, bez przepisywania dawnego audytu. |
| unpunish | Undo konkretnego template event; cofnięcie wpływu na eskalację bez kasowania historii. |
| unmark / pause / resume | Osobne operacje MarkControlUseCase; network pause ma pierwszeństwo nad lokalnym resume. |
| check, history, banlist | QueryService, typowane filtry, paginacja; brak SQL w rendererze i brak uprawnień ukrytych wyłącznie w GUI. |
| setjail, setunjail | Snapshot pozycji na schedulerze encji, walidacja regionu, zapis konfiguracji async; brak „Player w tle”. |
| report, reports | Jeden moduł zgłoszeń; limit otwartych i rozpatrzenie rozwiązane atomowo, nie SELECT→INSERT bez kontroli wyścigu. |
| panel, dialogi, książki | Adaptery prezentacji; każda akcja zapisująca wraca do tego samego use case. |
| prx validate, reload | Walidacja bez modyfikacji plików, potem atomowa wymiana konfiguracji; bez odtwarzania instancji pluginu. |
| prx diag/version/help | Dedykowane serwisy informacyjne, diagnostyka bez sekretów i bez pełnych IP dla nieuprawnionego. |
| export/import/migrate/backup | Długie zadania z postępem, jobId, potwierdzeniem planu i raportem końcowym. |
| cache | Zastąpić /prx cache stats i kontrolowaną invalidacją; nie udostępniać pełnej historii IP jako debug dump. |
| langfix | Nie przenosić automatycznie do nowego pluginu; brak wymogu starych placeholderów. |
| migracja UUID / AuthGatewayX | Wersjonowane API planowania/przeniesienia tożsamości; osobne od zmiany silnika DB, z rewalidacją i audytem. |
| kick all | Osobny plan masowy, snapshot sessionId, limity, podgląd i wynik dla każdego celu; nie specjalny nick w parserze player. |

Własne akcje z konfiguracji dla innych pluginów pozostają integracją zewnętrzną. Ich wykonanie jest po commicie, kontrolowane i audytowane. Własne kary PunisherX uruchamiamy typowanym API, z dziedziczeniem origin/mark/silent, limitem głębokości i correlationId zapobiegającym pętli eskalacji.

## 15. Uprawnienia, API i obserwowalność

- Drzewo Brigadiera ukrywa niedozwolone gałęzie, ale usługa sprawdza uprawnienia również dla GUI, API i integracji.
- ActorContext ma wersję/czas snapshotu uprawnień. Potwierdzenie wymaga ponownego sprawdzenia; po odebraniu uprawnień token nie jest sposobem obejścia.
- Odporność celu sprawdzamy według jawnej polityki także dla offline; brak informacji nie oznacza automatycznie braku odporności. Integracja z providerem uprawnień musi określić sposób uzyskania wiarygodnego snapshotu.
- Konsola i SYSTEM mają jawny zestaw capabilities; integracja nie może dowolnie podszyć się pod SYSTEM.
- Zakresy server/network i odczyt IP mają oddzielne prawa; --force nie rozszerza zakresu.
- Publiczne API otrzymuje wersję 2, typowane UUID/IP i wyniki bez Bukkit. Dotychczasowe API nie jest ograniczeniem zgodności.
- Publiczne zdarzenia rozróżniają Prepared/Committed/EffectApplied/Revoked. Odbiorcy wiedzą, w jakim kontekście wątku są wywoływani.
- Zdarzenie przed decyzją może odmówić w określonym terminie; zdarzenie po commicie jest informacją i nie może „anulować” transakcji.
- Metryki: czas kolejki/DB/całej operacji, cache hit/miss/load failure, wiek projekcji, outbox lag, retry, konflikty wersji, timeout, wielkość planu BAN_IP.
- Logi zawierają operationId, correlationId, eventId, etap i bezpieczny kod błędu. Sekrety połączenia i pełne IP nie trafiają do zwykłych logów debug.
- Dane powodu nie są interpolowane jako wykonywalna komenda ani zaufane znaczniki formatowania.

## 16. Kolejność prac i kryteria zakończenia

Wszystkie etapy implementacyjne pozostają **TODO**.

| Etap | Zakres | Warunek zakończenia |
| --- | --- | --- |
| 0 — decyzje i kontrakty | Zamrożenie zakresu platform, minimalnego API/Javy, semantyki flag i IP; lista przypadków użycia; ADR-y | Uzgodnione kontrakty oraz macierz wymagań, bez niejawnych zachowań odziedziczonych z 1.x. |
| 1 — fundamenty | Domain/application/api, typy celu/czasu/wyniku, katalog błędów, ActorContext | Rdzeń testowalny bez serwera Minecraft i bez DB. |
| 2 — SyntaxDB | Nowy schemat, repozytoria, UnitOfWork, idempotencja, migracje, outbox | Testy atomowości i restartu na wszystkich deklarowanych silnikach. |
| 3 — cache i runtime | Read model, schedulery, lifecycle, wersje, kolejki i przeciążenie | Brak I/O w gorących zdarzeniach; poprawność przy miss, starej wersji i awarii. |
| 4 — pipeline kar | KICK, WARN, MUTE, BAN, JAIL; wspólne polityki i efekty | Wszystkie typy korzystają z tego samego pipeline i mają prawdziwe receipt. |
| 5 — Brigadier | Pełne drzewa wszystkich rodzin, typy, flagi, uprawnienia, aliasy | Brak BasicCommand i brak logiki domenowej w definicjach komend Paper. |
| 6 — BAN_IP | Obserwacje, graf, podgląd, confirmation, zapis grupy | IPv4/IPv6/UUID/nick, kontrola zakresu, zero częściowego commitu. |
| 7 — szablony i cofanie | Eskalacja, progi WARN, mark pause/undo, amend/revoke | Brak duplikacji logiki i brak cofnięcia niezależnej kary. |
| 8 — GUI, zgłoszenia, API | Dialogi/panel/query/report/integracje | Zapis z każdego interfejsu przechodzi przez identyczną autoryzację i usługi. |
| 9 — sieć i wydanie | Synchronizacja, snapshoty, odbiorcy, dokumentacja PL/EN, CI i testy serwerowe | Udokumentowane gwarancje opóźnień, brak fałszywego sukcesu i gotowy scenariusz nowej instalacji. |

Pipeline i kontrakty projektujemy przed finalną budową wszystkich drzew. Etykieta 2.0 w dokumentacji nie podnosi jeszcze wersji artefaktu ani nie uruchamia wydania.

### 16.1. Macierz weryfikacji

| Obszar | Scenariusze konieczne przed wydaniem |
| --- | --- |
| Parser | Każdy typ z czasem/bez, cytowany powód, escape, powód podobny do czasu, błędny czas, IPv6, brak/duplikat flagi, wszystkie dozwolone kolejności. |
| Rozdział warstw | Definicje komend bez repozytoriów, schedulerów efektów i reguł kary; GUI i API używają wspólnych usług. |
| Uprawnienia | Player/console/integration, odebranie praw przed confirm, offline immunity, oddzielne IP/network/force/silent. |
| Atomowość | Dwa równoległe WARN, wybór poziomu szablonu, issue vs pause, revoke vs expire, BAN_IP z błędem na N-tym IP. |
| Idempotencja | Ten sam klucz dwa razy, inny payload pod tym samym kluczem, commit bez odpowiedzi, restart podczas efektu. |
| Cache | Miss nie daje ALLOW, stary load po nowym commicie, eksmisja/TTL, kilka niezależnych kar, resident state na wejściu i wyjściu. |
| IP | IPv4, IPv6, mapped IPv4, wiele nicków UUID, współdzielony NAT, brak historii, limity grafu, zmiana grafu po podglądzie. |
| Sesje | Wyjście gracza przed kick, nowe wejście z tym samym UUID, teleport między regionami, retired entity, uszkodzony punkt powrotu. |
| DB | SQLite, MySQL, MariaDB, PostgreSQL, SQL Server: rollback, blokady, ograniczenia, migracje, Unicode, backup/restore. |
| Sieć | Duplikaty, zdarzenia poza kolejnością, dwa odbiorniki, commit order różny od ID, utrata łączności, snapshot po restarcie. |
| Awarie | DB przed/po commicie, pełna kolejka, timeout, zamknięcie pluginu, webhook, błąd cache publication, nieudana teleportacja. |
| Wydajność | Zero SQL na ruch/czat/preprocess; czas obsługi z pamięci i propagacji zmierzony dla określonej liczby graczy/kar. |

Testy integracyjne muszą używać rzeczywistych deklarowanych silników, nie wyłącznie mocków i SQLite jako zastępstwa. Sposób ich uruchamiania w CI wybieramy zgodnie z infrastrukturą; Docker nie jest wymaganiem architektury.

## 17. Decyzje wymagające zamrożenia przed implementacją

Wymagania z sekcji 1 pozostają wiążące. Poniższe wartości są konkretną rekomendacją tego planu, do zatwierdzenia przy etapie 0:

| Temat | Rekomendacja |
| --- | --- |
| Priorytet platformy | Paper/Folia jako pierwszy pełny adapter; Spigot/Bungee osobno według zakresu produktu. |
| Minimalna platforma / Java | Jedna aktualnie wybrana i zweryfikowana linia API; brak odtwarzania dawnych fallbacków. |
| Wielowyrazowy powód | Obowiązkowy cudzysłów, aby flagi pozostały pełnymi węzłami Brigadiera. |
| Czas KICK | Jawnie niedozwolony; brak pozornego czasu blokady. |
| BAN_IP | Domyślnie depth=1, zawsze podgląd; maks. 64 IP / 256 kont / depth 3. |
| Confirmation | Token 60 s, actor-bound, jednorazowy; --force nie zastępuje zgody. |
| Niedostępność na logowaniu | Tymczasowa odmowa przy nieznanym/nieświeżym stanie. |
| Aktywne kary tego samego typu | Niezależne rekordy; ograniczenie trwa do zakończenia wszystkich pasujących kar. |
| Sieć | Trwały outbox per odbiorca; jednoznaczna granica COMMITTED i APPLIED. |
| Dane 1.x | Bez automatycznej migracji; opcjonalny osobny importer tylko na osobne zlecenie. |

## 18. Źródła techniczne i granice analizy

Dokument opiera się na kodzie wskazanych commitów i oficjalnych źródłach sprawdzonych 30.10.2026:

- [Paper Command API — wprowadzenie](https://docs.papermc.io/paper/dev/command-api/basics/introduction/).
- [Paper — rejestracja i lifecycle komend](https://docs.papermc.io/paper/dev/command-api/basics/registration/).
- [Paper — argumenty i ograniczenia greedyString](https://docs.papermc.io/paper/dev/command-api/basics/arguments-and-literals/).
- [Paper — CustomArgumentType](https://docs.papermc.io/paper/dev/command-api/basics/custom-arguments/).
- [Paper/Folia — schedulery i własność pracy](https://docs.papermc.io/paper/dev/folia-support/).
- [Caffeine — wydanie 3.3.1](https://github.com/ben-manes/caffeine/releases/tag/v3.3.1).
- [Caffeine — cache manualny i asynchroniczny](https://github.com/ben-manes/caffeine/wiki/Population).
- SyntaxDB: README, root build.gradle.kts, Database.kt, DatabaseType.kt i CoroutineDatabase.kt w wskazanym commicie repozytorium projektu.

Przegląd kodu nie dowodzi poprawności obecnej implementacji na wszystkich platformach ani dostępności opublikowanego SNAPSHOT. W tym etapie nie uruchamiano buildów i testów wykonawczych, ponieważ zmiana obejmuje wyłącznie dokumentację. Testy opisane powyżej są kryteriami przyszłej implementacji.
