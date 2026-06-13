package pl.syntaxdevteam.punisher.compatibility

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.meta.ItemMeta

private val legacySerializer = LegacyComponentSerializer.legacySection()

fun Component.toSpigotString(): String = legacySerializer.serialize(this)

fun CommandSender.sendMessage(component: Component) {
    sendMessage(component.toSpigotString())
}

fun CommandSender.sendRichMessage(message: String) {
    sendMessage(MiniMessage.miniMessage().deserialize(message))
}

fun Player.kick(component: Component) {
    kickPlayer(component.toSpigotString())
}

fun Player.sendActionBar(component: Component) {
    spigot().sendMessage(
        ChatMessageType.ACTION_BAR,
        *TextComponent.fromLegacyText(component.toSpigotString())
    )
}

fun PlayerLoginEvent.disallow(result: PlayerLoginEvent.Result, component: Component) {
    disallow(result, component.toSpigotString())
}

fun AsyncPlayerPreLoginEvent.disallow(
    result: AsyncPlayerPreLoginEvent.Result,
    component: Component
) {
    disallow(result, component.toSpigotString())
}

fun createSpigotInventory(
    holder: InventoryHolder?,
    size: Int,
    title: Component
): Inventory = Bukkit.createInventory(holder, size, title.toSpigotString())

fun createSpigotInventory(
    holder: InventoryHolder?,
    size: Int,
    title: String
): Inventory = Bukkit.createInventory(holder, size, title)

fun ItemMeta.displayName(component: Component) {
    setDisplayName(component.toSpigotString())
}

fun ItemMeta.lore(components: List<Component>?) {
    lore = components?.map(Component::toSpigotString)
}

fun InventoryView.title(): Component = Component.text(title)

val OfflinePlayer.lastLogin: Long
    get() = firstPlayed

val OfflinePlayer.lastSeen: Long
    get() = lastPlayed
