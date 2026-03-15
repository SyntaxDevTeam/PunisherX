package pl.syntaxdevteam.punisher.hooks

import org.bukkit.Bukkit
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
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.trim
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import kotlin.collections.set

class DiscordBridge(var plugin: PunisherX) {
    private val namespace = "punisherx"
    private val correlationContext = ConcurrentHashMap<String, ModerationContext>()
    private val gateway = plugin.hookHandler.getDscBridgeGateway()

    fun startDscBridge(){
        if (gateway == null) {
            plugin.logger.warning("BridgeGateway niedostępne — funkcje sterowania botem wyłączone.")
            //plugin.server.pluginManager.disablePlugin(this)
            return
        }

        val slashRegistered = gateway.registerSlashCommand(
            pluginNamespace = namespace,
            command = BridgeSlashCommand(
                name = "punish",
                description = "Pokaż panel moderacyjny dla wskazanego gracza",
                options = listOf(
                    BridgeSlashCommandOption(
                        name = "nick",
                        description = "Nick gracza do ukarania"
                    )
                ),
                allowedRoleIds = listOf("123456789012345678", "987654321098765432"),
                allowedChannelId = "112233445566778899"
            )
        )

        if (!slashRegistered) {
            plugin.logger.warning("Nie udało się zarejestrować /punish (kolizja nazwy, błąd walidacji lub problem transportu).")
        }

        gateway.registerSlashCommandHandler(namespace) { event ->
            if (event.commandName != "punish") return@registerSlashCommandHandler

            // Runtime guard (defense-in-depth): API już filtruje po rolach/kanałach,
            // ale plugin może chcieć własny audit/fallback.
            val allowedRoles = setOf("123456789012345678", "987654321098765432")
            val isAllowedRole = event.memberRoleIds.any { it in allowedRoles }
            val isAllowedChannel = event.channelId == "112233445566778899"
            if (!isAllowedRole || !isAllowedChannel) {
                plugin.logger.warning(
                    "Odrzucono /punish: userId=${event.userId}, channelId=${event.channelId}, roles=${event.memberRoleIds}"
                )
                return@registerSlashCommandHandler
            }

            val nick = event.options["nick"]?.trim().orEmpty()
            if (nick.isBlank()) {
                plugin.logger.warning("/punish bez parametru nick od userId=${event.userId}")
                return@registerSlashCommandHandler
            }

            val snapshot = loadPlayerSnapshot(nick)

            // Szablon embeda używa placeholder_fields i nowych pól: url/timestamp/author/color.
            val panelTemplate = BridgeEmbed(
                title = "👤 PROFIL GRACZA: {nick}",
                description = "Panel moderacyjny PunisherX",
                url = "https://example.com/cases/{nick}",
                timestamp = "now",
                thumbnailUrl = "{nick}", // nick/UUID/url/base64 wspierane przez bridge
                imageUrl = "https://mc-heads.net/body/{nick}",
                authorName = "{operator}",
                footer = "PunisherX • Paper",
                color = "9447935",
                placeholderFields = listOf(
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
                ),
                fields = listOf(
                    BridgeEmbedField(name = "📊 STATYSTYKI LIVE", value = "{statsField}", inline = true),
                    BridgeEmbedField(name = "📍 LOKALIZACJA", value = "{locationField}", inline = true),
                    BridgeEmbedField(name = "📡 POŁĄCZENIE", value = "{connectionField}", inline = false)
                )
            )

            val renderedEmbed = panelTemplate.render(
                mapOf(
                    "nick" to nick,
                    "operator" to event.userId,
                    "health" to snapshot.health,
                    "food" to snapshot.food,
                    "level" to snapshot.level,
                    "world" to snapshot.world,
                    "location" to snapshot.location,
                    "ping" to snapshot.ping,
                    "uuid" to snapshot.uuid
                )
            )

            val submission = gateway.submitMessage(
                BridgeMessageRequest(
                    pluginNamespace = namespace,
                    channelId = event.channelId,
                    content = "Panel karny dla $nick",
                    embed = renderedEmbed,
                    buttons = listOf(
                        BridgeButton(customId = "$namespace.ban", label = "BANUJ", style = ButtonStyle.DANGER),
                        BridgeButton(customId = "$namespace.kick", label = "WYRZUĆ", style = ButtonStyle.PRIMARY),
                        BridgeButton(customId = "$namespace.mute", label = "WYCISZ", style = ButtonStyle.SECONDARY)
                    )
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
                    plugin.logger.info("Panel moderacyjny utworzony, correlationId=${submission.correlationId}, nick=$nick")
                }

                is BridgeSubmitResult.Rejected -> plugin.logger.warning("Nie udało się utworzyć panelu: ${submission.error.code}")
            }
        }

        gateway.registerInteractionHandler(namespace) { interaction ->
            val context = resolveContext(interaction.correlationId) ?: run {
                plugin.logger.warning("Brak kontekstu dla correlationId=${interaction.correlationId}; odrzucam akcję ${interaction.customId}")
                return@registerInteractionHandler
            }

            when (interaction.customId) {
                "$namespace.ban" -> dispatchServerCommand("ban ${context.nick} 1h Złamanie regulaminu serwera.")
                "$namespace.kick" -> dispatchServerCommand("kick ${context.nick} Naruszenie zasad serwera.")
                "$namespace.mute" -> dispatchServerCommand("mute ${context.nick} 30m Toksyczne zachowanie.")
            }
        }
    }

    private fun dispatchServerCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
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

    private companion object {
        val contextTtl: Duration = Duration.ofHours(6)
    }
}