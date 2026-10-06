package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.input.GuiTextInputServices
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.Locale

enum class PunishPresetKind(
    val path: String,
    val titleKey: String,
    val maxLength: Int,
    val material: Material
) {
    TIME("gui.punish.times", "PunishTime.title", 32, Material.CLOCK),
    REASON("gui.punish.reasons", "PunishReason.title", 160, Material.PAPER)
}

data class PresetMutationResult(
    val success: Boolean,
    val message: String
)

class PunishPresetManager(private val plugin: PunisherX) {

    fun values(kind: PunishPresetKind): List<String> = synchronized(plugin.cfg) {
        plugin.cfg.config.getStringList(kind.path).toList()
    }

    fun add(kind: PunishPresetKind, rawValue: String): PresetMutationResult = synchronized(plugin.cfg) {
        val (value, error) = normalize(kind, rawValue)
        if (error != null || value == null) {
            return@synchronized PresetMutationResult(false, error ?: "Invalid preset value.")
        }

        val values = plugin.cfg.config.getStringList(kind.path).toMutableList()
        if (values.any { it.equals(value, ignoreCase = true) }) {
            return@synchronized PresetMutationResult(false, "This preset already exists.")
        }

        values += value
        persist(kind, values, "Preset added.")
    }

    fun update(kind: PunishPresetKind, oldValue: String, rawValue: String): PresetMutationResult = synchronized(plugin.cfg) {
        val (value, error) = normalize(kind, rawValue)
        if (error != null || value == null) {
            return@synchronized PresetMutationResult(false, error ?: "Invalid preset value.")
        }

        val values = plugin.cfg.config.getStringList(kind.path).toMutableList()
        val index = values.indexOf(oldValue)
        if (index < 0) {
            return@synchronized PresetMutationResult(false, "The preset no longer exists.")
        }
        if (values.indices.any { it != index && values[it].equals(value, ignoreCase = true) }) {
            return@synchronized PresetMutationResult(false, "This preset already exists.")
        }

        values[index] = value
        persist(kind, values, "Preset updated.")
    }

    fun remove(kind: PunishPresetKind, value: String): PresetMutationResult = synchronized(plugin.cfg) {
        val values = plugin.cfg.config.getStringList(kind.path).toMutableList()
        if (!values.remove(value)) {
            return@synchronized PresetMutationResult(false, "The preset no longer exists.")
        }

        persist(kind, values, "Preset removed.")
    }

    private fun persist(
        kind: PunishPresetKind,
        values: List<String>,
        successMessage: String
    ): PresetMutationResult {
        return try {
            plugin.cfg.config.set(kind.path, values)
            plugin.cfg.config.save()

            // Keep Bukkit's already-loaded view in sync without rewriting config.yml through Bukkit.
            plugin.config.set(kind.path, values)

            PresetMutationResult(true, successMessage)
        } catch (throwable: Throwable) {
            plugin.logger.err("Failed to save ${kind.path}: ${throwable.message}")
            plugin.reportError(throwable)
            PresetMutationResult(false, "Could not save the preset to config.yml.")
        }
    }

    private fun normalize(kind: PunishPresetKind, rawValue: String): Pair<String?, String?> {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null to "The value cannot be empty."
        if (trimmed.length > kind.maxLength) {
            return null to "The value is too long (max ${kind.maxLength} characters)."
        }

        return when (kind) {
            PunishPresetKind.TIME -> {
                val normalized = trimmed.lowercase(Locale.ROOT)
                if (normalized == "perm" || normalized == "permanent") {
                    "perm" to null
                } else {
                    try {
                        val seconds = plugin.timeHandler.parseTime(normalized)
                        if (seconds <= 0) null to "The punishment time must be greater than zero."
                        else normalized to null
                    } catch (_: NumberFormatException) {
                        null to "Invalid time. Use values such as 10m, 2h, 7d or perm."
                    }
                }
            }

            PunishPresetKind.REASON -> {
                if (trimmed.contains('\n') || trimmed.contains('\r')) {
                    null to "The reason must fit on one line."
                } else {
                    trimmed to null
                }
            }
        }
    }
}

class PunishPresetManageGUI(plugin: PunisherX) : BaseGUI(plugin) {
    private val manager = PunishPresetManager(plugin)

    fun open(
        player: Player,
        kind: PunishPresetKind,
        onBack: (Player) -> Unit
    ) {
        open(player, kind, 0, onBack)
    }

    fun open(
        player: Player,
        kind: PunishPresetKind,
        page: Int,
        onBack: (Player) -> Unit
    ) {
        if (!canManage(player)) {
            player.sendMessage(mH.stringMessageToComponent("error", "no_permission"))
            return
        }

        val values = manager.values(kind)
        val pageSize = 45
        val totalPages = maxOf(1, (values.size + pageSize - 1) / pageSize)
        val currentPage = page.coerceIn(0, totalPages - 1)
        val gui = createGui(6, title(kind))

        if (values.isEmpty()) {
            gui.setItem(22, createGuiItem(Material.BARRIER, "<gray>No presets configured.</gray>"))
        } else {
            values.drop(currentPage * pageSize).take(pageSize).forEachIndexed { index, value ->
                gui.setItem(
                    index,
                    createGuiItem(
                        kind.material,
                        "<yellow>${value}</yellow>",
                        listOf("<gray>Click to edit or remove.</gray>")
                    ) { clicker ->
                        PunishPresetEntryGUI(plugin).open(clicker, kind, value, currentPage, onBack)
                    }
                )
            }
        }

        if (currentPage > 0) {
            gui.setItem(
                45,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) {
                    open(it, kind, currentPage - 1, onBack)
                }
            )
        }

        gui.setItem(
            48,
            createNavGuiItem(Material.EMERALD, "<green>+</green> <white>Add preset</white>") { clicker ->
                requestValue(
                    player = clicker,
                    kind = kind,
                    initialValue = "",
                    onCancel = { cancelled -> open(cancelled, kind, currentPage, onBack) }
                ) { submitter, value ->
                    mutate(
                        submitter,
                        operation = { manager.add(kind, value) },
                        onComplete = { viewer -> open(viewer, kind, currentPage, onBack) }
                    )
                }
            }
        )

        gui.setItem(
            49,
            createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back"), onBack)
        )

        if (currentPage < totalPages - 1) {
            gui.setItem(
                53,
                createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) {
                    open(it, kind, currentPage + 1, onBack)
                }
            )
        }

        gui.open(player)
    }

    private fun requestValue(
        player: Player,
        kind: PunishPresetKind,
        initialValue: String,
        onCancel: (Player) -> Unit,
        onSubmit: (Player, String) -> Unit
    ) {
        GuiTextInputServices.create(plugin).request(
            player = player,
            title = title(kind),
            initialValue = initialValue,
            maxLength = kind.maxLength,
            multiline = false,
            onSubmit = onSubmit,
            onCancel = onCancel
        )
    }

    internal fun mutate(
        player: Player,
        operation: () -> PresetMutationResult,
        onComplete: (Player) -> Unit
    ) {
        plugin.schedulerAdapter.runAsync(Runnable {
            val result = operation()
            plugin.schedulerAdapter.runForPlayer(player, Runnable {
                val color = if (result.success) "green" else "red"
                player.sendMessage(mH.miniMessageFormat("<$color>${result.message}</$color>"))
                if (player.isOnline) onComplete(player)
            })
        })
    }

    internal fun title(kind: PunishPresetKind): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", kind.titleKey)
    }

    private fun canManage(player: Player): Boolean {
        return PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component = Component.text("Punishment presets")
}

private class PunishPresetEntryGUI(plugin: PunisherX) : BaseGUI(plugin) {
    private val manager = PunishPresetManager(plugin)

    fun open(
        player: Player,
        kind: PunishPresetKind,
        value: String,
        page: Int,
        onBack: (Player) -> Unit
    ) {
        if (!PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE)) {
            player.sendMessage(mH.stringMessageToComponent("error", "no_permission"))
            return
        }

        val gui = createGui(3, Component.text("Preset"))
        gui.setItem(13, createGuiItem(kind.material, "<yellow>${value}</yellow>"))
        gui.setItem(
            11,
            createGuiItem(Material.ANVIL, "<yellow>✎ Edit</yellow>") { clicker ->
                GuiTextInputServices.create(plugin).request(
                    player = clicker,
                    title = mH.stringMessageToComponentNoPrefix("GUI", kind.titleKey),
                    initialValue = value,
                    maxLength = kind.maxLength,
                    multiline = false,
                    onSubmit = { submitter, updated ->
                        PunishPresetManageGUI(plugin).mutate(
                            submitter,
                            operation = { manager.update(kind, value, updated) },
                            onComplete = { viewer ->
                                PunishPresetManageGUI(plugin).open(viewer, kind, page, onBack)
                            }
                        )
                    },
                    onCancel = { cancelled -> open(cancelled, kind, value, page, onBack) }
                )
            }
        )
        gui.setItem(
            15,
            createGuiItem(
                Material.TNT,
                "<red>✕ Remove</red>",
                listOf("<gray>This immediately removes the selected preset.</gray>")
            ) { clicker ->
                PunishPresetManageGUI(plugin).mutate(
                    clicker,
                    operation = { manager.remove(kind, value) },
                    onComplete = { viewer ->
                        PunishPresetManageGUI(plugin).open(viewer, kind, page, onBack)
                    }
                )
            }
        )
        gui.setItem(
            22,
            createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) {
                PunishPresetManageGUI(plugin).open(it, kind, page, onBack)
            }
        )
        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component = Component.text("Preset")
}
