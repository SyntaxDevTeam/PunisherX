package pl.syntaxdevteam.punisher.hooks

import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.*

class DiscordSrvNotificationPolicyTest {
    private val config = YamlConfiguration()
    private val values = mapOf("player" to "Steve", "time" to "10m", "reason" to "Spam @everyone 192.0.2.1 2001:db8::1", "operator" to "Admin")
    private fun render(type: String = "mute", silent: Boolean = false, data: Map<String, String> = values) =
        DiscordSrvNotificationPolicy.render(config, type, data, silent) { key, args -> key + " " + args.values.joinToString(" ") }

    @Test fun `silent and disabled types never publish`() {
        assertNull(render(silent = true))
        config.set("integration.discordsrv.notifications.punishments.mute", false)
        assertNull(render())
        config.set("integration.discordsrv.enabled", false)
        assertNull(render("ban"))
    }

    @Test fun `public messages hide operator and remove mentions and IPs`() {
        val text = assertNotNull(render())
        assertTrue("Steve" in text && "10m" in text && "Spam" in text)
        assertFalse("Admin" in text || "@everyone" in text || "192.0.2.1" in text || "2001:db8::1" in text)
        assertFalse("192.0.2.1" in assertNotNull(render("banip", data = values + ("player" to "192.0.2.1"))))
    }

    @Test fun `reason and operator switches and removal formatting`() {
        config.set("integration.discordsrv.notifications.show-reason", false)
        config.set("integration.discordsrv.notifications.show-operator", true)
        val text = assertNotNull(render())
        assertFalse("Spam" in text)
        assertTrue("Admin" in text)
        assertFalse("10m" in assertNotNull(render("unmute")))
        assertNull(render("unknown"))
    }
}
