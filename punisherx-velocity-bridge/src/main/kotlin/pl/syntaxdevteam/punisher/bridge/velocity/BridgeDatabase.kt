package pl.syntaxdevteam.punisher.bridge.velocity

import org.slf4j.Logger
import pl.syntaxdevteam.syntaxdb.Database
import java.util.UUID

data class ActiveBan(val reason: String, val endTime: Long)

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

    fun ensureSchema() {
        try {
            database.start()
            database.execute("""CREATE TABLE IF NOT EXISTS bridge_events (
                id INT AUTO_INCREMENT PRIMARY KEY, action VARCHAR(16) NOT NULL,
                target VARCHAR(64) NOT NULL, reason TEXT CHARACTER SET utf8mb4, endTime BIGINT NOT NULL,
                processed TINYINT DEFAULT 0, processedAt BIGINT
            ) ENGINE=InnoDB""".trimIndent())
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

    fun findActiveBan(uuid: UUID, now: Long = System.currentTimeMillis()): ActiveBan? = runCatching {
        database.query("""SELECT reason, endTime FROM punishments
            WHERE uuid = ? AND punishmentType = 'BAN' AND server = 'network'
            AND (endTime = -1 OR endTime > ?) ORDER BY start DESC LIMIT 1""".trimIndent(), listOf(uuid.toString(), now))
            .firstOrNull()?.let { ActiveBan(it.string("reason").orEmpty(), it.long("endTime") ?: 0L) }
    }.getOrElse { error -> logger.error("Failed to read active BAN for $uuid", error); null }
}
