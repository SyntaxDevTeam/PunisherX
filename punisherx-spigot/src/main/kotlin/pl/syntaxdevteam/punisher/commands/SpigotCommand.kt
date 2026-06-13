package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandSourceStack(val sender: CommandSender)

interface BasicCommand {
    fun execute(stack: CommandSourceStack, args: Array<String>)

    fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> = emptyList()
}

internal class SpigotCommandAdapter(
    private val delegate: BasicCommand
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        delegate.execute(CommandSourceStack(sender), args.map { it }.toTypedArray())
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return delegate.suggest(CommandSourceStack(sender), args.map { it }.toTypedArray())
    }
}
