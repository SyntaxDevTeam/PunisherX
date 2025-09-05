package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class PanelCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "not_a_player"))
            return
        }
        if (!PermissionChecker.hasWithManage(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        PunisherMain(plugin).open(stack.sender as Player)
    }
}