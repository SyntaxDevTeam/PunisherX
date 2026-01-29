# PunisherX Velocity Bridge

PunisherX Velocity Bridge is the missing link between PunisherX for Paper and your Velocity 3.4 network. Connected to the same database where BAN/BANIP events are stored, it automatically listens for new entries in the `bridge_events` table and instantly disconnects matching players from the proxy — even if the ban was issued on an empty server or directly from the console.

In practice, this means you are no longer limited by the requirement that the banned player must be on the same server where the punishment is executed.

Configuration is limited to setting up the MySQL connection (host, port, database, username, password) and the polling interval.

## Configuration

* `bridge.properties` contains the parameters: host/port/database/username/password — configure them exactly the same as in `database.sql.*` on the servers running PunisherX, so both ends point to the same database and the same `bridge_events` table.
* `poll-interval-ms` controls how often the proxy polls the queue; default is 1000 ms, minimum is 200 ms.

## How It Works

1. During a BAN/BANIP, PunisherX writes an event entry to the `bridge_events` table (on the Paper server side), regardless of whether the player is online.
2. On startup, the Velocity bridge creates the table (if it does not exist) and starts a scheduled task that, every `poll-interval-ms`, fetches all unprocessed records and immediately disconnects matching players.
3. After processing, the entry is marked as `processed=1`, so subsequent polls do not trigger another kick.

Result: even a command executed from the console on an empty server is written to the shared database, and the proxy independently polls the queue and disconnects the player if they are online on another backend server.
