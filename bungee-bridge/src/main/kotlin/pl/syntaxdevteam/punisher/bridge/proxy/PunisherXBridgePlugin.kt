package pl.syntaxdevteam.punisher.bridge.proxy

import com.google.common.io.ByteStreams
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.time.Duration
import java.util.Locale
import java.util.UUID

class PunisherXBridgePlugin : Plugin(), Listener {

    private companion object {
        const val CHANNEL = "punisherx:bridge"
    }

    override fun onEnable() {
        proxy.registerChannel(CHANNEL)
        proxy.pluginManager.registerListener(this, this)
        proxy.logger.info("PunisherX proxy bridge ready for channel $CHANNEL")
    }

    override fun onDisable() {
        proxy.unregisterChannel(CHANNEL)
        proxy.pluginManager.unregisterListener(this)
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
            (player.socketAddress as? java.net.InetSocketAddress)?.address?.hostAddress == normalizedIp
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
