package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandTreeTest {
    private class Fixture(timed: Boolean = true) {
        val plugin = mock<PunisherX>()
        val source = mock<CommandSourceStack>()
        val sender = mock<CommandSender>()
        val command = mock<BasicCommand>()
        val config = YamlConfiguration()
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        init {
            whenever(source.sender).thenReturn(sender)
            whenever(sender.isOp).thenReturn(true)
            whenever(plugin.config).thenReturn(config)
            val timeHandler = mock<TimeHandler>()
            whenever(timeHandler.parseTime(any())).thenAnswer {
                if (!it.getArgument<String>(0).matches(Regex("\\d+[smhd]"))) throw NumberFormatException()
                1L
            }
            whenever(plugin.timeHandler).thenReturn(timeHandler)
            config.set("command-completion.arguments.player", "игрок")
            dispatcher.root.addChild(
                CommandTree(plugin).punishment("ban", "ban", PermissionChecker.PermissionKey.BAN, command, timed)
            )
        }

        fun run(input: String): List<String> {
            dispatcher.execute(input, source)
            val args = argumentCaptor<Array<String>>()
            verify(command).execute(eq(source), args.capture())
            return args.firstValue.toList()
        }
    }

    @Test fun `duration goes to time and the rest with flags to reason`() {
        assertEquals(listOf("Steve", "7d", "Griefing", "the", "spawn", "--silent"), Fixture().run("ban Steve 7d Griefing the spawn --silent"))
    }

    @Test fun `word that is not a duration starts the reason`() {
        assertEquals(listOf("Steve", "Griefing"), Fixture().run("ban Steve Griefing"))
    }

    @Test fun `time alone and player alone reach the command`() {
        assertEquals(listOf("Steve", "7d"), Fixture().run("ban Steve 7d"))
        assertEquals(listOf("Steve"), Fixture().run("ban Steve"))
    }

    @Test fun `untimed command reads a duration as reason`() {
        assertEquals(listOf("Steve", "7d"), Fixture(timed = false).run("ban Steve 7d"))
    }

    @Test fun `literals and multi word text reach the command as words`() {
        val f = Fixture()
        val player = CommandTree.Argument("player")
        val text = CommandTree.Argument("text", CommandTree.Kind.GREEDY)
        f.dispatcher.root.addChild(CommandTree(f.plugin).create("note", { true }, f.command, listOf(CommandTree.Literal("add"), player, text)))
        assertEquals(listOf("add", "Steve", "was", "rude"), f.run("note add Steve was rude"))
    }

    @Test fun `literal without its permission is not available`() {
        val f = Fixture()
        val manage = CommandTree.Literal("remove") { false }
        f.dispatcher.root.addChild(CommandTree(f.plugin).create("note", { true }, f.command, listOf(manage)))
        assertFailsWith<CommandSyntaxException> { f.dispatcher.execute("note remove", f.source) }
    }

    @Test fun `argument names come from config`() {
        assertEquals(listOf("игрок"), Fixture().dispatcher.root.getChild("ban").children.map { it.name })
    }
}
