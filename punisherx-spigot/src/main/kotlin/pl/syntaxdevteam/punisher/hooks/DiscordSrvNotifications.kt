package pl.syntaxdevteam.punisher.hooks

import pl.syntaxdevteam.punisher.PunisherX

internal object DiscordSrvNotifications {
    fun publish(plugin: PunisherX, type: String, values: Map<String, String>, silent: Boolean = false) {
        if (silent || !plugin.isEnabled) return
        if (!plugin.server.pluginManager.isPluginEnabled("DiscordSRV")) return
        runCatching {
            val config = plugin.config
            val text = DiscordSrvNotificationPolicy.render(config, type, values, silent) { key, placeholders ->
                plugin.messageHandler.getPlainText(
                    plugin.messageHandler.stringMessageToComponentNoPrefixLiteral("discordsrv", key, placeholders)
                )
            } ?: return
            val channel = config.getString("${DiscordSrvNotificationPolicy.ROOT}.notifications.channel", "global").orEmpty().trim()
            if (channel.isNotEmpty()) DiscordSrvNotificationTransport.send(channel, text)
        }.onFailure {
            plugin.logger.warning("DiscordSRV punishment notification failed: ${it.javaClass.simpleName}")
        }
    }
}
