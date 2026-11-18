package pl.syntaxdevteam.punisher.hooks

import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.ListenerPriority
import github.scarsz.discordsrv.api.Subscribe
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent
import java.util.UUID

/**
 * Cancels Minecraft-to-Discord chat relay before DiscordSRV processes the message.
 * Kept separate from Bukkit listeners so the optional API is only used when installed.
 */
class DiscordSrvChatHook(private val isChatRestricted: (UUID) -> Boolean) {

    fun register() {
        DiscordSRV.api.subscribe(this)
    }

    fun unregister() {
        DiscordSRV.api.unsubscribe(this)
    }

    @Subscribe(priority = ListenerPriority.HIGHEST)
    fun onGameChat(event: GameChatMessagePreProcessEvent) {
        if (event.isCancelled) return
        val uuid = event.player?.uniqueId ?: return
        if (isChatRestricted(uuid)) {
            event.isCancelled = true
        }
    }
}
