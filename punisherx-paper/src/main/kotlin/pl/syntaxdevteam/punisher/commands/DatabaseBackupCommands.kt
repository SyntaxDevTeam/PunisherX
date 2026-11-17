package pl.syntaxdevteam.punisher.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.util.concurrent.atomic.AtomicBoolean

/** One backup command at a time; database and file work never run on the command thread. */
internal class DatabaseBackupCommands(private val plugin: PunisherX) {
    private val inProgress = AtomicBoolean(false)

    fun execute(sender: CommandSender, importing: Boolean) {
        if (!inProgress.compareAndSet(false, true)) {
            send(sender, "busy")
            return
        }
        val operation = if (importing) "import" else "export"
        try {
            send(sender, "$operation-started")
            plugin.schedulerAdapter.runAsync(Runnable {
                val result = try {
                    if (importing) plugin.databaseHandler.importDatabase()
                    else plugin.databaseHandler.exportDatabase()
                    "$operation-success"
                } catch (error: Exception) {
                    report(operation, error)
                    "$operation-failed"
                } finally {
                    inProgress.set(false)
                }
                if (plugin.isEnabled) {
                    val reply = Runnable { send(sender, result) }
                    if (sender is Player) plugin.schedulerAdapter.runForPlayer(sender, reply)
                    else plugin.schedulerAdapter.runSync(reply)
                }
            })
        } catch (error: Exception) {
            inProgress.set(false)
            report(operation, error)
            send(sender, "$operation-failed")
        }
    }

    private fun send(sender: CommandSender, key: String) {
        sender.sendMessage(plugin.messageHandler.stringMessageToComponent("database-backup", key))
    }

    private fun report(operation: String, error: Exception) {
        plugin.logger.err("[Backup] Failed to $operation database: ${error.message ?: error.javaClass.simpleName}")
        plugin.reportError(error)
    }
}
