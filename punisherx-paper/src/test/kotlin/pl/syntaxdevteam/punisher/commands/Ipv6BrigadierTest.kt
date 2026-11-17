package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.commands.arguments.IpAddressArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDurationArgumentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Ipv6BrigadierTest {
    @Test fun `banip and unban native trees retain IPv6 duration reason and flags`() {
        val plugin = mock<PunisherX>()
        val sender = mock<CommandSender>()
        val source = mock<CommandSourceStack>()
        whenever(source.sender).thenReturn(sender)
        whenever(sender.hasPermission(any<String>())).thenReturn(true)
        for (ip in listOf("2001:db8::1", "::1", "2001:db8:0:0:0:0:0:1", "192.0.2.1")) {
            val ban = spy(BanIpCommand(plugin))
            doNothing().whenever(ban).execute(any(), any())
            val unban = spy(UnBanCommand(plugin))
            doNothing().whenever(unban).execute(any(), any())
            val dispatcher = CommandDispatcher<CommandSourceStack>()
            for ((command, name) in listOf(ban to "banip", unban to "unban")) {
                val node = (command as BrigadierCommand).build(name)
                BrigadierCommandUtils.addTrailingFlags(node, command as BasicCommand)
                dispatcher.root.addChild(node)
            }
            dispatcher.execute("banip $ip 1d test reason --silent --mark=check", source)
            verify(ban).execute(eq(source), eq(arrayOf(ip, "1d", "test", "reason", "--silent", "--mark=check")))
            dispatcher.execute("unban $ip --silent", source)
            verify(unban).execute(eq(source), eq(arrayOf(ip, "--silent")))
        }
    }

    @Test fun `IPv6 unban reaches address lookup rather than nickname resolver`() {
        val plugin = mock<PunisherX>()
        val sender = mock<CommandSender>()
        val source = mock<CommandSourceStack>()
        whenever(source.sender).thenReturn(sender)
        whenever(sender.hasPermission(any<String>())).thenReturn(true)
        whenever(plugin.databaseHandler).thenReturn(mock())
        whenever(plugin.logger).thenReturn(mock())
        whenever(plugin.messageHandler).thenReturn(mock())
        UnBanCommand(plugin).execute(source, arrayOf("2001:db8::1"))
        verify(plugin.databaseHandler).getPunishmentsByIP("2001:db8::1")
        verify(plugin, never()).resolvePlayerUuid(any())
    }

    @Test fun `IPv6 address parser stops at whitespace and duration rejects overflow`() {
        val reader = StringReader("2001:db8::1 1d")
        assertEquals("2001:db8::1", IpAddressArgumentType.ipAddress().parse(reader))
        assertEquals(" 1d", reader.remaining)
        assertEquals(86400L, PunishmentDurationArgumentType.parseRaw("1D")?.seconds)
        assertNull(PunishmentDurationArgumentType.parseRaw("9223372036854775807d"))
    }
}
