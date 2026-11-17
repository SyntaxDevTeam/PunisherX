# PunisherX BungeeCord Bridge

PunisherX BungeeCord Bridge enforces bans across connected BungeeCord servers from a shared SQL database. Requires PunisherX installed on the Paper/Folia or Spigot backends.

## Security update (critical)

Older BungeeCord bridge versions accepted `BAN` and `BANIP` commands from the `punisherx:bridge` client plugin messaging channel without sender verification. Update **the proxy and backends together**. The hardened bridge consumes legacy messages but never executes their payloads.

Only **committed, active, network-scoped** `BAN` and `BANIP` records in the `punishments` table can disconnect players. Expired or withdrawn penalties no longer trigger stale queue events; each proxy checks its own connections independently. This also supports deployments with multiple BungeeCord instances.

## Configuration

Configure `plugins/PunisherXBridge/bridge.properties` (the actual folder follows the bridge plugin descriptor):

```properties
host=localhost
port=3306
database=punisherx
username=punisherx_proxy
password=replace_with_your_read_only_password
poll-interval-ms=1000
```

Set `poll-interval-ms` to at least 200 ms. The default is 1000 ms. New logins use asynchronous SQL verification; existing players are reconciled at this interval, so there may be up to approximately one interval before an already-connected player is removed. Backend transfers use the latest asynchronous snapshot and never block the proxy's connection event loop on JDBC.

The proxy SQL user requires only `SELECT` on `punishments`, not SQL write or schema-creation privileges. Configure a distinct limited account; do not use MySQL `root` or backend database credentials.

**Database errors deny new logins and transfers until access can be verified.** Existing sessions are not automatically disconnected solely because of an SQL outage. The bridge periodically retries SQL connectivity, including after startup failures.

To create the account (from a privileged SQL administrator session; adapt the host and credentials):

```sql
CREATE USER 'punisherx_proxy'@'proxy-host' IDENTIFIED BY 'strong-private-password';
GRANT SELECT ON punisherx.punishments TO 'punisherx_proxy'@'proxy-host';
```

Restart the proxy after changing `bridge.properties`.

## Building and testing

From the repository root:

```bash
./gradlew :punisherx-bungee-bridge:build
```

Security regression tests cover current BAN and BANIP records, backend-local and expired records, removal, multi-proxy independent reads, batching and SQL errors.

See the [project changelog](../CHANGELOG.md) for the critical security update.
