package pl.syntaxdevteam.punisher.basic

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.sendMessage
import java.util.UUID

/** Sends the plugin author an informational message without granting any privileges. */
internal class AuthorUseNotifier(private val plugin: PunisherX) {
    private val authorUuid = UUID.fromString("248e508c-28de-4a8f-a284-2c73cf917d15")

    fun notifyIfAuthor(player: Player) {
        if (player.uniqueId != authorUuid) return

        player.sendMessage(
            plugin.messageHandler.formatMixedTextToMiniMessage(
                plugin.messageHandler.getPrefix() +
                    " <green>Witaj, <b>${player.name}</b><newline>    Ten serwer używa ${plugin.description.name} ${plugin.description.version} ❤",
                TagResolver.empty()
            )
        )
    }
}
