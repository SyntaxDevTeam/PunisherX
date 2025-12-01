package pl.syntaxdevteam.punisher.bridge.velocity

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.Logger
import java.sql.ResultSet

class BridgeDatabase(
    private val logger: Logger,
    private val config: BridgeConfig
) {
    private val dataSource: HikariDataSource by lazy { HikariDataSource(buildHikariConfig()) }

    fun ensureSchema() {
        runCatching {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE IF NOT EXISTS bridge_events (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            action VARCHAR(16) NOT NULL,
                            target VARCHAR(64) NOT NULL,
                            reason TEXT,
                            endTime BIGINT NOT NULL,
                            processed TINYINT DEFAULT 0,
                            processedAt BIGINT
                        )
                        """.trimIndent()
                    )
                }
            }
        }.onFailure { ex ->
            logger.error("Failed to ensure bridge_events schema", ex)
        }
    }

    fun close() {
        runCatching { dataSource.close() }
    }

    fun fetchPendingEvents(limit: Int = 128): List<BridgeEvent> {
        return runCatching {
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    """
                    SELECT id, action, target, reason, endTime
                    FROM bridge_events
                    WHERE processed = 0
                    ORDER BY id ASC
                    LIMIT ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setInt(1, limit)
                    statement.executeQuery().use { resultSet ->
                        generateSequence { if (resultSet.next()) resultSet else null }
                            .map(::mapEvent)
                            .toList()
                    }
                }
            }
        }.getOrElse { ex ->
            logger.error("Failed to read bridge_events", ex)
            emptyList()
        }
    }

    fun markProcessed(id: Long) {
        runCatching {
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    "UPDATE bridge_events SET processed = 1, processedAt = ? WHERE id = ?"
                ).use { statement ->
                    statement.setLong(1, System.currentTimeMillis())
                    statement.setLong(2, id)
                    statement.executeUpdate()
                }
            }
        }.onFailure { ex ->
            logger.error("Failed to mark bridge event $id as processed", ex)
        }
    }

    private fun mapEvent(resultSet: ResultSet): BridgeEvent {
        return BridgeEvent(
            id = resultSet.getLong("id"),
            action = resultSet.getString("action"),
            target = resultSet.getString("target"),
            reason = resultSet.getString("reason") ?: "",
            end = resultSet.getLong("endTime")
        )
    }

    private fun buildHikariConfig(): HikariConfig {
        return HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            maximumPoolSize = 4
            minimumIdle = 1
            connectionTimeout = 5000
            validationTimeout = 2000
            idleTimeout = 120_000
            maxLifetime = 300_000
        }
    }
}
