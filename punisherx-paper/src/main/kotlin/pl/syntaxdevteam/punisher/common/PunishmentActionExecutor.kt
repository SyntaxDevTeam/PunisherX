package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.ConfigurationSection
import pl.syntaxdevteam.punisher.PunisherX

class PunishmentActionExecutor(private val plugin: PunisherX) {

    fun executeWarnCountActions(player: String, warnCount: Int, silent: Boolean = false) {
        val placeholders = mapOf(
            "player" to player,
            "warn_no" to warnCount.toString()
        )

        val modernSection = plugin.config.getConfigurationSection("actions.warn.count")
        if (modernSection != null) {
            executeThresholdCommands(modernSection, player, warnCount, placeholders, silent)
            return
        }

        val legacySection = plugin.config.getConfigurationSection("warn.actions")
        if (legacySection != null) {
            executeThresholdCommands(legacySection, player, warnCount, placeholders, silent)
        }
    }

    fun executeAction(actionKey: String, player: String, placeholders: Map<String, String> = emptyMap(), silent: Boolean = false) {
        val path = "actions.$actionKey"
        if (!plugin.config.contains(path)) {
            return
        }

        val mergedPlaceholders = placeholders + ("player" to player)
        dispatchCommands(plugin.config.get(path), player, mergedPlaceholders, silent)
    }

    private fun executeThresholdCommands(
        section: ConfigurationSection,
        player: String,
        warnCount: Int,
        placeholders: Map<String, String>,
        silent: Boolean
    ) {
        val thresholds = section.getKeys(false).mapNotNull { it.toIntOrNull() }
        if (thresholds.isEmpty()) return

        val matched = thresholds.firstOrNull { it == warnCount }
            ?: thresholds.filter { it <= warnCount }.maxOrNull()
            ?: return

        dispatchCommands(section.get(matched.toString()), player, placeholders, silent)
    }

    private fun dispatchCommands(value: Any?, player: String, placeholders: Map<String, String>, silent: Boolean) {
        val commands = when (value) {
            is String -> listOf(value)
            is Collection<*> -> value.filterIsInstance<String>()
            is ConfigurationSection -> value.getKeys(false).flatMap { nestedKey ->
                when (val nestedValue = value.get(nestedKey)) {
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

            // Carry silence through PunisherX escalation without adding unsupported flags to other plugins.
            if (silent && isPunishmentCommand(formattedCommand)) {
                formattedCommand += " --silent"
            }
            val filteredCommand = formattedCommand.replace(" --force", "")
            plugin.server.dispatchCommand(plugin.server.consoleSender, formattedCommand)
            plugin.logger.debug("Executed command for $player: $filteredCommand")
        }
    }

    private fun isPunishmentCommand(command: String): Boolean {
        val label = command.trimStart().substringBefore(' ').removePrefix("/").lowercase()
        if (':' in label && label.substringBefore(':') != "punisherx") return false
        val name = label.substringAfter(':')
        val commands = setOf("kick", "jail", "warn", "mute", "ban", "banip", "punish",
            "unjail", "unwarn", "unmute", "unban", "unpunish", "clearall")
        return name in commands || commands.any { plugin.config.getString("aliases.$it")?.lowercase() == name }
    }

}
