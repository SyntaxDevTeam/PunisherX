# Strict player identity resolution

Inspired by d0ddn's cross-server player lookup proposal in [PR #98](https://github.com/SyntaxDevTeam/PunisherX/pull/98).

## Administrator-visible behavior

In Paper and Spigot, nickname commands resolve current local sessions only. For a player who is offline or only known to another server, use the full canonical UUID. The plugin never resolves account identity from punishment history, cached historical names, IP addresses, or a generated offline UUID. Unknown, conflicting and unavailable identities stop before account operations. The existing UUID-returning API now throws IdentityNotResolvedException for unresolved inputs; integrations can inspect resolvePlayerIdentity instead.

This deliberately does not add a remote nickname authority: a shared observation registry is not evidence of a nickname's current owner. No TTL can make that guarantee. Cross-server offline moderation uses explicit UUIDs. GUI template actions retain the UUID already selected by the moderator.

## Realm and authority

identity.realm defaults to online only for a direct online server. Offline and trusted-proxy modes require an explicit realm and fail startup if it is missing. Set an explicit lowercase realm identifier for an established network namespace. identity.authority=auto follows online-mode. online requires online-mode; offline requires offline-mode. trusted-proxy is an explicit administrator assertion that forwarding is authenticated and the backend cannot be reached by untrusted clients. The plugin does not discover or verify proxy/firewall configuration. Offline mode describes a separate server namespace, not proof of a real online account.

One punishment database is bound to one realm and online/offline namespace in player_identity_domain. The first start binds existing data to the administrator's declared domain; audit that choice and back up before rollout. A different domain fails startup before consumers are registered. Existing mixed-domain data cannot be repaired by guessing names; separate/audit it manually first. Proxy and direct online servers may share the same online realm only when the administrator deliberately configures that trust.

## Registry and lifecycle

player_identities is keyed by realm and UUID, with canonical/normalized observed name, authority, timestamp and version. Join observations are captured on the server thread and written asynchronously with compare-and-set version checks; old or tied timestamps cannot overwrite a later observation. Quit removes the local name mapping. Reload replaces the local directory; old queued writes are discarded. Shared observations never resolve offline nicknames and are not imported from punishment history. No account data is automatically reassigned on nickname changes. Alias history is not used for resolution.

Account and IP targets are separate types. BANIP for a player uses address history indexed by the resolved UUID, never by a historical nickname. A literal IP does not select a player UUID from another cache row. Literal IPv4/IPv6 addresses are parsed without DNS and normalized. On startup, existing active BANIP targets are normalized in a transaction; historical punishment rows and account UUIDs remain intact. Pending BANIP bridge events are normalized with active targets. Legacy BANIP rows without explicit player UUID metadata cannot contribute to an account ladder or be undone by nickname; undo their exact IP event instead. Encrypted cache addresses are normalized when read and newly captured addresses when written. Imported active IP punishments are normalized too.

The schema widens name columns to 64 characters so explicit UUID command targets fit. Backups include domain and registry metadata. Import into a bound database requires the same domain; legacy dumps without domain metadata are rejected. Verify legacy data and restore in an isolated environment before adopting the domain; do not relabel backups from another namespace. No automatic player identity migration is performed.

Audit output uses the existing debug logger and includes operator, input, chosen UUID, realm and source. Treat database credentials and registry write access as part of the trust boundary. A plugin/database writer inside that boundary can tamper with records; this mechanism cannot protect against arbitrary server code execution.

## Validation

Unit/integration tests cover rename/reassignment, duplicate sessions, explicit UUIDs, offline/online/proxy policies, domain conflicts, delayed registry updates, IP normalization, backup-domain checks and fail-closed account commands. Live proxy authentication, Paper/Spigot runtime behavior and real non-SQLite database engines require deployment validation; environment-gated database tests may skip when no disposable service is configured.
