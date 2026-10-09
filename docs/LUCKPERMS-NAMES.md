# Nazwy z LuckPerms

Paper i Spigot udostępniają dodatkowe placeholdery w komunikatach PunisherX, gdy wiadomość zawiera kontekst gracza lub operatora:

| Placeholder | Wartość |
| --- | --- |
| `<operator_display>` | Prefix, pokolorowany nick i suffix administratora wykonującego akcję |
| `<player_display>` | Prefix, pokolorowany nick i suffix gracza |
| `<operator_prefix>`, `<player_prefix>` | Sam prefix |
| `<operator_suffix>`, `<player_suffix>` | Sam suffix |

`<operator>` i `<player>` zachowują zwykłe nazwy. Odczyt odbywa się przez API LuckPerms; PlaceholderAPI nie jest wymagane. Wartości są pobierane dla osoby wskazanej w komunikacie, niezależnie od jego odbiorcy. Zmiana prefixu w LuckPerms wpływa na kolejne renderowane wiadomości.

Przykład w `messages_pl.yml`:

```yaml
mute:
  broadcast: "<player_display> <gray>został wyciszony przez</gray> <operator_display><gray>. Powód: <reason></gray>"
```

Prefix i suffix przyjmują formatowanie MiniMessage oraz legacy (`&`, `§`, `&#RRGGBB`, `§x…`). Spację po prefixie należy uwzględnić w jego wartości w LuckPerms. Bez osobnego koloru nick dziedziczy końcowy kolor prefixu.

Układ obu nazw można ustawić niezależnie w `config.yml`:

```yaml
name-formatting:
  operator-display: "$prefix $name $suffix"
  player-display: "$name <gray>($prefix)</gray>"
  name-color-meta: name-color
```

Tokeny: `$prefix`, `$name`, `$suffix`. `$player` i `$operator` są aliasami `$name` — zawsze oznaczają osobę, której nazwę formatuje dany szablon. Można zmieniać kolejność, pomijać elementy i dodawać tekst oraz formatowanie MiniMessage lub legacy. Kolor z metadanych jest stosowany w miejscu nicku. Tokeny wstawione z prefixu, suffixu lub nicku nie są ponownie rozwijane. `$$` oznacza dosłowny znak dolara.

Odstępy są zachowywane dokładnie. Domyślne `$prefix$name$suffix` zachowuje odstępy z LuckPerms. Jeśli dodasz spacje w szablonie, usuń zbędne spacje z wartości prefixu/suffixu, aby ich nie dublować. Bez danych LuckPerms prefix i suffix są puste, a szablon nadal jest stosowany. Migracja konfiguracji do wersji 168 dodaje domyślne szablony.

Opcjonalny klucz metadanych dla koloru nicku:

```yaml
name-formatting:
  name-color-meta: name-color
```

Przykładowe ustawienie w LuckPerms:

```text
/lp group admin meta set name-color "#ff5555"
```

Wartością może być też `red`, `&c` lub `<red>`. Sam LuckPerms nie określa jednego uniwersalnego klucza koloru nicku, dlatego jego nazwę można zmienić w konfiguracji.

Bez LuckPerms lub bez dostępnych danych użytkownika nowe placeholdery wyświetlają zwykły nick, a prefix i suffix są puste. Plugin używa wyłącznie już załadowanych danych LuckPerms: nie blokuje obsługi wiadomości oczekiwaniem na dane gracza offline. W komunikatach historii jest używany dostępny aktualny prefix, nie historyczny prefix z chwili nadania kary. Konsola zwykle nie ma danych LuckPerms i wyświetla swoją zwykłą nazwę.

Po wymianie JAR uruchom ponownie serwer. Sprawdź wiadomość po akcji wykonanej przez gracza z prefixem oraz z konsoli, kolor legacy i hex, brak LuckPerms i zmianę prefixu bez restartu.

## MessageHandler i budowanie

Parsowanie prefixów, suffixów, szablonów oraz serializacja komponentów korzystają z API MessageHandler `1.3.0-R0.3-SNAPSHOT`. `NameFormatting` rozwija tylko tokeny układu; dane `NameMetadata` są w osobnym pliku. Treści raportów korzystają z literalnych placeholderów MessageHandlera.

Zmiany API są w lokalnym projekcie `../MessageHandler`. Do sprawdzenia obu projektów przed publikacją zależności:

```bash
# W projekcie MessageHandler:
./gradlew :MessageHandler-Paper:test :MessageHandler-Spigot:test :MessageHandler-Paper:publishToMavenLocal :MessageHandler-Spigot:publishToMavenLocal

# W projekcie PunisherX:
./gradlew :punisherx-paper:test :punisherx-spigot:test :punisherx-paper:shadowJar :punisherx-spigot:shadowJar -Ppunisherx.messagehandler.local=true
```

Przed wdrożeniem PunisherX na serwerze należy opublikować moduły MessageHandler R0.3 do repozytorium snapshotów, z którego pobiera je loader pluginu. Lokalna publikacja Maven służy kompilacji i testom; nie udostępnia zależności serwerom. Publikację zdalną należy wykonać osobno.
