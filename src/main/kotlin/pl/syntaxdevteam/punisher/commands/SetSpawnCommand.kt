package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class SetSpawnCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponentNoPrefix("error", "console"))
            return
        }

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_SPAWN)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
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
                plugin.messageHandler.stringMessageToComponent(
                    "setunjail", "set", mapOf(
                        "world" to world,
                        "locationx" to locationX,
                        "locationy" to locationY,
                        "locationz" to locationZ
                    )
                )
            )
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("setunjail", "set_error"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MANAGE_SET_SPAWN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> listOf("radius")
            2 -> (1..100).map { it.toString() }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPlayerPermission(PermissionChecker.PermissionKey.MANAGE_SET_SPAWN))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .build()
    }
}
