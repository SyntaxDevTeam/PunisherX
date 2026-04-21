# PunisherX API

## Overview
The PunisherX API exposes asynchronous methods for checking a player’s punishment history, active punishments, and ban lists.
All methods return `CompletableFuture` results, making them easy to use in async-friendly server code.

## Adding PunisherX to Your Project
### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven {
        name = "syntaxdevteam-releases"
        url = uri("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
    }
}
dependencies {
    compileOnly("pl.syntaxdevteam.punisher:PunisherX:1.6.0") // use latest release
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

## Getting the API
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

## Core Methods
- `getLastTenPunishmentHistory(uuid: String)` – last ten punishments from history
- `getLastTenActivePunishments(uuid: String)` – last ten active punishments
- `getActivePunishments(uuid: String, type: String? = "ALL")` – active punishments, optionally filtered by type
- `getPunishmentHistory(uuid: String, type: String? = "ALL")` – history, optionally filtered by type
- `getBannedPlayers(limit: Int, offset: Int)` – paged list of current bans
- `getHistoryBannedPlayers(limit: Int, offset: Int)` – paged list of historical bans

### Example: Last Ten Active Punishments
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

## Punishment Types
PunisherX recognizes the following types:
- `KICK`
- `WARN`
- `MUTE`
- `JAIL`
- `BAN`
- `BANIP`

## Response Format
Each method returns a list of `PunishmentData` objects:
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
`start` and `end` are Unix timestamps in milliseconds.

## Additional Methods
- `getPunishmentHistory(uuid: String, type: String? = "ALL")`
- `getBannedPlayers(limit: Int, offset: Int)`
- `getHistoryBannedPlayers(limit: Int, offset: Int)`

Use these to create custom punishment dashboards or integrate PunisherX data into other systems.

**Happy coding!**