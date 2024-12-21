package pl.syntaxdevteam.punisher.commands

import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.basic.JailUtils

@Suppress("UnstableApiUsage")
class SetjailCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(messageHandler.getLogMessage("error", "console"))
            return
        }

        if (!stack.sender.hasPermission("punisherx.setjail")) {
            stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            return
        }

        if (args.isEmpty() || !args[0].equals("radius", ignoreCase = true)) {
            stack.sender.sendRichMessage(messageHandler.getMessage("setjail", "usage"))
            return
        }

        val player = stack.sender as Player
        val location = player.location

        val radius = if (args.size > 1) args[1].toDoubleOrNull() else null
        if (radius == null || radius <= 0) {
            stack.sender.sendRichMessage(messageHandler.getMessage("error", "invalid_radius"))
            return
        }

        val world = location.world?.name ?: "unknown"
        val locationX = location.blockX.toString()
        val locationY = location.blockY.toString()
        val locationZ = location.blockZ.toString()
        val mRadius = radius.toString()

        if (JailUtils.setJailLocation(plugin.config, location, radius)) {
            plugin.saveConfig()
            stack.sender.sendRichMessage(
                messageHandler.getMessage(
                    "setjail", "set", mapOf(
                        "world" to world,
                        "locationx" to locationX,
                        "locationy" to locationY,
                        "locationz" to locationZ,
                        "radius" to mRadius
                    )
                )
            )
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("setjail", "set_error"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("radius")
            2 -> (1..100).map { it.toString() }
            else -> emptyList()
        }
    }
}
