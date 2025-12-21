package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class SetjailCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {

        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponentNoPrefix("error", "console"))
            return
        }


        if (!PermissionChecker.hasWithManage(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("setjail", "usage"))
            return
        }

        val player = stack.sender as Player
        val location = player.location

        val radius = args[0].toDoubleOrNull()
        if (radius == null || radius <= 0) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "invalid_radius"))
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
                plugin.messageHandler.stringMessageToComponent(
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
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("setjail", "set_error"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
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

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val radiusArg = Commands.argument("radius", DoubleArgumentType.doubleArg(0.0))
            .executes { context ->
                val radius = DoubleArgumentType.getDouble(context, "radius")
                execute(context.source, listOf(radius.toString()))
                1
            }

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPlayerPermission(PermissionChecker.PermissionKey.MANAGE_SET_JAIL))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(radiusArg)
            .build()
    }
}
