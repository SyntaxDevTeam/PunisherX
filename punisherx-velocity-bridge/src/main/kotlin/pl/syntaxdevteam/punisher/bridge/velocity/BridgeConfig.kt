package pl.syntaxdevteam.punisher.bridge.velocity

import java.io.BufferedInputStream
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
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        fun load(configPath: Path): BridgeConfig {
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.parent)
                checkNotNull(BridgeConfig::class.java.getResourceAsStream("/bridge.properties")).use { input ->
                    Files.copy(input, configPath)
                }
            }

            val props = Properties()
            BufferedInputStream(Files.newInputStream(configPath)).use { input ->
                input.mark(UTF8_BOM.size)
                if (input.readNBytes(UTF8_BOM.size).contentEquals(UTF8_BOM)) {
                    props.load(input.reader(Charsets.UTF_8))
                } else {
                    input.reset()
                    props.load(input)
                }
            }

            val host = props.getProperty("host")?.trim().orEmpty()
            require(host.isNotEmpty()) { "Set host to the MySQL server address" }
            val port = props.getProperty("port", "3306").trim().toIntOrNull()
            require(port != null && port in 1..65535) { "Set port to a number between 1 and 65535" }
            val pollMs = props.getProperty("poll-interval-ms")?.toLongOrNull() ?: DEFAULT_POLL_MS

            return BridgeConfig(
                host = host,
                port = port,
                database = props.getProperty("database", "punisherx"),
                username = props.getProperty("username", "punisherx_proxy"),
                password = props.getProperty("password", ""),
                pollInterval = Duration.ofMillis(pollMs.coerceAtLeast(200))
            )
        }
    }
}
