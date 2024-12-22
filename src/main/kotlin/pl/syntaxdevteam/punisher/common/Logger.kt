package pl.syntaxdevteam.punisher.common

import org.bukkit.Bukkit
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class Logger(plugin: PunisherX, private val debugMode: Boolean) {
    private val plName = plugin.pluginMeta.name
    private val plVer = plugin.pluginMeta.version
    private val serverVersion = plugin.server.version
    private val serverName = plugin.server.name

    private fun clear(s: String?) {
        Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(s!!))
    }

    fun success(s: String) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] ").color(NamedTextColor.GREEN).append(Component.text(s).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)))
    }

    fun info(s: String) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] $s"))
    }

    fun warning(s: String) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] ").color(NamedTextColor.GOLD).append(Component.text(s).color(NamedTextColor.GOLD)))
    }

    fun err(s: String) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] ").color(NamedTextColor.RED).append(Component.text(s).color(NamedTextColor.RED)))
    }

    fun severe(s: String) {
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] ").color(NamedTextColor.RED).append(Component.text(s).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)))
    }

    fun log(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize(s)
        Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] ").append(component))
    }

    fun clearLog(component: Component) {
        val coloredMessage = LegacyComponentSerializer.legacySection().serialize(component)
        Bukkit.getConsoleSender().sendMessage("[$plName] $coloredMessage")
    }

    fun debug(s: String) {
        if (debugMode) {
            Bukkit.getConsoleSender().sendMessage(Component.text("[$plName] [DEBUG] ").color(NamedTextColor.YELLOW).append(Component.text(s).color(NamedTextColor.YELLOW).decorate(
                TextDecoration.BOLD)))
        }
    }

    fun pluginStart(pluginsByPriority: List<Pair<String, String>>) {
        clear("")
        clear("&9    __                   __        ___            ")
        clear("&9   (_      _  |_  _     |  \\  _     |   _  _   _  ")
        clear("&9   __) \\/ | ) |_ (_| )( |__/ (- \\/  |  (- (_| ||| ")
        clear("&9       /                                          ")
        clear("&9           ... is proud to present and enable:")
        clear("&9                     &f * &f&l${plName} v${plVer}")
        for ((pluginName, pluginVersion) in pluginsByPriority) {
            clear("&9                     &f * $pluginName v$pluginVersion")
        }
        clear("&9                 utilizing all the optimizations of your server $serverName $serverVersion!         ")
        clear("")
        clear("&a    Join our Discord! &9&lhttps://discord.gg/Zk6mxv7eMh")
        clear("")
        clear("")
    }
}