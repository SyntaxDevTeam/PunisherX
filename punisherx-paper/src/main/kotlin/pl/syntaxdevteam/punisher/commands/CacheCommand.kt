package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

class CacheCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val sender = stack.sender

        if (sender !is org.bukkit.command.ConsoleCommandSender) {
            sender.sendRichMessage("Console only!")
            return
        }

        val playerIPManager = plugin.playerIPManager
        val latestRecords = playerIPManager.getLatestDecryptedRecords()
        val allRecords = playerIPManager.getAllDecryptedRecords()

        if (latestRecords.isEmpty()) {
            plugin.logger.info("Cache is empty.")
        } else {
            plugin.logger.info("Cache Contents (latest per UUID):")
            latestRecords.forEach { record ->
                plugin.logger.info(record.toString())
            }
            plugin.logger.info("Displayed ${latestRecords.size} latest record(s) from ${allRecords.size} total cache line(s).")
        }
    }
}
