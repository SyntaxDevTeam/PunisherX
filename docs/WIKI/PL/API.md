# API PunisherX

## Przegląd
API PunisherX udostępnia asynchroniczne metody do sprawdzania historii kar gracza, aktywnych kar oraz list banów.
Wszystkie metody zwracają `CompletableFuture`, dzięki czemu dobrze współpracują z kodem serwerowym opartym o async.

## Dodanie PunisherX do projektu
### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven {
        name = "syntaxdevteam-releases"
        url = uri("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
    }
}
dependencies {
    compileOnly("pl.syntaxdevteam.punisher:PunisherX:1.6.0") // użyj najnowszego wydania
}
```

### Maven
```xml
<repository>
  <id>syntaxdevteam-releases</id>
  <url>https://nexus.syntaxdevteam.pl/repository/maven-releases/</url>
</repository>

<dependency>
  <groupId>pl.syntaxdevteam.punisher</groupId>
  <artifactId>PunisherX</artifactId>
  <version>1.6.0</version>
  <scope>provided</scope>
</dependency>
```

## Pobranie API
#### Kotlin
```kotlin
override fun onEnable() {
    val api = server.servicesManager.load(PunisherXApi::class.java)
        ?: run {
            logger.severe("PunisherX API is not available – disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

    api.getLastTenPunishmentHistory(player.uniqueId.toString())
        .thenAccept { list ->
            list.forEach {
                println("Punishment: ${it.type}, Reason: ${it.reason}, Operator: ${it.operator}")
            }
        }
}
```
#### JAVA
```java
@Override
public void onEnable() {
    PunisherXApi api = getServer().getServicesManager().load(PunisherXApi.class);
    if (api == null) {
        getLogger().severe("PunisherX API is not available – disabling plugin.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }

    api.getLastTenPunishmentHistory(player.getUniqueId().toString())
        .thenAccept(list -> {
            for (PunishmentData data : list) {
                System.out.println("Punishment: " + data.getType()
                    + ", Reason: " + data.getReason()
                    + ", Operator: " + data.getOperator());
            }
        });
}
```

## Główne metody
- `getLastTenPunishmentHistory(uuid: String)` – ostatnie dziesięć kar z historii
- `getLastTenActivePunishments(uuid: String)` – ostatnie dziesięć aktywnych kar
- `getActivePunishments(uuid: String, type: String? = "ALL")` – aktywne kary, opcjonalnie filtrowane po typie
- `getPunishmentHistory(uuid: String, type: String? = "ALL")` – historia kar, opcjonalnie filtrowana po typie
- `getBannedPlayers(limit: Int, offset: Int)` – stronicowana lista aktualnych banów
- `getHistoryBannedPlayers(limit: Int, offset: Int)` – stronicowana lista historycznych banów

### Przykład: ostatnie dziesięć aktywnych kar
#### Kotlin
```kotlin
api.getLastTenActivePunishments(player.uniqueId.toString())
    .thenAccept { list ->
        list.forEach {
            println("Active Punishment: ${it.type}, Reason: ${it.reason}, Ends: ${it.end}")
        }
    }
```
#### Java
```java
api.getLastTenActivePunishments(player.getUniqueId().toString())
    .thenAccept(list -> {
        for (PunishmentData data : list) {
            System.out.println("Active Punishment: " + data.getType()
                + ", Reason: " + data.getReason()
                + ", Ends: " + data.getEnd());
        }
    });
```

## Typy kar
PunisherX rozpoznaje następujące typy:
- `KICK`
- `WARN`
- `MUTE`
- `JAIL`
- `BAN`
- `BANIP`

## Format odpowiedzi
Każda metoda zwraca listę obiektów `PunishmentData`:
```json
{
  "id": 1,
  "uuid": "b4d3c2e1-1234-5678-90ab-cdef12345678",
  "type": "BAN",
  "reason": "Hacking",
  "start": 1742484600000,
  "end": 1745086600000,
  "name": "Notch",
  "operator": "Admin123"
}
```
`start` i `end` to znaczniki czasu Unix w milisekundach.

## Dodatkowe metody
- `getPunishmentHistory(uuid: String, type: String? = "ALL")`
- `getBannedPlayers(limit: Int, offset: Int)`
- `getHistoryBannedPlayers(limit: Int, offset: Int)`

Możesz ich użyć do budowy własnych dashboardów kar lub integracji danych PunisherX z innymi systemami.

**Miłego kodowania!**
