package pl.syntaxdevteam.punisher.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.databases.SyntaxDbTestFixture
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import pl.syntaxdevteam.punisher.templates.PunishTemplateLevel
import kotlin.test.*

/** The platform adapters invoke actual command classes. Only Bukkit/services are mocked, never SyntaxDB. */
abstract class SyntaxDbCommandContract : SyntaxDbTestFixture() {
    protected val sender = mock<ConsoleCommandSender>()
    private val world = mock<World>()
    private val commands = listOf("warn", "mute", "ban", "banip", "jail", "kick", "unwarn", "unmute", "unban", "clearall", "change-reason", "punish", "unpunish", "unmark")
    protected abstract fun executeCommand(name: String, sender: CommandSender, args: Array<String>)

    @BeforeTest fun prepareCommands() {
        start()
        whenever(sender.name).thenReturn("Admin")
        whenever(sender.spigot()).thenReturn(mock())
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.getSmartMessage(any(), any(), any())).thenReturn(emptyList())
        whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>())).thenReturn(Component.empty())
        whenever(plugin.messageHandler.stringMessageToComponentNoPrefix(any(), any(), any<Map<String, String>>())).thenReturn(Component.empty())
        whenever(plugin.messageHandler.stringMessageToStringNoPrefix(any(), any(), any<Map<String, String>>())).thenReturn("label")
        whenever(plugin.messageHandler.miniMessageFormat(any())).thenReturn(Component.empty())
        whenever(plugin.timeHandler).thenReturn(TimeHandler({ _, key -> key }))
        whenever(plugin.commandLoggerPlugin).thenReturn(mock())
        whenever(plugin.actionExecutor).thenReturn(mock())
        whenever(plugin.proxyBridgeMessenger).thenReturn(mock())
        whenever(plugin.cache).thenReturn(mock())
        whenever(plugin.playerIPManager).thenReturn(mock())
        whenever(plugin.playerIPManager.getPlayerIPsByName(any())).thenReturn(listOf("192.0.2.1", "192.0.2.2"))
        whenever(plugin.playerIPManager.getPlayerIPsByUUID(any())).thenReturn(listOf("192.0.2.1", "192.0.2.2"))
        whenever(plugin.resolvePlayerUuid(any())).thenReturn(uuid)
        whenever(plugin.server.onlinePlayers).thenReturn(emptyList())
        whenever(plugin.punishTemplateManager).thenReturn(mock())
        config.set("jail.location.world", "world")
    }

    private fun execute(name: String, vararg args: String, actor: CommandSender = sender, online: Player? = null) {
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<World?> { Bukkit.getWorld("world") }.thenReturn(world)
            if (online != null) bukkit.`when`<Player?> { Bukkit.getPlayer(uuid) }.thenReturn(online)
            val offline = mock<OfflinePlayer>()
            whenever(offline.name).thenReturn("Target")
            bukkit.`when`<OfflinePlayer> { Bukkit.getOfflinePlayer(uuid) }.thenReturn(offline)
            executeCommand(name, actor, arrayOf(*args))
        }
    }

    private fun template(type: String, time: String? = "10m") {
        whenever(plugin.punishTemplateManager.getTemplate("spam")).thenReturn(
            PunishTemplate("spam", reason, mapOf(1 to PunishTemplateLevel(1, type, time))))
    }

    private fun assertStored(type: String, target: String = uuid.toString(), permanent: Boolean = false) {
        val active = handler.getPunishments(target).single()
        val history = handler.getPunishmentHistory(target).single()
        assertEquals(type, active.type); assertEquals(reason, active.reason)
        assertEquals("Admin", active.operator); assertEquals("survival", active.server)
        assertEquals(history.reason, active.reason); assertEquals(history.start, active.start); assertEquals(history.end, active.end)
        if (permanent) assertEquals(-1L, active.end) else assertTrue(active.end - active.start in 600_000L..600_100L)
    }

    @Test fun `warn command persists temporary punishment history and updated warning count`() {
        execute("warn", "Target", "10m", reason, "--silent")
        assertStored("WARN")
        verify(plugin.actionExecutor).executeWarnCountActions("Target", 1, silent = true)
    }

    @Test fun `warn without duration persists permanent punishment and full reason`() {
        execute("warn", "Target", reason, "--silent")
        assertStored("WARN", permanent = true)
    }

    @Test fun `mute persists temporary punishment and history`() {
        execute("mute", "Target", "10m", reason, "--silent")
        assertStored("MUTE")
        verify(plugin.actionExecutor).executeAction(eq("muted"), eq("Target"), any(), eq(true), isNull())
    }

    @Test fun `mute without duration is permanent`() {
        execute("mute", "Target", reason, "--silent"); assertStored("MUTE", permanent = true)
    }

    @Test fun `ban persists temporary punishment history and bridge notification`() {
        execute("ban", "Target", "10m", reason, "--silent")
        assertStored("BAN")
        val row = handler.getPunishments(uuid.toString()).single()
        verify(plugin.proxyBridgeMessenger).notifyBan(uuid, reason, row.end)
    }

    @Test fun `permanent ban retains minus one expiration`() {
        execute("ban", "Target", reason, "--silent"); assertStored("BAN", permanent = true)
    }

    @Test fun `banip persists every resolved address with history and bridge notification`() {
        execute("banip", "Target", "10m", reason, "--silent")
        for (ip in listOf("192.0.2.1", "192.0.2.2")) {
            assertStored("BANIP", ip)
            verify(plugin.proxyBridgeMessenger).notifyIpBan(eq(ip), eq(reason), any())
        }
        assertEquals(2, count("punishments")); assertEquals(2, count("punishmenthistory"))
    }

    @Test fun `offline jail persists punishment history and cache using the generated ID`() {
        execute("jail", "Target", "10m", reason, "--silent")
        assertStored("JAIL")
        verify(plugin.cache).addOrUpdatePunishment(eq(uuid), any(), isNull())
    }

    @Test fun `jail without location never persists a punishment`() {
        config.set("jail.location.world", null)
        execute("jail", "Target", "10m", reason, "--silent")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.cache)
    }

    @Test fun `kick history option persists only history without an active punishment`() {
        config.set("kick.history", true)
        execute("kick", "Target", reason, "--silent")
        assertEquals(0, count("punishments"))
        val history = handler.getPunishmentHistory(uuid.toString()).single()
        assertEquals("KICK", history.type); assertEquals(reason, history.reason); assertEquals(history.start, history.end)
    }

    @Test fun `kick with disabled history performs no database writes`() {
        execute("kick", "Target", reason, "--silent")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verify(plugin.actionExecutor).executeAction(eq("kicked"), eq("Target"), any(), eq(true), isNull())
    }

    @Test fun `unwarn removes latest warning only and retains history and other types`() {
        execute("warn", "Target", "10m", reason, "--silent")
        execute("warn", "Target", "10m", reason, "--silent")
        add("MUTE")
        execute("unwarn", "Target", "--silent")
        assertEquals(1, handler.getActiveWarnCount(uuid.toString()))
        assertEquals(2, count("punishments")); assertEquals(2, count("punishmenthistory"))
    }

    @Test fun `unmute removes active mutes and preserves warnings and history`() {
        execute("mute", "Target", reason, "--silent"); add("WARN")
        execute("unmute", "Target", "--silent")
        assertEquals(listOf("WARN"), handler.getPunishments(uuid.toString()).map { it.type })
        assertEquals(1, count("punishmenthistory"))
    }

    @Test fun `unban removes player ban and preserves other types`() {
        execute("ban", "Target", reason, "--silent"); add("MUTE")
        execute("unban", "Target", "--silent")
        assertEquals(listOf("MUTE"), handler.getPunishments(uuid.toString()).map { it.type })
        assertEquals(1, count("punishmenthistory"))
    }

    @Test fun `unban IP removes only the requested address`() {
        execute("banip", "Target", reason, "--silent")
        execute("unban", "192.0.2.1", "--silent")
        assertTrue(handler.getPunishmentsByIP("192.0.2.1").isEmpty())
        assertEquals(1, handler.getPunishmentsByIP("192.0.2.2").size)
    }

    @Test fun `clearall removes ban mute and warn in current scope and retains history`() {
        for (type in listOf("BAN", "MUTE", "WARN")) add(type)
        add("BAN", "creative")
        handler.addPunishmentHistory("Target", uuid.toString(), reason, "Admin", "BAN", 100, -1)
        execute("clearall", "Target", "--silent")
        assertTrue(handler.getPunishments(uuid.toString()).isEmpty())
        assertEquals(1, count("punishments")); assertEquals(1, count("punishmenthistory"))
    }

    @Test fun `change reason command updates persisted history and rejects invalid ID`() {
        handler.addPunishmentHistory("Target", uuid.toString(), "Old reason", "Admin", "BAN", 100, -1)
        execute("change-reason", "1", reason)
        assertEquals(reason, handler.getPunishmentHistory(uuid.toString()).single().reason)
        execute("change-reason", "invalid", "Bad reason")
        verify(plugin.messageHandler).stringMessageToComponent(eq("change-reason"), eq("invalid_id"), any<Map<String, String>>())
    }

    @Test fun `commands with missing arguments leave all persistence tables unchanged`() {
        for (command in commands) execute(command)
        for (table in listOf("punishments", "punishmenthistory", "bridge_events", "punishment_templates", "punishment_undos")) assertEquals(0, count(table), table)
    }

    @Test fun `commands without permissions never persist or invoke punishment actions`() {
        val denied = mock<CommandSender>()
        whenever(denied.spigot()).thenReturn(mock())
        for (command in commands) execute(command, "Target", "10m", reason, actor = denied)
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.actionExecutor, plugin.proxyBridgeMessenger)
    }

    @Test fun `warn mute and jail abort on real SQLite insertion failure`() {
        sql("CREATE TRIGGER reject_punishment BEFORE INSERT ON punishments BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        for (command in listOf("warn", "mute", "jail")) execute(command, "Target", "10m", reason, "--silent")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.actionExecutor, plugin.cache, plugin.discordWebhook)
        verify(plugin.messageHandler, times(3)).stringMessageToComponent(eq("error"), eq("db_error"), any<Map<String, String>>())
    }

    @Test fun `banip insert failure never writes history or notifies proxy`() {
        sql("CREATE TRIGGER reject_punishment BEFORE INSERT ON punishments BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        execute("banip", "Target", "10m", reason, "--silent")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.proxyBridgeMessenger, plugin.actionExecutor)
    }

    @Test fun `template mute records escalation and unpunish reverses it without deleting history`() {
        template("MUTE")
        execute("punish", "Target", "spam", "--silent")
        assertStored("MUTE")
        assertEquals(1, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
        execute("unpunish", "Target", "spam", "--silent")
        assertEquals(0, count("punishments")); assertEquals(1, count("punishmenthistory")); assertEquals(1, count("punishment_undos"))
        assertTrue(handler.getPunishmentHistory(uuid.toString()).single().undone)
        assertEquals(0, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
    }

    @Test fun `template warnings escalate from database history and clamp to last level`() {
        whenever(plugin.punishTemplateManager.getTemplate("spam")).thenReturn(PunishTemplate("spam", reason, mapOf(
            1 to PunishTemplateLevel(1, "WARN", "10m"), 2 to PunishTemplateLevel(2, "MUTE", "1h"))))
        execute("punish", "Target", "spam", "--silent")
        execute("punish", "Target", "spam", "--silent")
        execute("punish", "Target", "spam", "--silent")
        assertEquals(listOf("WARN", "MUTE", "MUTE"), handler.getPunishmentHistory(uuid.toString()).map { it.type })
        assertEquals(3, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
    }

    @Test fun `template IP bans deduplicate addresses and undo as one escalation event`() {
        template("BANIP")
        execute("punish", "Target", "spam", "--silent")
        assertEquals(2, count("punishments")); assertEquals(2, count("punishment_templates"))
        assertEquals(1, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
        execute("unpunish", "Target", "spam", "--silent")
        assertEquals(0, count("punishments")); assertEquals(2, count("punishment_undos"))
    }

    @Test fun `template jail ban warn and kick persist template identity`() {
        for (type in listOf("JAIL", "BAN", "WARN", "KICK")) {
            template(type)
            execute("punish", "Target", "spam", "1", "--silent")
        }
        assertEquals(4, count("punishment_templates")); assertEquals(4, count("punishmenthistory"))
        assertEquals(3, count("punishments"))
    }

    @Test fun `template invalid time missing template and invalid level never persist`() {
        execute("punish", "Target", "missing", "--silent")
        template("MUTE", "invalid")
        execute("punish", "Target", "spam", "--silent")
        execute("punish", "Target", "spam", "0", "--silent")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
    }

    @Test fun `template escalation read failure does not reset punishment to level one`() {
        template("MUTE"); sql("DROP TABLE punishmenthistory")
        execute("punish", "Target", "spam", "--silent")
        assertEquals(0, count("punishments"))
        verify(plugin.messageHandler).stringMessageToComponent(eq("punish"), eq("history_error"), any<Map<String, String>>())
    }

    @Test fun `template IP failure never falls back to a permanent vanilla ban`() {
        template("BANIP")
        sql("CREATE TRIGGER reject_punishment BEFORE INSERT ON punishments BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<ConsoleCommandSender> { Bukkit.getConsoleSender() }.thenReturn(sender)
            executeCommand("punish", sender, arrayOf("Target", "spam", "--silent"))
            bukkit.verify({ Bukkit.dispatchCommand(eq(sender), any()) }, never())
        }
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.proxyBridgeMessenger, plugin.actionExecutor)
    }

    @Test fun `partial template IP failure records and publishes only persisted IP`() {
        template("BANIP")
        sql("CREATE TRIGGER reject_one_ip BEFORE INSERT ON punishments WHEN NEW.uuid = '192.0.2.2' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        execute("punish", "Target", "spam", "--silent")
        assertEquals(1, count("punishments")); assertEquals(1, count("punishmenthistory"))
        assertEquals(1, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
        verify(plugin.proxyBridgeMessenger).notifyIpBan(eq("192.0.2.1"), eq(reason), any())
        verify(plugin.proxyBridgeMessenger, never()).notifyIpBan(eq("192.0.2.2"), any(), any())
        verifyNoInteractions(plugin.actionExecutor)
    }

    @Test fun `history command renders the requested database page only`() {
        repeat(12) { handler.addPunishmentHistory("Target", uuid.toString(), "History-$it", "Admin", "WARN", it.toLong(), -1) }
        execute("history", "Target", "2")
        val output = argumentCaptor<String>()
        verify(plugin.messageHandler, atLeastOnce()).miniMessageFormat(output.capture())
        val rows = output.allValues.filter { "History-" in it }
        assertEquals(2, rows.size)
        assertTrue(rows[0].contains("History-1")); assertTrue(rows[1].contains("History-0"))
    }

    @Test fun `check command renders requested type and excludes foreign scope`() {
        val visible = add("MUTE")
        add("BAN"); add("MUTE", "creative")
        execute("check", "Target", "mute")
        val output = argumentCaptor<String>()
        verify(plugin.messageHandler, atLeastOnce()).miniMessageFormat(output.capture())
        val rows = output.allValues.filter { "<white>#" in it }
        assertEquals(1, rows.size); assertContains(rows.single(), "#$visible"); assertContains(rows.single(), "MUTE")
    }

    @Test fun `banlist command reads active and history pages independently`() {
        val active = add("BAN")
        handler.addPunishmentHistory("Historical", uuid.toString(), "Old ban", "Admin", "BANIP", 1, -1)
        execute("banlist", "1")
        verify(plugin.messageHandler).stringMessageToComponentNoPrefix(eq("banlist"), eq("ban_list"), argThat<Map<String, String>> {
            this["id"] == active.toString() && this["player"] == "Target" && this["reason"] == reason
        })
        execute("banlist", "--h", "1")
        verify(plugin.messageHandler).stringMessageToComponentNoPrefix(eq("banlist"), eq("ban_list"), argThat<Map<String, String>> {
            this["player"] == "Historical" && this["type"] == "BANIP" && this["reason"] == "Old ban"
        })
    }

    @Test fun `all direct penalty commands support marks and unmark all preserves manual punishment`() {
        for (name in listOf("warn", "mute", "ban", "banip", "jail", "kick")) {
            execute(name, "Target", "10m", reason, "--mark=grim.speed", "--silent")
        }
        assertEquals(7, count("punishment_marks")) // BANIP stores one entry per IP.
        assertEquals(7, count("punishmenthistory"))
        execute("mute", "Target", reason)
        execute("unmark", "all", "grim.speed")
        assertEquals(1, count("punishments"))
        assertEquals("MUTE", handler.getPunishments(uuid.toString()).single().type)
        assertEquals(7, count("punishment_undos"))
        verify(plugin.cache).removePunishment(uuid, notify = false, removeDatabase = false)
    }

    @Test fun `pause blocks side effects of every direct command and resume restores punishments`() {
        execute("ban", "Target", reason, "--mark=speed")
        execute("unmark", "pause", "speed")
        clearInvocations(plugin.proxyBridgeMessenger, plugin.actionExecutor, plugin.cache)
        for (name in listOf("warn", "mute", "ban", "banip", "jail", "kick")) {
            execute(name, "Target", reason, "--mark=speed", "--force")
        }
        assertEquals(0, count("punishments")); assertEquals(1, count("punishmenthistory"))
        verifyNoInteractions(plugin.proxyBridgeMessenger, plugin.actionExecutor, plugin.cache)
        execute("unmark", "resume", "speed")
        execute("ban", "Target", reason, "--mark=speed")
        assertEquals(1, count("punishments"))
    }

    @Test fun `unmark player reverses all their marked check types but does not pause future penalties`() {
        execute("warn", "Target", reason, "--mark")
        execute("mute", "Target", reason, "--mark=fly")
        execute("unmark", "Target")
        assertEquals(0, count("punishments"))
        assertFalse(handler.isMarkPaused("default")); assertFalse(handler.isMarkPaused("fly"))
        execute("warn", "Target", reason, "--mark")
        assertEquals(1, count("punishments"))
    }

    @Test fun `unmark leaves another active jail in cache`() {
        execute("jail", "Target", reason, "--mark")
        execute("jail", "Target", reason)
        clearInvocations(plugin.cache)
        execute("unmark", "all")
        assertEquals(1, count("punishments"))
        verifyNoInteractions(plugin.cache)
    }

    @Test fun `marked template penalties retain check identity and undo escalation for all types`() {
        for (type in listOf("BAN", "BANIP", "MUTE", "WARN", "KICK", "JAIL")) {
            template(type)
            execute("punish", "Target", "spam", "1", "--mark=template.check")
        }
        assertEquals(7, count("punishment_marks")); assertEquals(7, count("punishment_templates"))
        execute("unmark", "all", "template.check")
        assertEquals(0, count("punishments"))
        assertEquals(0, handler.countEscalationHistory(uuid.toString(), "Target", reason, "spam"))
    }

    @Test fun `invalid or repeated marks never become plain penalties`() {
        for (flags in listOf(arrayOf("--mark="), arrayOf("--mark=bad/check"), arrayOf("--mark=a", "--mark=b"))) {
            execute("ban", "Target", reason, *flags)
        }
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.proxyBridgeMessenger)
    }

    @Test fun `failure storing a marked ban never invokes platform fallback or proxy disconnect`() {
        sql("CREATE TRIGGER fail_mark BEFORE INSERT ON punishment_marks BEGIN SELECT RAISE(ABORT, 'injected'); END")
        execute("ban", "Target", reason, "--mark")
        template("BAN")
        execute("punish", "Target", "spam", "--mark")
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.proxyBridgeMessenger)
    }

    @Test fun `failed marked jail write restores online target location and gamemode`() {
        sql("CREATE TRIGGER fail_mark BEFORE INSERT ON punishment_marks BEGIN SELECT RAISE(ABORT, 'injected'); END")
        for (command in listOf("jail", "punish")) {
            val target = mock<Player>()
            val original = org.bukkit.Location(world, 20.0, 70.0, 20.0)
            whenever(target.name).thenReturn("Target")
            whenever(target.uniqueId).thenReturn(uuid)
            whenever(target.location).thenReturn(original)
            whenever(target.gameMode).thenReturn(org.bukkit.GameMode.SURVIVAL)
            val teleport = mock<pl.syntaxdevteam.punisher.teleport.SafeTeleportService>()
            whenever(plugin.safeTeleportService).thenReturn(teleport)
            doAnswer { invocation -> invocation.getArgument<(Boolean) -> Unit>(2)(true); null }
                .whenever(teleport).teleportSafely(any(), any(), any())
            if (command == "punish") {
                template("JAIL")
                execute(command, "Target", "spam", "1", "--mark", online = target)
            } else execute(command, "Target", reason, "--mark", online = target)
            verify(teleport).teleportSafely(eq(target), eq(original), any())
            verify(target).gameMode = org.bukkit.GameMode.SURVIVAL
        }
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
        verifyNoInteractions(plugin.cache)
    }
}
