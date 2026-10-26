package pl.syntaxdevteam.punisher.commands


import org.bukkit.command.CommandSender

class DatabaseBackupCommandTest : DatabaseBackupCommandContract() {
    private val command by lazy { PunisherRootCommand(plugin) }

    override fun executeRoot(sender: CommandSender, operation: String) {
        val stack = CommandSourceStack(sender)
        command.execute(stack, arrayOf(operation))
    }
}
