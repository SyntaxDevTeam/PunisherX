package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class PunishReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer, type: String, time: String) {
        val reasons = PunishPresetManager(plugin).values(PunishPresetKind.REASON)
        openPage(player, target, type, time, reasons, 0)
    }

    private fun openPage(
        player: Player,
        target: OfflinePlayer,
        type: String,
        time: String,
        reasons: List<String>,
        page: Int
    ) {
        val gui = createGui(5)
        val slots = (10..16) + (19..25) + (28..34)
        val perPage = slots.size
        val totalPages = maxOf(1, (reasons.size + perPage - 1) / perPage)
        val currentPage = page.coerceIn(0, totalPages - 1)

        reasons.drop(currentPage * perPage).take(perPage).forEachIndexed { index, reason ->
            gui.setItem(slots[index], createGuiItem(Material.PAPER, "<yellow>$reason</yellow>") { clicker ->
                clicker.closeInventory()
                plugin.guiPunishmentService.apply(clicker, target, type, time, reason)
            })
        }

        if (currentPage > 0) {
            gui.setItem(
                36,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) {
                    openPage(it, target, type, time, reasons, currentPage - 1)
                }
            )
        }

        if (PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE)) {
            gui.setItem(
                39,
                createNavGuiItem(Material.COMPARATOR, "<gold>⚙</gold> <yellow>Manage reason presets</yellow>") { clicker ->
                    PunishPresetManageGUI(plugin).open(clicker, PunishPresetKind.REASON) { back ->
                        open(back, target, type, time)
                    }
                }
            )
        }

        gui.setItem(
            40,
            createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) {
                PunishTimeGUI(plugin).open(it, target, type)
            }
        )

        if (currentPage < totalPages - 1) {
            gui.setItem(
                44,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) {
                    openPage(it, target, type, time, reasons, currentPage + 1)
                }
            )
        }

        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishReason.title")
    }
}
