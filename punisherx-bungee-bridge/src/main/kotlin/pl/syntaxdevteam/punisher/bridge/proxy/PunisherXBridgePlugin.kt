package pl.syntaxdevteam.punisher.bridge.proxy

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.scheduler.ScheduledTask
import net.md_5.bungee.event.EventHandler
import pl.syntaxdevteam.syntaxdb.Database
import java.net.InetSocketAddress
import java.nio.file.Files
import java.time.Duration
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PunisherXBridgePlugin : Plugin(), Listener {
    private companion object {
        const val CHANNEL = "punisherx:bridge"
        const val UNAVAILABLE = "PunisherX: unable to verify access. Please retry shortly."
    }

    @Volatile private var database: Database? = null
    @Volatile private var databaseHealthy = false
    @Volatile private var lastFailureLog = 0L
    private lateinit var properties: Properties
    private var pollTask: ScheduledTask? = null
    private val polling = AtomicBoolean(false)
    private val cachedBans = ConcurrentHashMap<UUID, ActiveBan>()

    override fun onEnable() {
        properties = loadConfig()
        proxy.registerChannel(CHANNEL)
        proxy.pluginManager.registerListener(this, this)
        database = runCatching { connectDatabase(properties) }.onFailure { error ->
            proxy.logger.severe("PunisherX database unavailable: connections will be denied until recovery: ${error.message}")
        }.getOrNull()
        databaseHealthy = database != null
        val interval = (properties.getProperty("poll-interval-ms")?.toLongOrNull() ?: 1000L).coerceAtLeast(200L)
        pollTask = proxy.scheduler.schedule(this, Runnable { reconcileOnlinePlayers() },
            interval, interval, TimeUnit.MILLISECONDS)
        proxy.logger.info("PunisherX Bungee bridge ready: SQL-authoritative punishment enforcement")
    }

    override fun onDisable() {
        pollTask?.cancel()
        pollTask = null
        proxy.unregisterChannel(CHANNEL)
        proxy.pluginManager.unregisterListener(this)
        database?.close()
        database = null
        databaseHealthy = false
        cachedBans.clear()
    }

    /** The legacy channel must never execute a player- or backend-supplied BAN payload. */
    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (!event.tag.equals(CHANNEL, ignoreCase = true)) return
        event.isCancelled = true
        if (event.sender !is Server) return // Explicit client-origin rejection
        // Backend-origin payloads are also ignored. Only SQL can authorize a punishment.
    }

    /** LoginEvent supports async intents; no JDBC work runs on Bungee's connection event loop. */
    @EventHandler
    fun onLogin(event: LoginEvent) {
        event.registerIntent(this)
        proxy.scheduler.runAsync(this, Runnable {
            try {
                val uuid = event.connection.uniqueId
                    ?: throw IllegalStateException("Player UUID not available at login")
                val ip = (event.connection.socketAddress as? InetSocketAddress)?.address?.hostAddress
                    ?: throw IllegalStateException("Client IP not available at login")
                val ban = findActiveBan(uuid, ip)
                databaseHealthy = true
                if (ban != null) {
                    event.isCancelled = true
                    event.reason = TextComponent(banText(ban))
                }
            } catch (error: Exception) {
                databaseHealthy = false
                logDatabaseFailure(error)
                event.isCancelled = true
                event.reason = TextComponent(UNAVAILABLE)
            } finally {
                event.completeIntent(this)
            }
        })
    }

    /** Transfer checks use the last async snapshot; no blocking database access here. */
    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        if (!databaseHealthy) {
            event.isCancelled = true
            event.player.disconnect(TextComponent(UNAVAILABLE))
            return
        }
        val ban = cachedBans[event.player.uniqueId] ?: return
        event.isCancelled = true
        disconnect(event.player, ban)
    }

    /** Each proxy reconciles its own sessions with authoritative active SQL state.
     * No shared processed cursor, replayable packet or write permission is required.
     */
    private fun reconcileOnlinePlayers() {
        if (!polling.compareAndSet(false, true)) return
        try {
            val db = database ?: connectDatabase(properties).also { database = it }
            val players = proxy.players.toList()
            if (players.isEmpty()) {
                db.query("SELECT 1", emptyList())
                databaseHealthy = true
                cachedBans.clear()
                return
            }
            val identities = players.mapNotNull { player ->
                (player.socketAddress as? InetSocketAddress)?.address?.hostAddress
                    ?.let { ConnectionIdentity(player.uniqueId, it) }
            }
            val bans = BridgeBanRepository.findActiveBans(db, identities)
            databaseHealthy = true
            cachedBans.clear()
            cachedBans.putAll(bans)
            players.forEach { player ->
                val ban = bans[player.uniqueId] ?: return@forEach
                if (identities.any { it.uuid == player.uniqueId &&
                        it.ip == (player.socketAddress as? InetSocketAddress)?.address?.hostAddress }) {
                    disconnect(player, ban)
                }
            }
        } catch (error: Exception) {
            databaseHealthy = false
            logDatabaseFailure(error)
        } finally {
            polling.set(false)
        }
    }

    private fun findActiveBan(uuid: UUID, ip: String): ActiveBan? =
        BridgeBanRepository.findActiveBans(requireNotNull(database) { "PunisherX database is not connected" },
            listOf(ConnectionIdentity(uuid, ip)))[uuid]

    private fun logDatabaseFailure(error: Exception) {
        val now = System.currentTimeMillis()
        if (now - lastFailureLog >= 30_000L) {
            lastFailureLog = now
            proxy.logger.warning("PunisherX database verification failed (access checks fail closed): ${error.message}")
        }
    }

    private fun banText(ban: ActiveBan): String = buildString {
        append("PunisherX ${ban.type}")
        if (ban.reason.isNotBlank()) append("\nReason: ${ban.reason}")
        append("\nDuration: ${formatDuration(ban.endTime)}")
    }

    private fun disconnect(player: ProxiedPlayer, ban: ActiveBan) {
        val builder = ComponentBuilder().append("PunisherX ${ban.type}", ComponentBuilder.FormatRetention.NONE)
            .color(ChatColor.RED)
        if (ban.reason.isNotBlank()) {
            builder.append("\nReason: ${ban.reason}", ComponentBuilder.FormatRetention.NONE).color(ChatColor.YELLOW)
        }
        builder.append("\nDuration: ${formatDuration(ban.endTime)}", ComponentBuilder.FormatRetention.NONE)
            .color(ChatColor.GOLD)
        player.disconnect(*builder.create())
    }

    private fun loadConfig(): Properties {
        val file = dataFolder.toPath().resolve("bridge.properties")
        if (Files.notExists(file)) {
            Files.createDirectories(dataFolder.toPath())
            javaClass.getResourceAsStream("/bridge.properties")?.use { input -> Files.copy(input, file) }
        }
        return Properties().apply { Files.newInputStream(file).use { load(it) } }
    }

    private fun connectDatabase(properties: Properties): Database =
        Database.connect("mysql") {
            val host = properties.getProperty("host", "localhost")
            val port = properties.getProperty("port", "3306").toIntOrNull() ?: 3306
            val name = properties.getProperty("database", "punisherx")
            jdbcUrl = "jdbc:mysql://$host:$port/$name"
            username = properties.getProperty("username", "root")
            password = properties.getProperty("password", "change_me")
            property("characterEncoding", "UTF-8")
            pool {
                maximumPoolSize = 4; minimumIdle = 1
                connectionTimeoutMs = 5_000; validationTimeoutMs = 2_000
                idleTimeoutMs = 120_000; maxLifetimeMs = 300_000
            }
        }

    private fun formatDuration(end: Long): String {
        if (end == -1L) return "Permanent"
        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0) return "Expired"
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
