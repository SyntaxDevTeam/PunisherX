package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class PanelCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "not_a_player"))
            return
        }
        if (!PermissionChecker.hasWithManage(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        PunisherMain(plugin).open(stack.sender as Player)
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPlayerPermission(PermissionChecker.PermissionKey.PUNISHERX_COMMAND))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .build()
    }
}
