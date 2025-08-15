package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PlayerListGUI

class PlayerListCommand(private val plugin: PunisherX) : BasicCommand {
    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (stack.sender !is Player) {
            stack.sender.sendMessage(plugin.messageHandler.getLogMessage("error", "console"))
            return
        }

        val player = stack.sender as Player
        PlayerListGUI(plugin).open(player)
    }
}
