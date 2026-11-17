package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo
import org.mockito.kotlin.*
import net.kyori.adventure.text.Component
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.TimeHandler
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PunishmentMaxTimeTest {
    @Test fun `longest active valid cap wins without affecting other types`() {
        val sender = mock<Player>()
        val source = mock<CommandSourceStack>()
        val plugin = mock<PunisherX>()
        whenever(source.sender).thenReturn(sender)
        whenever(plugin.timeHandler).thenReturn(TimeHandler({ _, _ -> "days" }))
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>()))
            .thenReturn(Component.text("Limit"))
        val nodes = mapOf("1d" to true, "7d" to true, "30d" to false, "wrong" to true, "*" to true,
            "9223372036854775807d" to true)
        val permissions = nodes.map { (time, value) ->
            val node = "punisherx.max.ban.$time"
            whenever(sender.hasPermission(node)).thenReturn(value)
            PermissionAttachmentInfo(sender, node, null, value)
        }.toSet()
        whenever(sender.effectivePermissions).thenReturn(permissions)
        assertFalse(PunishmentCommandUtils.exceedsMaxTime(plugin, source, "ban", "7d"))
        assertTrue(PunishmentCommandUtils.exceedsMaxTime(plugin, source, "ban", "8d"))
        assertTrue(PunishmentCommandUtils.exceedsMaxTime(plugin, source, "ban", null))
        assertFalse(PunishmentCommandUtils.exceedsMaxTime(plugin, source, "mute", null))
        whenever(sender.effectivePermissions).thenReturn(emptySet())
        assertFalse(PunishmentCommandUtils.exceedsMaxTime(plugin, source, "ban", null))
    }
}
