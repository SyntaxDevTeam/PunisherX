package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class SetSpawnCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.getLogMessage("error", "console"))
            return
        }

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_SPAWN)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        val player = stack.sender as Player
        val location = player.location

        val world = location.world?.name ?: "world"
        val locationX = location.blockX.toString()
        val locationY = location.blockY.toString()
        val locationZ = location.blockZ.toString()

        if (JailUtils.setUnjailLocation(plugin.config, location)) {
            plugin.saveConfig()
            stack.sender.sendMessage(
                plugin.messageHandler.getMessage(
                    "setspawn", "set", mapOf(
                        "world" to world,
                        "locationx" to locationX,
                        "locationy" to locationY,
                        "locationz" to locationZ
                    )
                )
            )
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("setspawn", "set_error"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_SPAWN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> listOf("radius")
            2 -> (1..100).map { it.toString() }
            else -> emptyList()
        }
    }
}
