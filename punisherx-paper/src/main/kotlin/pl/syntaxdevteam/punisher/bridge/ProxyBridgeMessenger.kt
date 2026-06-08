package pl.syntaxdevteam.punisher.bridge

import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

class ProxyBridgeMessenger(private val plugin: PunisherX) {

    companion object {
        const val CHANNEL = "punisherx:bridge"
    }

    fun registerChannel() {
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, CHANNEL)
    }

    fun unregisterChannel() {
        plugin.server.messenger.unregisterOutgoingPluginChannel(plugin)
    }

    fun notifyBan(target: UUID, reason: String, end: Long) {
        dispatch("BAN", target.toString(), reason, end)
    }

    fun notifyIpBan(ip: String, reason: String, end: Long) {
        dispatch("BANIP", ip, reason, end)
    }

    private fun dispatch(action: String, target: String, reason: String, end: Long) {
        plugin.databaseHandler.enqueueBridgeEvent(action, target, reason, end)

        val output = ByteStreams.newDataOutput()
        output.writeUTF(action)
        output.writeUTF(target)
        output.writeUTF(reason)
        output.writeLong(end)

        val carrier = plugin.server.onlinePlayers.firstOrNull()
        if (carrier != null) {
            sendMessage(output, carrier)
            return
        }

        plugin.logger.debug("PunisherX proxy bridge skipped sending $action for $target: no players online to carry the message.")
    }

    private fun sendMessage(output: ByteArrayDataOutput, carrier: Player) {
        carrier.sendPluginMessage(plugin, CHANNEL, output.toByteArray())
    }
}
