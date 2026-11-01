package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PunishBrigadierUsageTest {
    private class Fixture(allowed: Boolean = true) {
        val plugin = mock<PunisherX>()
        val source = mock<CommandSourceStack>()
        val sender = mock<CommandSender>()
        val usage = Component.text("Usage: /punish <player> <template> [level]")
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        init {
            whenever(source.sender).thenReturn(sender)
            whenever(sender.hasPermission(any<String>())).thenReturn(allowed)
            whenever(plugin.messageHandler).thenReturn(mock())
            whenever(plugin.messageHandler.stringMessageToComponent(eq("punish"), eq("usage"), any<Map<String, String>>()))
                .thenReturn(usage)
            dispatcher.root.addChild(CommandManager(plugin).createPunishNode())
        }
    }

    @Test fun `bare punish and missing template display localized usage through registered tree`() {
        for (input in listOf("punish", "punish Steve")) {
            val f = Fixture()
            assertEquals(1, f.dispatcher.execute(input, f.source))
            verify(f.sender).sendMessage(f.usage)
            verify(f.plugin, never()).resolvePlayerUuid(any())
            verify(f.plugin, never()).databaseHandler
            verify(f.plugin, never()).punishTemplateManager
        }
    }

    @Test fun `usage handlers retain command permission gate`() {
        val f = Fixture(false)
        assertFailsWith<CommandSyntaxException> { f.dispatcher.execute("punish", f.source) }
        verify(f.sender, never()).sendMessage(any<Component>())
    }
}
