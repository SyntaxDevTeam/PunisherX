package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.report.ReportSelectorGUI

class ReportCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        val sender = stack.sender
        val mH = plugin.messageHandler

        if (sender !is Player) {
            sender.sendMessage(mH.stringMessageToComponent("error", "console"))
            return
        }

        ReportSelectorGUI(plugin).open(sender)
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> = emptyList()

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPlayer())
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .build()
    }
}
