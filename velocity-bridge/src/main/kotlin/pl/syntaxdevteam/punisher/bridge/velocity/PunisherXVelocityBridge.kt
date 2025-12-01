package pl.syntaxdevteam.punisher.bridge.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
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
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.nio.file.Path
import javax.inject.Inject

private const val BRIDGE_VERSION: String = "1.6.0-DEV" // Keep aligned with the root project version and velocity-plugin.json expansion

@Plugin(
    id = "punisherxbridge",
    name = "PunisherXBridge",
    version = BRIDGE_VERSION,
    authors = ["SyntaxDevTeam"],
    description = "PunisherX bridge for Velocity"
)
class PunisherXVelocityBridge @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) {

    private val channel: ChannelIdentifier = MinecraftChannelIdentifier.from("punisherx:bridge")
    private lateinit var bridgeConfig: BridgeConfig
    private lateinit var bridgeDatabase: BridgeDatabase
    private var pollTask: ScheduledTask? = null

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        server.channelRegistrar.register(channel)
        logger.info("PunisherX proxy bridge ready for channel {}", channel.id)

        bridgeConfig = BridgeConfig.load(dataDirectory, logger)
        bridgeDatabase = BridgeDatabase(logger, bridgeConfig)
        bridgeDatabase.ensureSchema()
        schedulePolling()
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != channel) {
            return
        }

        event.result = PluginMessageEvent.ForwardResult.handled()

        val dataInput = DataInputStream(ByteArrayInputStream(event.data))
        val action = runCatching { dataInput.readUTF() }.getOrNull()?.uppercase(Locale.getDefault()) ?: return
        val target = runCatching { dataInput.readUTF() }.getOrNull() ?: return
        val reason = runCatching { dataInput.readUTF() }.getOrNull() ?: ""
        val end = runCatching { dataInput.readLong() }.getOrNull() ?: -1L

        when (action) {
            "BAN" -> handleBan(target, reason, end)
            "BANIP" -> handleBanIp(target, reason, end)
            else -> logger.warn("Received unsupported PunisherX bridge action: $action")
        }
    }

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        pollTask?.cancel()
        bridgeDatabase.close()
    }

    private fun schedulePolling() {
        pollTask?.cancel()
        pollTask = server.scheduler
            .buildTask(this, Runnable { pollQueue() })
            .repeat(bridgeConfig.pollInterval)
            .schedule()
    }

    private fun pollQueue() {
        val events = bridgeDatabase.fetchPendingEvents()
        if (events.isEmpty()) return

        events.forEach { event ->
            when (event.action.uppercase(Locale.getDefault())) {
                "BAN" -> handleBan(event.target, event.reason, event.end)
                "BANIP" -> handleBanIp(event.target, event.reason, event.end)
                else -> logger.warn("Unknown bridge action ${event.action} for event ${event.id}")
            }

            bridgeDatabase.markProcessed(event.id)
        }
    }

    private fun handleBan(target: String, reason: String, end: Long) {
        val uuid = runCatching { UUID.fromString(target) }.getOrNull()
        if (uuid == null) {
            logger.warn("PunisherX bridge received invalid UUID for BAN: $target")
            return
        }

        server.getPlayer(uuid).ifPresent { player -> disconnect(player, "BAN", reason, end) }
    }

    private fun handleBanIp(targetIp: String, reason: String, end: Long) {
        val normalizedIp = targetIp.trim()
        server.allPlayers
            .filter { player -> player.remoteAddress.address.hostAddress == normalizedIp }
            .forEach { player -> disconnect(player, "BANIP", reason, end) }
    }

    private fun disconnect(player: Player, type: String, reason: String, end: Long) {
        val lines = buildList {
            add(Component.text("PunisherX $type", NamedTextColor.RED))
            if (reason.isNotBlank()) {
                add(Component.text("Reason: $reason", NamedTextColor.YELLOW))
            }
            add(Component.text("Duration: ${formatDuration(end)}", NamedTextColor.GOLD))
        }

        val message = Component.join(JoinConfiguration.newlines(), lines)
        player.disconnect(message)
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
