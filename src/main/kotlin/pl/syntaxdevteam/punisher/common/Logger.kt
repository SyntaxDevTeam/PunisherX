package pl.syntaxdevteam.punisher.common

import org.bukkit.Bukkit
import net.kyori.adventure.text.Component
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
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(s!!))
    }

    fun success(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize("<green><bold>[$plName]</bold> $s")
        Bukkit.getConsoleSender().sendMessage(component)
    }

    fun info(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize("[$plName] $s")
        Bukkit.getConsoleSender().sendMessage(component)
    }

    fun warning(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize("<gold>[$plName] $s")
        Bukkit.getConsoleSender().sendMessage(component)
    }

    fun err(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize("<red>[$plName] $s")
        Bukkit.getConsoleSender().sendMessage(component)
    }

    fun severe(s: String) {
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize("<red><bold>[$plName]</bold> $s")
        Bukkit.getConsoleSender().sendMessage(component)
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
            val miniMessage = MiniMessage.miniMessage()
            val component = miniMessage.deserialize("<yellow><bold>[$plName] [DEBUG]</bold> $s")
            Bukkit.getConsoleSender().sendMessage(component)
        }
    }

    fun pluginStart(pluginsByPriority: List<Pair<String, String>>) {
        clear("")
        clear("<blue>    __                   __        ___            ")
        clear("<blue>   (_      _  |_  _     |  \\  _     |   _  _   _  ")
        clear("<blue>   __) \\/ | ) |_ (_| )( |__/ (- \\/  |  (- (_| ||| ")
        clear("<blue>       /                                          ")
        clear("<blue>           ... is proud to present and enable:")
        clear("<blue>                     <white> * <white><bold>${plName} v${plVer}")
        for ((pluginName, pluginVersion) in pluginsByPriority) {
            clear("<blue>                     <white> * $pluginName v$pluginVersion")
        }
        clear("<blue>                 utilizing all the optimizations of your server $serverName $serverVersion!         ")
        clear("")
        clear("<green>    Join our Discord! <blue><bold>https://discord.gg/Zk6mxv7eMh")
        clear("")
        clear("")
    }
}