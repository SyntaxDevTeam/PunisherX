package pl.syntaxdevteam.punisher.gui.player.action
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import java.util.Locale

class QuickPunishGUI(plugin: PunisherX) : BaseGUI(plugin) {

    companion object {
        private const val PAGE_SIZE = 45
    }

    fun open(player: Player, target: OfflinePlayer, page: Int = 0) {
        if (!PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.PUNISH)) {
            player.sendMessage(mH.stringMessageToComponent("error", "no_permission"))
            return
        }

        val targetName = target.name
        if (targetName.isNullOrBlank()) {
            player.sendMessage(mH.stringMessageToComponent("error", "no_data"))
            return
        }

        val templates = plugin.punishTemplateManager.getTemplateNames()
            .mapNotNull(plugin.punishTemplateManager::getTemplate)
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        plugin.schedulerAdapter.runAsync(Runnable {
            val history = plugin.databaseHandler.getPunishmentHistory(target.uniqueId.toString())
            val nextLevels = templates.associate { template ->
                template.name to (history.count { it.reason == template.reason } + 1)
            }

            plugin.schedulerAdapter.runForPlayer(player, Runnable {
                if (player.isOnline) {
                    show(player, target, targetName, templates, nextLevels, page)
                }
            })
        })
    }

    private fun show(
        player: Player,
        target: OfflinePlayer,
        targetName: String,
        templates: List<PunishTemplate>,
        nextLevels: Map<String, Int>,
        page: Int
    ) {
        val totalPages = maxOf(1, (templates.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val currentPage = page.coerceIn(0, totalPages - 1)
        val gui = createGui(6)

        if (templates.isEmpty()) {
            gui.setItem(
                22,
                createGuiItem(
                    Material.BARRIER,
                    mH.stringMessageToStringNoPrefix("error", "no_data")
                )
            )
        } else {
            templates
                .drop(currentPage * PAGE_SIZE)
                .take(PAGE_SIZE)
                .forEachIndexed { index, template ->
                    val nextLevel = nextLevels[template.name] ?: 1
                    val resolved = template.resolveLevel(nextLevel)
                    val lore = mutableListOf(
                        "<gray>${template.reason}</gray>"
                    )

                    if (resolved == null) {
                        lore += "<red>#$nextLevel ?</red>"
                        gui.setItem(
                            index,
                            createGuiItem(
                                Material.BARRIER,
                                "<red>${template.name}</red>",
                                lore
                            )
                        )
                        return@forEachIndexed
                    }

                    val time = resolved.time?.takeIf { it.isNotBlank() } ?: "∞"
                    lore += "<dark_gray>#$nextLevel</dark_gray> <gray>→</gray> <yellow>${resolved.type.uppercase(Locale.ROOT)}</yellow> <gray>$time</gray>"
                    lore += "<dark_gray>/punish $targetName ${template.name}</dark_gray>"

                    gui.setItem(
                        index,
                        createGuiItem(
                            materialFor(resolved.type),
                            "<yellow>${template.name}</yellow>",
                            lore
                        ) { clicker ->
                            clicker.closeInventory()
                            executePunish(clicker, targetName, template.name)
                        }
                    )
                }
        }

        if (currentPage > 0) {
            gui.setItem(
                45,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
                    open(clicker, target, currentPage - 1)
                }
            )
        }

        gui.setItem(
            49,
            createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
                PlayerActionGUI(plugin).open(clicker, target)
            }
        )

        if (currentPage < totalPages - 1) {
            gui.setItem(
                53,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
                    open(clicker, target, currentPage + 1)
                }
            )
        }

        gui.open(player)
    }

    private fun materialFor(type: String): Material {
        return when (type.uppercase(Locale.ROOT)) {
            "BAN" -> Material.RED_CONCRETE
            "BANIP" -> Material.REDSTONE_BLOCK
            "MUTE" -> Material.WRITABLE_BOOK
            "WARN" -> Material.PAPER
            "KICK" -> Material.IRON_BOOTS
            "JAIL" -> Material.IRON_BARS
            else -> Material.NAME_TAG
        }
    }

    private fun executePunish(player: Player, targetName: String, templateName: String) {
        val command = plugin.getCommand("punish")
        if (command != null) {
            command.execute(player, "punish", arrayOf(targetName, templateName))
            return
        }

        player.performCommand("punish $targetName $templateName")
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PlayerAction.punish")
    }
}
