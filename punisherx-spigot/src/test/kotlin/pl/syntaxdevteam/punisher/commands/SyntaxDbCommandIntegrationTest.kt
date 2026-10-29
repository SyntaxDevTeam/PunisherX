package pl.syntaxdevteam.punisher.commands

import org.bukkit.command.CommandSender
import org.mockito.kotlin.*

class SyntaxDbCommandIntegrationTest : SyntaxDbCommandContract() {
    override fun executeCommand(name: String, sender: CommandSender, args: Array<String>) {
        val command: BasicCommand = when (name) {
            "warn" -> WarnCommand(plugin)
            "mute" -> MuteCommand(plugin)
            "ban" -> BanCommand(plugin)
            "banip" -> BanIpCommand(plugin)
            "jail" -> JailCommand(plugin)
            "kick" -> KickCommand(plugin)
            "unwarn" -> UnWarnCommand(plugin)
            "unmute" -> UnMuteCommand(plugin)
            "unban" -> UnBanCommand(plugin)
            "clearall" -> ClearAllCommand(plugin)
            "change-reason" -> ChangeReasonCommand(plugin)
            "punish" -> PunishCommand(plugin)
            "unmark" -> UnmarkCommand(plugin)
            "unpunish" -> UnpunishCommand(plugin)
            "history" -> HistoryCommand(plugin, plugin.playerIPManager)
            "check" -> CheckCommand(plugin, plugin.playerIPManager)
            "banlist" -> BanListCommand(plugin)
            else -> error("Unknown command: $name")
        }
        val stack = CommandSourceStack(sender)
        command.execute(stack, args)
    }
}
