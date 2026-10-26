package pl.syntaxdevteam.punisher.bridge.proxy

import com.google.common.io.ByteStreams
import pl.syntaxdevteam.syntaxdb.Database
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.net.InetSocketAddress
import java.nio.file.Files
import java.time.Duration
import java.util.Locale
import java.util.Properties
import java.util.UUID

class PunisherXBridgePlugin : Plugin(), Listener {

    private companion object {
        const val CHANNEL = "punisherx:bridge"
    }

    private var database: Database? = null

    override fun onEnable() {
        proxy.registerChannel(CHANNEL)
        proxy.pluginManager.registerListener(this, this)
        database = connectDatabase(loadConfig())
        proxy.logger.info("PunisherX proxy bridge ready for channel $CHANNEL")
    }

    override fun onDisable() {
        proxy.unregisterChannel(CHANNEL)
        proxy.pluginManager.unregisterListener(this)
        database?.close()
        database = null
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (!event.tag.equals(CHANNEL, ignoreCase = true)) {
            return
        }

        val input = ByteStreams.newDataInput(event.data)
        val action = input.readUTF().uppercase(Locale.getDefault())
        val target = input.readUTF()
        val reason = input.readUTF()
        val end = input.readLong()

        when (action) {
            "BAN" -> handleBan(target, reason, end)
            "BANIP" -> handleBanIp(target, reason, end)
            else -> proxy.logger.warning("Received unsupported PunisherX bridge action: $action")
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val activeBan = findActiveBan(player.uniqueId) ?: return
        disconnect(player, "BAN", activeBan.reason, activeBan.endTime)
        event.isCancelled = true
    }

    private fun handleBan(target: String, reason: String, end: Long) {
        val uuid = runCatching { UUID.fromString(target) }.getOrNull()
        if (uuid == null) {
            proxy.logger.warning("PunisherX bridge received invalid UUID for BAN: $target")
            return
        }

        val player = proxy.getPlayer(uuid) ?: return
        disconnect(player, "BAN", reason, end)
    }

    private fun handleBanIp(targetIp: String, reason: String, end: Long) {
        val normalizedIp = targetIp.trim()
        proxy.players.filter { player ->
            (player.socketAddress as? InetSocketAddress)?.address?.hostAddress == normalizedIp
        }.forEach { player -> disconnect(player, "BANIP", reason, end) }
    }

    private fun disconnect(player: ProxiedPlayer, type: String, reason: String, end: Long) {
        val builder = ComponentBuilder()
            .append("PunisherX $type", ComponentBuilder.FormatRetention.NONE)
            .color(ChatColor.RED)
        if (reason.isNotBlank()) {
            builder.append("\nReason: $reason", ComponentBuilder.FormatRetention.NONE).color(ChatColor.YELLOW)
        }
        builder.append("\nDuration: ${formatDuration(end)}", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GOLD)
        player.disconnect(*builder.create())
    }

    private fun findActiveBan(uuid: UUID): ActiveBan? {
        return runCatching {
            database?.query(
                """SELECT reason, endTime FROM punishments
                    WHERE uuid = ? AND punishmentType = 'BAN' AND server = 'network'
                    AND (endTime = -1 OR endTime > ?) ORDER BY start DESC LIMIT 1""".trimIndent(),
                listOf(uuid.toString(), System.currentTimeMillis())
            )?.firstOrNull()?.let { ActiveBan(it.string("reason").orEmpty(), it.long("endTime") ?: 0L) }
        }.onFailure { ex ->
            proxy.logger.warning("Failed to check active BAN for $uuid: ${ex.message}")
        }.getOrNull()
    }

    private fun loadConfig(): Properties {
        val file = dataFolder.toPath().resolve("bridge.properties")
        if (Files.notExists(file)) {
            Files.createDirectories(dataFolder.toPath())
            javaClass.getResourceAsStream("/bridge.properties")?.use { input ->
                Files.copy(input, file)
            }
        }

        return Properties().apply {
            Files.newInputStream(file).use { load(it) }
        }
    }

    private fun connectDatabase(properties: Properties): Database {
        return Database.connect("mysql") {
            val host = properties.getProperty("host", "localhost")
            val port = properties.getProperty("port", "3306").toIntOrNull() ?: 3306
            val database = properties.getProperty("database", "punisherx")
            jdbcUrl = "jdbc:mysql://$host:$port/$database"
            username = properties.getProperty("username", "root")
            password = properties.getProperty("password", "change_me")
            property("characterEncoding", "UTF-8")
            pool {
                maximumPoolSize = 4; minimumIdle = 1
                connectionTimeoutMs = 5_000; validationTimeoutMs = 2_000
                idleTimeoutMs = 120_000; maxLifetimeMs = 300_000
            }
        }
    }

    private fun formatDuration(end: Long): String {
        if (end < 0) {
            return "Permanent"
        }

        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0) {
            return "Expired"
        }

        val duration = Duration.ofMillis(remaining)
        val days = duration.toDays()
        val hours = duration.minusDays(days).toHours()
        val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
        val seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).seconds

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}

data class ActiveBan(
    val reason: String,
    val endTime: Long
)
