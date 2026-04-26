package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

class ReportCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        val sender = stack.sender
        val mH = plugin.messageHandler

        if (sender !is Player) {
            sender.sendMessage(mH.stringMessageToComponent("error", "console"))
            return
        }

        plugin.punisherMainGuiController.openReportSelector(sender)
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> = emptyList()
}
