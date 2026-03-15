package pl.syntaxdevteam.punisher.hooks

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
//import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeButton
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeEmbed
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeEmbedField
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeEmbedPlaceholderField
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeMessageRequest
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeSlashCommand
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeSlashCommandOption
import pl.syntaxdevteam.dscbridgeapi.external.model.BridgeSubmitResult
import pl.syntaxdevteam.dscbridgeapi.external.model.ButtonStyle
import pl.syntaxdevteam.dscbridgeapi.external.model.render
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.text.trim

class DiscordBridge(var plugin: PunisherX) {
    private val namespace = "punisherx"
    private val correlationContext = ConcurrentHashMap<String, ModerationContext>()
    private val gateway = plugin.hookHandler.getDscBridgeGateway()
    private val discordConfig = plugin.config.getConfigurationSection("dscbridge.discord")

    fun startDscBridge() {
        if (gateway == null) {
            plugin.logger.warning("BridgeGateway niedostępne — funkcje sterowania botem wyłączone.")
            //plugin.server.pluginManager.disablePlugin(this)
            return
        }

        val commandName = discordConfig?.getString("command_name")?.trim().orEmpty().ifBlank { "punish" }
        val commandDescription = discordConfig?.getString("command_description")?.trim().orEmpty()
            .ifBlank { "Pokaż panel moderacyjny dla wskazanego gracza" }
        val commandOptions = loadCommandOptions()
        val targetOptionName = commandOptions.firstOrNull()?.name ?: "nick"
        val allowedRoleIds = readStringList(discordConfig, "role_ids", listOf("123456789012345678", "987654321098765432"))
        val allowedChannelId = discordConfig?.getString("channel_id")?.trim().orEmpty().ifBlank { "112233445566778899" }
        val panelTemplate = loadEmbedTemplate()
        val messageContentTemplate = discordConfig?.getString("content") ?: "Panel karny dla {nick}"
        val configuredButtons = loadButtons()
        val interactionCommands = configuredButtons.associate { button ->
            "$namespace.${button.id}" to button.commandTemplate
        }

        val slashRegistered = gateway.registerSlashCommand(
            pluginNamespace = namespace,
            command = BridgeSlashCommand(
                name = commandName,
                description = commandDescription,
                options = commandOptions,
                allowedRoleIds = allowedRoleIds,
                allowedChannelId = allowedChannelId
            )
        )

        if (!slashRegistered) {
            plugin.logger.warning("Nie udało się zarejestrować /$commandName (kolizja nazwy, błąd walidacji lub problem transportu).")
        }

        gateway.registerSlashCommandHandler(namespace) { event ->
            if (event.commandName != commandName) return@registerSlashCommandHandler

            val isAllowedRole = event.memberRoleIds.any { it in allowedRoleIds }
            val isAllowedChannel = event.channelId == allowedChannelId
            if (!isAllowedRole || !isAllowedChannel) {
                plugin.logger.warning(
                    "Odrzucono /$commandName: userId=${event.userId}, channelId=${event.channelId}, roles=${event.memberRoleIds}"
                )
                return@registerSlashCommandHandler
            }

            val nick = event.options[targetOptionName]?.trim().orEmpty()
            if (nick.isBlank()) {
                plugin.logger.warning("/$commandName bez parametru $targetOptionName od userId=${event.userId}")
                return@registerSlashCommandHandler
            }

            val snapshot = loadPlayerSnapshot(nick)

            val placeholders = mapOf(
                "nick" to nick,
                "player" to nick,
                "operator" to event.userId,
                "operator_discord_id" to event.userId,
                "health" to snapshot.health,
                "food" to snapshot.food,
                "level" to snapshot.level,
                "world" to snapshot.world,
                "location" to snapshot.location,
                "ping" to snapshot.ping,
                "uuid" to snapshot.uuid
            )

            val renderedEmbed = panelTemplate.render(placeholders)
            val content = applyPlaceholders(messageContentTemplate, placeholders)

            val submission = gateway.submitMessage(
                BridgeMessageRequest(
                    pluginNamespace = namespace,
                    channelId = event.channelId,
                    content = content,
                    embed = renderedEmbed,
                    buttons = configuredButtons.map { button ->
                        BridgeButton(
                            customId = "$namespace.${button.id}",
                            label = button.label,
                            style = button.style
                        )
                    }
                )
            )

            when (submission) {
                is BridgeSubmitResult.Accepted -> {
                    correlationContext[submission.correlationId] = ModerationContext(
                        nick = nick,
                        commandAuthorDiscordId = event.userId,
                        createdAtMs = nowMs()
                    )
                    purgeExpiredContexts()
                    plugin.logger.debug("Panel moderacyjny utworzony, correlationId=${submission.correlationId}, nick=$nick")
                }

                is BridgeSubmitResult.Rejected -> plugin.logger.warning("Nie udało się utworzyć panelu: ${submission.error.code}")
            }
        }

        gateway.registerInteractionHandler(namespace) { interaction ->
            val context = resolveContext(interaction.correlationId) ?: run {
                plugin.logger.warning("Brak kontekstu dla correlationId=${interaction.correlationId}; odrzucam akcję ${interaction.customId}")
                return@registerInteractionHandler
            }

            val commandTemplate = interactionCommands[interaction.customId]
            if (commandTemplate == null) {
                plugin.logger.warning("Nieznany customId interakcji: ${interaction.customId}")
                return@registerInteractionHandler
            }

            val command = applyPlaceholders(
                commandTemplate,
                mapOf(
                    "nick" to context.nick,
                    "player" to context.nick,
                    "operator_discord_id" to context.commandAuthorDiscordId,
                    "command_author_discord_id" to context.commandAuthorDiscordId
                )
            )

            dispatchServerCommand(command)
        }
    }

    private fun dispatchServerCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

    private fun loadCommandOptions(): List<BridgeSlashCommandOption> {
        val parsed = discordConfig
            ?.getMapList("command_options")
            ?.mapNotNull { raw ->
                val name = raw["name"]?.toString()?.trim().orEmpty()
                val description = raw["description"]?.toString()?.trim().orEmpty()
                if (name.isBlank() || description.isBlank()) return@mapNotNull null
                BridgeSlashCommandOption(name = name, description = description)
            }
            ?.takeIf { it.isNotEmpty() }

        return parsed ?: defaultCommandOptions
    }

    private fun loadButtons(): List<ConfiguredButton> {
        val parsed = discordConfig
            ?.getMapList("buttons")
            ?.mapNotNull { raw ->
                val id = raw["id"]?.toString()?.trim().orEmpty()
                val label = raw["label"]?.toString()?.trim().orEmpty()
                val command = raw["command"]?.toString()?.trim().orEmpty()
                if (id.isBlank() || label.isBlank() || command.isBlank()) return@mapNotNull null

                ConfiguredButton(
                    id = id,
                    label = label,
                    style = parseButtonStyle(raw["style"]?.toString()),
                    commandTemplate = command
                )
            }
            ?.takeIf { it.isNotEmpty() }

        return parsed ?: defaultButtons
    }

    private fun parseButtonStyle(rawStyle: String?): ButtonStyle {
        val normalized = rawStyle?.trim()?.uppercase().orEmpty()
        return when (normalized) {
            "PRIMARY" -> ButtonStyle.PRIMARY
            "SECONDARY" -> ButtonStyle.SECONDARY
            "SUCCESS" -> ButtonStyle.SUCCESS
            "DANGER" -> ButtonStyle.DANGER
            else -> ButtonStyle.SECONDARY
        }
    }

    private fun applyPlaceholders(template: String, values: Map<String, String>): String {
        var result = template
        values.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }

    private fun loadEmbedTemplate(): BridgeEmbed {
        val embedSection = discordConfig?.getConfigurationSection("embed")

        val fields = embedSection
            ?.getMapList("fields")
            ?.mapNotNull { raw ->
                val name = raw["name"]?.toString()?.trim().orEmpty()
                val value = raw["value"]?.toString().orEmpty()
                if (name.isBlank() || value.isBlank()) return@mapNotNull null
                BridgeEmbedField(
                    name = name,
                    value = value,
                    inline = raw["inline"]?.toString()?.toBooleanStrictOrNull() ?: false
                )
            }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultFields

        val placeholderFields = embedSection
            ?.getMapList("placeholder_fields")
            ?.mapNotNull { raw ->
                val name = raw["name"]?.toString()?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null

                val values = when (val rawValue = raw["value"]) {
                    is List<*> -> rawValue.mapNotNull { it?.toString() }
                    is String -> listOf(rawValue)
                    else -> emptyList()
                }

                if (values.isEmpty()) return@mapNotNull null
                BridgeEmbedPlaceholderField(name = name, value = values)
            }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultPlaceholderFields

        return BridgeEmbed(
            title = embedSection?.getString("title") ?: "👤 PROFIL GRACZA: {nick}",
            description = embedSection?.getString("description") ?: "Panel moderacyjny PunisherX",
            url = embedSection?.getString("url") ?: "https://example.com/cases/{nick}",
            timestamp = embedSection?.getString("timestamp") ?: "now",
            thumbnailUrl = embedSection?.getString("thumbnail-url") ?: "{nick}",
            imageUrl = embedSection?.getString("image-url") ?: "https://mc-heads.net/body/{nick}",
            authorName = embedSection?.getConfigurationSection("author")?.getString("name") ?: "{operator}",
            footer = embedSection?.getConfigurationSection("footer")?.getString("text") ?: "PunisherX • Paper",
            color = discordConfig?.getString("colors") ?: "9447935",
            placeholderFields = placeholderFields,
            fields = fields
        )
    }

    private fun readStringList(section: ConfigurationSection?, path: String, default: List<String>): List<String> {
        if (section == null) return default

        val fromList = section.getStringList(path)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (fromList.isNotEmpty()) return fromList

        val raw = section.get(path)
        if (raw is String) {
            val fromCsv = raw
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (fromCsv.isNotEmpty()) return fromCsv
        }

        return default
    }

    private fun resolveContext(correlationId: String): ModerationContext? {
        val context = correlationContext[correlationId] ?: return null
        if (nowMs() - context.createdAtMs > contextTtl.toMillis()) {
            correlationContext.remove(correlationId)
            return null
        }
        return context
    }

    private fun purgeExpiredContexts() {
        val cutoff = nowMs() - contextTtl.toMillis()
        correlationContext.entries.removeIf { (_, value) -> value.createdAtMs < cutoff }
    }

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun loadPlayerSnapshot(nick: String): PlayerSnapshot {
        val uuid = plugin.resolvePlayerUuid(nick)
        val online: Player? = Bukkit.getPlayer(uuid)

        if (online != null && online.isOnline) {
            val loc = online.location
            val worldName = loc.world?.name ?: "unknown"
            val x = loc.blockX
            val y = loc.blockY
            val z = loc.blockZ

            val maxHealth = online.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
            val health = "${online.health.toInt()}/${maxHealth.toInt()}"
            val food = "${online.foodLevel}/20"
            val level = online.level.toString()

            val ping = runCatching { "${online.ping}ms" }.getOrDefault("N/A")
            val playTicks = runCatching { online.getStatistic(Statistic.PLAY_ONE_MINUTE) }.getOrDefault(0)
            val playTime = formatPlayTime(playTicks.toLong())
            val lastSeen = "Online teraz"

            return PlayerSnapshot(
                location = "X:$x Y:$y Z:$z",
                world = worldName,
                health = health,
                food = food,
                level = level,
                ping = ping,
                uuid = uuid.toString(),
                playTime = playTime,
                lastSeen = lastSeen
            )
        }

        //val offline: OfflinePlayer = Bukkit.getOfflinePlayer(uuid)

        val playTime = PlayerStatsService.getTotalPlaytimeString(uuid)
            ?: PlayerStatsService.getCurrentOnlineString(uuid)
            ?: "Brak danych"

        val lastSeen = PlayerStatsService.getLastActiveString(uuid)
            ?: "Brak danych"

        val lastLocationString = PlayerStatsService.getLastLocationString(uuid)
            ?: "Offline"

        val (worldName, coords) = if (lastLocationString.contains(":")) {
            val parts = lastLocationString.split(":", limit = 2)
            parts[0].trim() to parts.getOrNull(1)?.trim().orEmpty()
        } else {
            "Brak danych" to lastLocationString
        }

        return PlayerSnapshot(
            location = coords.ifBlank { "Offline" },
            world = worldName,
            health = "Brak danych",
            food = "Brak danych",
            level = "Brak danych",
            ping = "Offline",
            uuid = uuid.toString(),
            playTime = playTime,
            lastSeen = lastSeen
        )
    }

    private fun formatPlayTime(ticks: Long): String {
        val totalSeconds = ticks / 20
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}s"
        }
    }

    private data class ConfiguredButton(
        val id: String,
        val label: String,
        val style: ButtonStyle,
        val commandTemplate: String
    )

    private companion object {
        val contextTtl: Duration = Duration.ofHours(6)

        val defaultFields = listOf(
            BridgeEmbedField(name = "📊 STATYSTYKI LIVE", value = "{statsField}", inline = true),
            BridgeEmbedField(name = "📍 LOKALIZACJA", value = "{locationField}", inline = true),
            BridgeEmbedField(name = "📡 POŁĄCZENIE", value = "{connectionField}", inline = false)
        )

        val defaultPlaceholderFields = listOf(
            BridgeEmbedPlaceholderField(
                name = "statsField",
                value = listOf(
                    "❤️ Zdrowie: {health}",
                    "🍗 Głód: {food}",
                    "⭐ Poziom XP: {level}"
                )
            ),
            BridgeEmbedPlaceholderField(
                name = "locationField",
                value = listOf(
                    "🗺️ Świat: {world}",
                    "📍 {location}"
                )
            ),
            BridgeEmbedPlaceholderField(
                name = "connectionField",
                value = listOf(
                    "📶 Ping: {ping}",
                    "🆔 UUID: `{uuid}`"
                )
            )
        )

        val defaultCommandOptions = listOf(
            BridgeSlashCommandOption(
                name = "nick",
                description = "Nick gracza do ukarania"
            )
        )

        val defaultButtons = listOf(
            ConfiguredButton(
                id = "ban",
                label = "BANUJ",
                style = ButtonStyle.DANGER,
                commandTemplate = "ban {nick} 1h Złamanie regulaminu serwera."
            ),
            ConfiguredButton(
                id = "kick",
                label = "WYRZUĆ",
                style = ButtonStyle.PRIMARY,
                commandTemplate = "kick {nick} Naruszenie zasad serwera."
            ),
            ConfiguredButton(
                id = "mute",
                label = "WYCISZ",
                style = ButtonStyle.SECONDARY,
                commandTemplate = "mute {nick} 30m Toksyczne zachowanie."
            )
        )
    }
}
