# Velocity runtime compatibility test

This test starts the built bridge inside an official Velocity proxy with an isolated
MariaDB database. It does not access production proxy configuration or databases.
The probe plugin is compiled separately and is never packaged into the bridge JAR.

Requirements: Linux, Python 3, JDK 25+, `mariadb-install-db`, `mariadbd`, `mariadb`,
`mariadb-admin`, and permission to open localhost TCP sockets.

From the repository root:

```bash
./gradlew -Ppunisherx.build.modules=velocity-bridge :punisherx-velocity-bridge:build
python3 punisherx-velocity-bridge/src/runtimeTest/python/run_velocity.py \
  --proxy-jar /path/to/velocity-4.3.0-44.jar \
  --bridge-jar punisherx-velocity-bridge/build/libs/PunisherX-Velocity-Bridge-1.1.3-R0.1-SNAPSHOT.jar \
  --java-home /path/to/jdk-25
```

Use the current bridge artifact version if it changes. Obtain Velocity from
[PaperMC](https://papermc.io/downloads/velocity). The tested official 4.3.0 build 44
has SHA-256 `85be73b90f295acd8d5d45a00ad10bac791d4b72b7554172612a89565ebaaa8f`.

Logs are retained under `punisherx-velocity-bridge/build/runtime-test/velocity-*/`.
Database files are created in a temporary directory and removed after shutdown.
Each run uses a new database, proxy directory and local ports. The proxy uses
offline mode so the wire test can authenticate without Mojang credentials.

## What runs

The real proxy loads the bridge through its plugin descriptor and Guice injection,
initializes HikariCP/MySQL Connector and creates `bridge_events`. Its real event
dispatcher runs checks for:

- Permanent and temporary network BANs denying `LoginEvent`.
- Active network BANs denying `ServerPreConnectEvent` and disconnecting the player.
- Expired and backend-local BANs allowing proxy login.
- Backend BAN and BANIP messages invoking disconnection.
- Client-originated bridge messages being handled without executing a BAN.
- Truncated messages leaving players connected.
- The real scheduler polling SQL events, disconnecting the target, marking events
  processed and not replaying them.

For these eleven checks, player discovery is temporarily replaced with a delegating
`ProxyServer` and synthetic `Player`/`ServerConnection` objects. SQL, the production
bridge instance, event dispatcher and scheduler remain real. Discovery is restored
before the network checks.

Two actual Minecraft 1.8 protocol TCP login attempts then verify the returned BAN
disconnect packet and reason. The probe also checks `getSessionId()` continuity
from `PreLoginEvent` to `LoginEvent`, and a new session ID on reconnect. Finally,
the runner checks a graceful proxy and connection pool shutdown, exit code zero
and absence of ERROR entries in the proxy log.

This does not run a full Minecraft backend, online-mode authentication or a game
client. Backend transfers and plugin messages are tested through real Velocity
event dispatch, rather than a live backend network connection.

## Verified 2026-10-25

Velocity **4.3.0 build 44**, Amazon Corretto **25**, local MariaDB:
all eleven dispatcher/SQL checks, both TCP login denials, both session checks and
graceful shutdown passed without ERROR entries.

Before the event-result fix, the same runtime probe failed all three checks that
required login/transfer denial: disconnection was invoked, but the event result
remained allowed. The bridge now denies login with `ComponentResult.denied(...)`
and transfer with `ServerResult.denied()` before disconnecting.
