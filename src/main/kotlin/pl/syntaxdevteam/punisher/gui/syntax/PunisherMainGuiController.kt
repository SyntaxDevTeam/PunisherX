package pl.syntaxdevteam.punisher.gui.syntax

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.admin.AdminListGUI
import pl.syntaxdevteam.punisher.gui.admin.ConfigGUI
import pl.syntaxdevteam.punisher.gui.player.OfflinePlayerListGUI
import pl.syntaxdevteam.punisher.gui.player.PlayerListGUI
import pl.syntaxdevteam.punisher.gui.punishments.PunishedListGUI
import pl.syntaxdevteam.syntaxguiapi.components.NavigationComponents
import pl.syntaxdevteam.syntaxguiapi.core.GuiAction
import pl.syntaxdevteam.syntaxguiapi.core.GuiBase
import pl.syntaxdevteam.syntaxguiapi.core.GuiIcon
import pl.syntaxdevteam.syntaxguiapi.core.GuiLayout
import pl.syntaxdevteam.syntaxguiapi.core.GuiMaterial
import pl.syntaxdevteam.syntaxguiapi.paper.PaperGuiListener
import pl.syntaxdevteam.syntaxguiapi.paper.PaperGuiRenderer
import java.lang.management.ManagementFactory

class PunisherMainGuiController(private val plugin: PunisherX) {

    private val renderer = PaperGuiRenderer()

    val listener = PaperGuiListener { player, holder, action ->
        val gui = holder.gui as? PunisherMainScreen ?: return@PaperGuiListener
        when (action) {
            is GuiAction.SelectElement -> gui.handleSelection(action.globalIndex, player)
            else -> Unit
        }
    }

    fun open(player: Player) {
        renderer.open(player, PunisherMainScreen(plugin))
    }

    private class PunisherMainScreen(
        private val plugin: PunisherX,
    ) : GuiBase(
        title = plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.title")),
        size = 45,
    ) {

        private val entries = listOf(
            MenuEntry(
                slot = 4,
                material = GuiMaterial.PAPER,
                title = message("PunisherMain.serwerInfo.title"),
                loreProvider = { listOf(serverNameLine(), dailyLine(), tpsLine()) },
                onClick = { _, _ -> },
            ),
            MenuEntry(
                slot = 10,
                material = GuiMaterial.PLAYER_HEAD,
                title = message("PunisherMain.playerOnline.title"),
                loreProvider = { listOf(playerOnlineLine(), message("PunisherMain.playerOnline.clickToView")) },
            ) { _, player -> PlayerListGUI(plugin).open(player) },
            MenuEntry(
                slot = 16,
                material = GuiMaterial.BOOK,
                title = message("PunisherMain.playerOffline.title"),
                loreProvider = { listOf(playerOfflineLine(), message("PunisherMain.playerOffline.clickToView")) },
            ) { _, player -> OfflinePlayerListGUI(plugin).open(player) },
            MenuEntry(
                slot = 22,
                material = GuiMaterial.TRIAL_KEY,
                title = message("PunisherMain.adminOnline.title"),
            ) { _, player -> AdminListGUI(plugin).open(player) },
            MenuEntry(
                slot = 29,
                material = GuiMaterial.MACE,
                title = message("PlayerAction.list"),
            ) { _, player -> PunishedListGUI(plugin).open(player) },
            MenuEntry(
                slot = 33,
                material = GuiMaterial.BARRIER,
                title = message("PlayerAction.config"),
            ) { _, player -> ConfigGUI(plugin).open(player) },
        )

        override fun buildLayout(page: Int): GuiLayout {
            val slots = mutableMapOf<Int, GuiIcon>()
            for (slot in 0 until size) {
                slots[slot] = NavigationComponents.filler(" ")
            }
            entries.forEach { entry ->
                slots[entry.slot] = GuiIcon(
                    key = "punisher_main_${entry.slot}",
                    label = entry.title,
                    lore = entry.loreProvider(),
                    material = entry.material,
                )
            }
            return GuiLayout(
                title = title,
                size = size,
                slots = slots,
            )
        }

        override fun onClick(slot: Int): GuiAction {
            val index = entries.indexOfFirst { it.slot == slot }
            return if (index == -1) GuiAction.None else GuiAction.SelectElement(index)
        }

        fun handleSelection(index: Int, player: Player) {
            entries.getOrNull(index)?.onClick?.invoke(index, player)
        }

        private fun message(key: String, vars: Map<String, String> = emptyMap()): String {
            return plain(plugin.messageHandler.formatMixedTextToMiniMessage(
                plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars),
            ))
        }

        private fun serverNameLine(): String = message(
            "PunisherMain.serwerInfo.serverName",
            mapOf("servername" to plugin.getServerName()),
        )

        private fun dailyLine(): String = message(
            "PunisherMain.serwerInfo.daily",
            mapOf("daily" to plugin.databaseHandler.countTodayPunishments().toString()),
        )

        private fun tpsLine(): String {
            val uptimeSeconds = ManagementFactory.getRuntimeMXBean().uptime / 1000
            val time = plugin.timeHandler.formatTime(uptimeSeconds.toString())
            return message(
                "PunisherMain.serwerInfo.tps",
                mapOf("time" to time, "tps" to getServerTPS()),
            )
        }

        private fun playerOnlineLine(): String = message(
            "PunisherMain.playerOnline.online",
            mapOf("onlineplayers" to Bukkit.getOnlinePlayers().size.toString()),
        )

        private fun playerOfflineLine(): String = message(
            "PunisherMain.playerOffline.total",
            mapOf("totalplayers" to plugin.playerIPManager.getAllDecryptedRecords().size.toString()),
        )

        private fun getServerTPS(): String {
            return try {
                val method = Bukkit.getServer()::class.java.getMethod("getTPS")
                val tps = method.invoke(Bukkit.getServer()) as? DoubleArray
                if (tps != null && tps.isNotEmpty()) String.format("%.2f", tps[0]) else "N/A"
            } catch (_: Exception) {
                "N/A"
            }
        }

        companion object {
            private val serializer = PlainTextComponentSerializer.plainText()

            private fun plain(component: Component): String = serializer.serialize(component)
        }
    }

    private data class MenuEntry(
        val slot: Int,
        val material: GuiMaterial,
        val title: String,
        val loreProvider: () -> List<String> = { emptyList() },
        val onClick: (Int, Player) -> Unit,
    )
}
