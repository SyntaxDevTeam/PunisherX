package pl.syntaxdevteam.punisher.bridge.velocity

import org.slf4j.Logger
import pl.syntaxdevteam.syntaxdb.Database
import java.util.UUID

data class ActiveBan(val reason: String, val endTime: Long, val type: String = "BAN")

data class ConnectionIdentity(val uuid: UUID, val ip: String?)

class BridgeDatabase internal constructor(private val logger: Logger, private val database: Database) {
    constructor(logger: Logger, config: BridgeConfig) : this(logger, Database.create("mysql") {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password
        property("characterEncoding", "UTF-8")
        pool {
            maximumPoolSize = 4; minimumIdle = 1
            connectionTimeoutMs = 5_000; validationTimeoutMs = 2_000
            idleTimeoutMs = 120_000; maxLifetimeMs = 300_000
        }
    })

    /** Proxy database accounts require SELECT only; migrations belong to the backend. */
    fun ensureSchema() {
        try {
            database.start()
            database.query("SELECT 1", emptyList())
        } catch (error: Exception) {
            runCatching { database.close() }.onFailure { error.addSuppressed(it) }
            throw error
        }
    }

    fun close() = database.close()

    fun fetchPendingEvents(limit: Int = 128): List<BridgeEvent> = runCatching {
        database.select("bridge_events").columns("id", "action", "target", "reason", "endTime")
            .where { "processed" eq 0 }.orderBy("id").limit(limit).fetch().map { row ->
                BridgeEvent(row.long("id") ?: 0L, row.string("action").orEmpty(), row.string("target").orEmpty(),
                    row.string("reason").orEmpty(), row.long("endTime") ?: 0L)
            }
    }.getOrElse { error -> logger.error("Failed to read bridge_events", error); emptyList() }

    fun markProcessed(id: Long) {
        runCatching {
            database.update("bridge_events", mapOf("processed" to 1, "processedAt" to System.currentTimeMillis())) { "id" eq id }
        }.onFailure { error -> logger.error("Failed to mark bridge event $id as processed", error) }
    }

    /** SQL is authoritative: malformed, expired, local-scope and revoked events cannot kick players. 
     * Read failures propagate so callers deny new sessions/transfers instead of failing open.
     */
    fun findActiveBan(uuid: UUID, now: Long = System.currentTimeMillis(), ip: String? = null): ActiveBan? =
        findActiveBans(listOf(ConnectionIdentity(uuid, ip)), now)[uuid]

    /** One bounded SQL read per 128 sessions, independent of any shared processed queue flag. */
    fun findActiveBans(
        identities: Collection<ConnectionIdentity>,
        now: Long = System.currentTimeMillis()
    ): Map<UUID, ActiveBan> {
        if (identities.isEmpty()) return emptyMap()
        val bans = mutableMapOf<UUID, ActiveBan>()

        identities.distinctBy { it.uuid }.chunked(128).forEach { batch ->
            val uuids = batch.map { it.uuid.toString() }
            val ips = batch.mapNotNull { it.ip?.takeIf(String::isNotBlank) }.distinct()
            fun placeholders(size: Int) = List(size) { "?" }.joinToString(",")
            val ipSql = if (ips.isEmpty()) "" else
                " OR (punishmentType = 'BANIP' AND uuid IN (${placeholders(ips.size)}))"
            val sql = """SELECT uuid, punishmentType, reason, endTime FROM punishments
                WHERE server = 'network' AND start <= ? AND (endTime = -1 OR endTime > ?)
                AND ((punishmentType = 'BAN' AND uuid IN (${placeholders(uuids.size)}))$ipSql)
                ORDER BY start DESC""".trimIndent()
            val matches = database.query(sql, listOf(now, now) + uuids + ips)
            val byUuid = mutableMapOf<String, ActiveBan>()
            val byIp = mutableMapOf<String, ActiveBan>()
            matches.forEach { row ->
                val target = row.string("uuid") ?: return@forEach
                val type = row.string("punishmentType") ?: return@forEach
                val end = row.long("endTime") ?: return@forEach
                if (end != -1L && end <= now) return@forEach
                val ban = ActiveBan(row.string("reason").orEmpty(), end, type)
                when (type) {
                    "BAN" -> byUuid.putIfAbsent(target, ban)
                    "BANIP" -> byIp.putIfAbsent(target, ban)
                }
            }
            batch.forEach { identity ->
                val ban = byUuid[identity.uuid.toString()]
                    ?: identity.ip?.let(byIp::get)
                if (ban != null) bans[identity.uuid] = ban
            }
        }
        return bans
    }
}
