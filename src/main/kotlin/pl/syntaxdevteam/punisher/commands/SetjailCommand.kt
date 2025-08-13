package pl.syntaxdevteam.punisher.commands

import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class SetjailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        // Sprawdzamy, czy to gracz
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.getLogMessage("error", "console"))
            return
        }

        // Sprawdzamy uprawnienia
        if (!PermissionChecker.hasWithManage(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("setjail", "usage"))
            return
        }

        val player = stack.sender as Player
        val location = player.location

        val radius = args[0].toDoubleOrNull()
        if (radius == null || radius <= 0) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "invalid_radius"))
            return
        }

        val world = location.world?.name ?: "unknown"
        val locationX = location.blockX.toString()
        val locationY = location.blockY.toString()
        val locationZ = location.blockZ.toString()
        val mRadius = radius.toString()

        if (JailUtils.setJailLocation(plugin.config, location, radius)) {
            plugin.saveConfig()
            stack.sender.sendMessage(
                plugin.messageHandler.getMessage(
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("setjail", "set_error"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithManage(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_JAIL)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> {
                (1..100).map { it.toString() }
            }
            else -> emptyList()
        }
    }
}
