package pl.syntaxdevteam.punisher.books

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Shared transport for book menus. Every opened menu replaces the player's previous actions. */
class BookManager(private val plainText: (Component) -> String, private val clock: () -> Long = System::currentTimeMillis) : Listener {
    private data class Session(val expiresAt: Long, val actions: Map<String, () -> Unit>)
    private val sessions = ConcurrentHashMap<UUID, Session>()

    class Menu {
        internal val actions = mutableMapOf<String, () -> Unit>()
        fun button(label: Component, action: () -> Unit): Component {
            val token = UUID.randomUUID().toString()
            actions[token] = action
            return label.clickEvent(ClickEvent.runCommand("/report __prx:book $token"))
        }
    }

    fun open(player: Player, title: Component, author: Component, build: (Menu) -> List<Component>) {
        val menu = Menu()
        val pages = build(menu).ifEmpty { listOf(Component.empty()) }.take(100).map {
            Component.empty().color(net.kyori.adventure.text.format.NamedTextColor.BLACK).append(it)
        }
        sessions[player.uniqueId] = Session(clock() + 300_000, menu.actions.toMap())
        val book = org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK)
        val meta = book.itemMeta as org.bukkit.inventory.meta.BookMeta
        meta.title = plainText(title).take(32)
        meta.author = plainText(author)
        meta.spigot().setPages(*pages.map { net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(it) }.toTypedArray())
        book.itemMeta = meta
        player.openBook(book)
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!event.message.startsWith("/report __prx:book ", true)) return
        event.isCancelled = true
        val session = sessions[event.player.uniqueId] ?: return
        if (session.expiresAt < clock()) { sessions.remove(event.player.uniqueId); return }
        val token = event.message.substringAfter("__prx:book ", "")
        val action = session.actions[token] ?: return
        if (sessions.remove(event.player.uniqueId, session)) action()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) { sessions.remove(event.player.uniqueId) }
}
