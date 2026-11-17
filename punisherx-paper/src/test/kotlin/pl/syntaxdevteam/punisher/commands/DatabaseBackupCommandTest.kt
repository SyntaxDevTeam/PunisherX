package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.mockito.kotlin.*
import org.bukkit.command.CommandSender

class DatabaseBackupCommandTest : DatabaseBackupCommandContract() {
    private val command by lazy { PunisherRootCommand(plugin) }

    override fun executeRoot(sender: CommandSender, operation: String) {
        val stack = mock<CommandSourceStack>().also { whenever(it.sender).thenReturn(sender) }
        command.execute(stack, arrayOf(operation))
    }
}
