package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.report.ReportReasonGUI
import pl.syntaxdevteam.punisher.gui.report.ReportSelectorGUI
import java.util.UUID

class ReportCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        val sender = stack.sender
        val mH = plugin.messageHandler

        if (sender !is Player) {
            sender.sendMessage(mH.stringMessageToComponent("error", "console"))
            return
        }

        if (!plugin.databaseHandler.isReady()) {
            sender.sendMessage(mH.stringMessageToComponent("error", "db_not_ready"))
            return
        }
        if (plugin.reportService.hasOpenReport(sender)) {
            sender.sendMessage(mH.stringMessageToComponent("reports", "already-submitted"))
            return
        }

        val target = args.firstOrNull()?.let(::resolveTarget)
        if (args.isNotEmpty() && target == null) {
            sender.sendMessage(
                mH.stringMessageToComponent(
                    "error",
                    "player_not_found",
                    mapOf("player" to args[0])
                )
            )
            return
        }
        if (target?.uniqueId == sender.uniqueId) {
            sender.sendMessage(mH.stringMessageToComponent("reports", "cannot-report-self"))
            return
        }

        if (target == null) {
            ReportSelectorGUI(plugin).open(sender)
        } else {
            ReportReasonGUI(plugin).open(sender, target)
        }
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        val sender = stack.sender as? Player ?: return emptyList()
        if (args.size > 1) return emptyList()
        val input = args.lastOrNull().orEmpty()
        return plugin.server.onlinePlayers
            .asSequence()
            .filter { it.uniqueId != sender.uniqueId }
            .map { it.name }
            .filter { it.startsWith(input, ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun resolveTarget(identifier: String): OfflinePlayer? {
        Bukkit.getPlayerExact(identifier)?.let { return it }
        Bukkit.getOfflinePlayers()
            .firstOrNull { it.name.equals(identifier, ignoreCase = true) }
            ?.let { return it }
        val uuid = runCatching { UUID.fromString(identifier) }.getOrNull() ?: return null
        return Bukkit.getOfflinePlayer(uuid)
    }
}
