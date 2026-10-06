package pl.syntaxdevteam.punisher.bridge.velocity

import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Properties

data class BridgeConfig(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val pollInterval: Duration
) {
    val jdbcUrl: String = "jdbc:mysql://$host:$port/$database"

    companion object {
        private const val DEFAULT_POLL_MS = 1000L

        fun load(dataDirectory: Path, logger: Logger): BridgeConfig {
            val configPath = dataDirectory.resolve("bridge.properties")
            if (Files.notExists(configPath)) {
                Files.createDirectories(dataDirectory)
                BridgeConfig::class.java.getResourceAsStream("/bridge.properties")?.use { input ->
                    Files.copy(input, configPath)
                }
            }

            val props = Properties()
            try {
                Files.newInputStream(configPath).use(props::load)
            } catch (ex: IOException) {
                logger.error("Unable to read bridge.properties, falling back to defaults", ex)
            }

            val pollMs = props.getProperty("poll-interval-ms")?.toLongOrNull() ?: DEFAULT_POLL_MS

            return BridgeConfig(
                host = props.getProperty("host", "localhost"),
                port = props.getProperty("port")?.toIntOrNull() ?: 3306,
                database = props.getProperty("database", "punisherx"),
                username = props.getProperty("username", "root"),
                password = props.getProperty("password", "change_me"),
                pollInterval = Duration.ofMillis(pollMs.coerceAtLeast(200))
            )
        }
    }
}
