package pl.syntaxdevteam.punisher.hooks

import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.util.DiscordUtil

/** Loaded only when the optional DiscordSRV plugin is enabled. */
internal object DiscordSrvNotificationTransport {
    fun send(channelName: String, text: String) {
        val channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName) ?: return
        DiscordUtil.queueMessage(channel, text, false)
    }
}
