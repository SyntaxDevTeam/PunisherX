package pl.syntaxdevteam.punisher.bridge.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.ResultedEvent.ComponentResult
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import java.time.Duration
import java.nio.file.Path
import com.google.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

// Plugin metadata and version come from the Gradle-expanded velocity-plugin.json.
class PunisherXVelocityBridge @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @param:DataDirectory private val dataDirectory: Path
) {

    private val channel: ChannelIdentifier = MinecraftChannelIdentifier.from("punisherx:bridge")
    private lateinit var bridgeConfig: BridgeConfig
    private lateinit var bridgeDatabase: BridgeDatabase
    private var pollTask: ScheduledTask? = null
    private val polling = AtomicBoolean(false)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        server.channelRegistrar.register(channel)
        logger.info("PunisherX proxy bridge ready for channel {}", channel.id)

        bridgeConfig = BridgeConfig.load(dataDirectory, logger)
        bridgeDatabase = BridgeDatabase(logger, bridgeConfig)
        runCatching { bridgeDatabase.ensureSchema() }.onFailure {
            logger.error("PunisherX proxy access checks are fail-closed: database initialization failed", it)
        }
        schedulePolling()
    }

    /** Legacy clients/backends cannot submit punishment commands over plugin messaging.
     * Consume the private channel without forwarding or executing any payload.
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier == channel) {
            event.result = PluginMessageEvent.ForwardResult.handled()
        }
    }

    @Subscribe(async = true)
    fun onLogin(event: LoginEvent) {
        try {
            val player = event.player
            val ban = bridgeDatabase.findActiveBan(
                player.uniqueId, ip = player.remoteAddress.address.hostAddress
            )
            if (ban != null) {
                event.result = ComponentResult.denied(banMessage(ban.type, ban.reason, ban.endTime))
            }
        } catch (error: Exception) {
            logger.error("Unable to verify network punishment during login", error)
            event.result = ComponentResult.denied(databaseUnavailable())
        }
    }

    @Subscribe(async = true)
    fun onServerPreConnect(event: ServerPreConnectEvent) {
        try {
            val player = event.player
            val ban = bridgeDatabase.findActiveBan(
                player.uniqueId, ip = player.remoteAddress.address.hostAddress
            )
            if (ban != null) {
                event.result = ServerPreConnectEvent.ServerResult.denied()
                disconnect(player, ban.type, ban.reason, ban.endTime)
            }
        } catch (error: Exception) {
            logger.error("Unable to verify network punishment before transfer", error)
            event.result = ServerPreConnectEvent.ServerResult.denied()
            event.player.disconnect(databaseUnavailable())
        }
    }

    private fun databaseUnavailable(): Component =
        Component.text("PunisherX: unable to verify access. Please retry shortly.", NamedTextColor.RED)

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        pollTask?.cancel()
        if (::bridgeDatabase.isInitialized) {
            bridgeDatabase.close()
        }
    }

    private fun schedulePolling() {
        pollTask?.cancel()
        pollTask = server.scheduler
            .buildTask(this, Runnable { pollOnlinePlayers() })
            .repeat(bridgeConfig.pollInterval)
            .schedule()
    }

    /** Reconciliation uses current SQL state, not replayable queue messages or a global processed flag.
     * Every proxy reads independently, so no competing proxy can consume another proxy's delivery.
     */
    private fun pollOnlinePlayers() {
        if (!polling.compareAndSet(false, true)) return
        try {
            val snapshots = server.allPlayers.map {
                ConnectionIdentity(it.uniqueId, it.remoteAddress.address.hostAddress)
            }
            val bans = bridgeDatabase.findActiveBans(snapshots)
            snapshots.forEach { snapshot ->
                val ban = bans[snapshot.uuid] ?: return@forEach
                server.getPlayer(snapshot.uuid).ifPresent { player ->
                    if (player.remoteAddress.address.hostAddress == snapshot.ip) {
                        disconnect(player, ban.type, ban.reason, ban.endTime)
                    }
                }
            }
        } catch (error: Exception) {
            logger.error("PunisherX network ban reconciliation failed; new logins and transfers remain fail-closed", error)
        } finally {
            polling.set(false)
        }
    }

    private fun disconnect(player: Player, type: String, reason: String, end: Long) {
        player.disconnect(banMessage(type, reason, end))
    }

    private fun banMessage(type: String, reason: String, end: Long): Component {
        val lines = buildList {
            add(Component.text("PunisherX $type", NamedTextColor.RED))
            if (reason.isNotBlank()) {
                add(Component.text("Reason: $reason", NamedTextColor.YELLOW))
            }
            add(Component.text("Duration: ${formatDuration(end)}", NamedTextColor.GOLD))
        }

        return Component.join(JoinConfiguration.newlines(), lines)
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
