package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UnpunishCommandTest {
    private val plugin = mock<PunisherX>()
    private val source = mock<CommandSourceStack>()
    private val sender = mock<ConsoleCommandSender>()
    private val uuid = UUID.randomUUID()
    private val template = PunishTemplate("Chat spam", "Spam", emptyMap())
    private val entry = PunishmentData(42, uuid.toString(), "MUTE", "Spam", 100, -1, "Target", "Admin", "network")

    init {
        whenever(source.sender).thenReturn(sender)
        whenever(sender.name).thenReturn("Reviewer")
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>()))
            .thenReturn(Component.text("message"))
        whenever(plugin.punishTemplateManager).thenReturn(mock())
        whenever(plugin.punishTemplateManager.getTemplate("Chat spam")).thenReturn(template)
        whenever(plugin.databaseHandler).thenReturn(mock())
        whenever(plugin.commandLoggerPlugin).thenReturn(mock())
        whenever(plugin.resolvePlayerUuid("Target")).thenReturn(uuid)
    }

    @Test fun `quoted template is resolved and newest punishment is undone using template identity`() {
        whenever(plugin.databaseHandler.undoPunishment(uuid.toString(), "Target", "Spam", "Chat spam", "Reviewer"))
            .thenReturn(listOf(entry))
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "\"Chat", "spam\""))
        verify(plugin.databaseHandler).undoPunishment(uuid.toString(), "Target", "Spam", "Chat spam", "Reviewer")
        verify(plugin.commandLoggerPlugin).logCommand("Reviewer", "UNPUNISH", "Target", "Chat spam: history #42")
        verify(plugin.messageHandler).stringMessageToComponent("unpunish", "success",
            mapOf("player" to "Target", "template" to "Chat spam", "operator" to "Reviewer", "id" to "42"))
    }

    @Test fun `silent undo removes punishment and escalation without chat success`() {
        whenever(plugin.databaseHandler.undoPunishment(any(), any(), any(), any(), any())).thenReturn(listOf(entry))
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "Chat spam", "--silent"))
        verify(plugin.databaseHandler).undoPunishment(uuid.toString(), "Target", "Spam", "Chat spam", "Reviewer")
        verify(plugin.commandLoggerPlugin).logCommand("Reviewer", "UNPUNISH", "Target", "Chat spam: history #42")
        verifyNoInteractions(plugin.messageHandler)
        verify(sender, never()).sendMessage(any<Component>())
    }

    @Test fun `no matching history reports empty ladder without recording success`() {
        whenever(plugin.databaseHandler.undoPunishment(any(), any(), any(), any(), any())).thenReturn(emptyList())
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "Chat spam"))
        verify(plugin.messageHandler).stringMessageToComponent(eq("unpunish"), eq("not_found"), any<Map<String, String>>())
        verifyNoInteractions(plugin.commandLoggerPlugin)
    }

    @Test fun `database failure does not report success`() {
        whenever(plugin.databaseHandler.undoPunishment(any(), any(), any(), any(), any())).thenReturn(null)
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "Chat spam"))
        verify(plugin.messageHandler).stringMessageToComponent("unpunish", "db_error", emptyMap())
        verifyNoInteractions(plugin.commandLoggerPlugin)
    }

    @Test fun `unknown template never writes to database`() {
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "unknown"))
        verify(plugin.messageHandler).stringMessageToComponent("punish", "template_not_found", mapOf("template" to "unknown"))
        verifyNoInteractions(plugin.databaseHandler)
    }

    @Test fun `missing arguments report usage`() {
        UnpunishCommand(plugin).execute(source, arrayOf("Target"))
        verify(plugin.messageHandler).stringMessageToComponent("unpunish", "usage", emptyMap())
        verifyNoInteractions(plugin.databaseHandler)
    }

    @Test fun `permission is required for execution and suggestions`() {
        val unauthorized = mock<CommandSender>()
        whenever(source.sender).thenReturn(unauthorized)
        val command = UnpunishCommand(plugin)
        command.execute(source, arrayOf("Target", "Chat spam"))
        assertEquals(emptyList(), command.suggest(source, arrayOf("Target", "")))
        verify(plugin.messageHandler).stringMessageToComponent("error", "no_permission", emptyMap())
        verifyNoInteractions(plugin.databaseHandler)
    }

    @Test fun `another remaining jail prevents release`() {
        val jail = entry.copy(type = "JAIL")
        whenever(plugin.databaseHandler.undoPunishment(any(), any(), any(), any(), any())).thenReturn(listOf(jail))
        whenever(plugin.databaseHandler.getPunishments(uuid.toString())).thenReturn(listOf(jail.copy(id = 43)))
        whenever(plugin.cache).thenReturn(mock())
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "Chat spam"))
        verifyNoInteractions(plugin.cache)
    }

    @Test fun `last jail is released without deleting another database punishment`() {
        whenever(plugin.databaseHandler.undoPunishment(any(), any(), any(), any(), any())).thenReturn(listOf(entry.copy(type = "JAIL")))
        whenever(plugin.databaseHandler.getPunishments(uuid.toString())).thenReturn(emptyList())
        whenever(plugin.cache).thenReturn(mock())
        UnpunishCommand(plugin).execute(source, arrayOf("Target", "Chat spam"))
        verify(plugin.cache).removePunishment(uuid, teleportPlayer = true, notify = false, removeDatabase = false)
    }
}
