# MessageHandler i budowanie

Parsowanie prefixów, suffixów, szablonów oraz serializacja komponentów korzystają z API MessageHandler `1.3.0-R0.3-SNAPSHOT`. `NameFormatting` rozwija tylko tokeny układu; dane `NameMetadata` są w osobnym pliku. Treści raportów korzystają z literalnych placeholderów MessageHandlera.

Zmiany API są w lokalnym projekcie `../MessageHandler`. Do sprawdzenia obu projektów przed publikacją zależności:

```bash
# W projekcie MessageHandler:
./gradlew :MessageHandler-Paper:test :MessageHandler-Spigot:test :MessageHandler-Paper:publishToMavenLocal :MessageHandler-Spigot:publishToMavenLocal

# W projekcie PunisherX:
./gradlew :punisherx-paper:test :punisherx-spigot:test :punisherx-paper:shadowJar :punisherx-spigot:shadowJar -Ppunisherx.messagehandler.local=true
```

Przed wdrożeniem PunisherX na serwerze należy opublikować moduły MessageHandler R0.3 do repozytorium snapshotów, z którego pobiera je loader pluginu. Lokalna publikacja Maven służy kompilacji i testom; nie udostępnia zależności serwerom. Publikację zdalną należy wykonać osobno.
