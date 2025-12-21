package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import pl.syntaxdevteam.punisher.PunisherX

class CacheCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        val sender = stack.sender

        if (sender !is org.bukkit.command.ConsoleCommandSender) {
            sender.sendRichMessage("Console only!")
            return
        }

        val playerIPManager = plugin.playerIPManager
        val allRecords = playerIPManager.getAllDecryptedRecords()

        if (allRecords.isEmpty()) {
            plugin.logger.info("Cache is empty.")
        } else {
            plugin.logger.info("Cache Contents:")
            allRecords.forEach { record ->
                plugin.logger.info(record.toString())
            }
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresConsole())
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .build()
    }
}
