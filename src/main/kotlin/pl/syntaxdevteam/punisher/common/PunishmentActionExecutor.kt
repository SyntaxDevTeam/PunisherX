package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.ConfigurationSection
import pl.syntaxdevteam.punisher.PunisherX

class PunishmentActionExecutor(private val plugin: PunisherX) {

    fun executeWarnCountActions(player: String, warnCount: Int) {
        val placeholders = mapOf(
            "player" to player,
            "warn_no" to warnCount.toString()
        )

        val modernSection = plugin.config.getConfigurationSection("actions.warn.count")
        if (modernSection != null) {
            executeThresholdCommands(modernSection, player, warnCount, placeholders)
            return
        }

        val legacySection = plugin.config.getConfigurationSection("warn.actions")
        if (legacySection != null) {
            executeThresholdCommands(legacySection, player, warnCount, placeholders)
        }
    }

    fun executeAction(actionKey: String, player: String, placeholders: Map<String, String> = emptyMap()) {
        val path = "actions.$actionKey"
        if (!plugin.config.contains(path)) {
            return
        }

        val mergedPlaceholders = placeholders + ("player" to player)
        dispatchCommands(plugin.config.get(path), player, mergedPlaceholders)
    }

    private fun executeThresholdCommands(
        section: ConfigurationSection,
        player: String,
        warnCount: Int,
        placeholders: Map<String, String>
    ) {
        section.getKeys(false).forEach { key ->
            val threshold = key.toIntOrNull() ?: return@forEach
            if (warnCount == threshold) {
                dispatchCommands(section.get(key), player, placeholders)
            }
        }
    }

    private fun dispatchCommands(value: Any?, player: String, placeholders: Map<String, String>) {
        val commands = when (value) {
            is String -> listOf(value)
            is Collection<*> -> value.filterIsInstance<String>()
            is ConfigurationSection -> value.getKeys(false).flatMap { nestedKey ->
                val nestedValue = value.get(nestedKey)
                when (nestedValue) {
                    is String -> listOf(nestedValue)
                    is Collection<*> -> nestedValue.filterIsInstance<String>()
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

        if (commands.isEmpty()) {
            return
        }

        commands.forEach { template ->
            var formattedCommand = template
            placeholders.forEach { (key, value) ->
                formattedCommand = formattedCommand.replace("{$key}", value)
            }
            if (formattedCommand.isBlank()) {
                return@forEach
            }

            val filteredCommand = formattedCommand.replace(" --force", "")
            plugin.server.dispatchCommand(plugin.server.consoleSender, formattedCommand)
            plugin.logger.debug("Executed command for $player: $filteredCommand")
        }
    }
}
