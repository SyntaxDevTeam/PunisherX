package pl.syntaxdevteam.punisher.hooks

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import pl.syntaxdevteam.punisher.PunisherX
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

/**
 * Class responsible for sending punishment notifications to a Discord channel via webhook
 */
class DiscordWebhook(plugin: PunisherX) {
    private val webhookUrl: String = plugin.config.getString("webhook.discord.url") ?: ""
    private val enabled: Boolean = plugin.config.getBoolean("webhook.discord.enabled", false)
    private val webhookSection = plugin.config.getConfigurationSection("webhook.discord")
    private val embedSection = webhookSection?.getConfigurationSection("embed")
    private val colorSection = webhookSection?.getConfigurationSection("colors")
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    private val log = plugin.logger
    private val mh = plugin.messageHandler

    private val namedColors = mapOf(
        "black" to 0x000000,
        "white" to 0xFFFFFF,
        "red" to 0xFF0000,
        "green" to 0x00FF00,
        "blue" to 0x0000FF,
        "yellow" to 0xFFFF00,
        "orange" to 0xFFA500,
        "purple" to 0x800080,
        "pink" to 0xFFC0CB,
        "cyan" to 0x00FFFF,
        "magenta" to 0xFF00FF,
        "gray" to 0x808080,
        "grey" to 0x808080,
        "brown" to 0xA52A2A,
    )

    private val defaultColors = mapOf(
        "ban" to 9447935,
        "mute" to 15158332,
        "warn" to 16753920,
        "kick" to 16776960,
        "default" to 8421504,
    )
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val nameRegex = Regex("^[A-Za-z0-9_]{3,16}$")

    /**
     * Sends a punishment notification to a Discord channel
     * @param playerName Player's name
     * @param adminName Administrator's name
     * @param reason Reason for the punishment
     * @param type Type of punishment (ban, mute, kick)
     * @param duration Duration of the punishment (in milliseconds)
     */
    fun sendPunishmentWebhook(
        playerName: String,
        adminName: String,
        reason: String,
        type: String,
        duration: Long
    ) {
        log.debug("Sending webhook...")
        log.debug("Webhook URL: $webhookUrl")
        log.debug("Webhook enabled: $enabled")

        if (!enabled || webhookUrl.isEmpty()) {
            log.debug("Webhook is disabled or URL is empty")
            return
        }

        val placeholders = buildPlaceholders(playerName, adminName, reason, type, duration)
        val fields = resolveFields(placeholders)

        val embed = JsonObject().apply {
            resolveString("title", mh.stringMessageToStringNoPrefix("webhook", "title"), placeholders)
                ?.let { addProperty("title", it) }
            resolveString("description", null, placeholders)
                ?.let { addProperty("description", it) }
            resolveString("url", null, placeholders)
                ?.let { addProperty("url", it) }
            addProperty("color", getColorForPunishmentType(type))
            resolveTimestamp()?.let { addProperty("timestamp", it) }
            if (fields.size() > 0) {
                add("fields", fields)
            }
            add("footer", JsonObject().apply {
                val defaultFooter = "${mh.stringMessageToStringNoPrefix("webhook", "app_name")}${LocalDateTime.now().format(formatter)}"
                val footerRaw = embedSection?.getString("footer.text")
                val footerText = when {
                    footerRaw != null -> footerRaw.takeIf { it.isNotBlank() }?.let { applyPlaceholders(it, placeholders) }
                    else -> applyPlaceholders(defaultFooter, placeholders)
                }
                footerText?.let { addProperty("text", it) }
                resolveImageUrl(embedSection?.getString("footer.icon-url"))
                    ?.let { iconUrl -> addProperty("icon_url", iconUrl) }
            })

            embedSection?.getConfigurationSection("author")?.let { authorSection ->
                val authorObject = JsonObject()

                resolveString("author.name", null, placeholders)
                    ?.let { authorObject.addProperty("name", it) }
                resolveImageUrl(authorSection.getString("icon-url"))
                    ?.let { authorObject.addProperty("icon_url", it) }
                if (authorObject.entrySet().isNotEmpty()) {
                    add("author", authorObject)
                }
            }

            resolveImageUrl(embedSection?.getString("thumbnail-url"))
                ?.let { url -> add("thumbnail", JsonObject().apply { addProperty("url", url) }) }
            resolveImageUrl(embedSection?.getString("image-url"))
                ?.let { url -> add("image", JsonObject().apply { addProperty("url", url) }) }
        }

        val json = JsonObject().apply {
            webhookSection?.getString("username")?.takeIf { it.isNotBlank() }?.let { addProperty("username", it) }
            resolveImageUrl(webhookSection?.getString("avatar-url"))
                ?.let { addProperty("avatar_url", it) }
            add("embeds", JsonArray().apply {
                add(embed)
            })
        }

        log.debug("Sending JSON: $json")
        sendWebhook(json.toString())
    }

    private fun createField(name: String, value: String, inline: Boolean): JsonObject {
        return JsonObject().apply {
            addProperty("name", name)
            addProperty("value", value)
            addProperty("inline", inline)
        }
    }

    private fun getColorForPunishmentType(type: String): Int {
        val key = type.lowercase()
        val color = getColorValue(key) ?: getColorValue("default") ?: defaultColors[key]
        return color ?: defaultColors.getValue("default")
    }

    private fun formatDuration(duration: Long): String {
        return if (duration == -1L) {
            mh.stringMessageToStringNoPrefix("formatTime", "undefined")
        } else {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(duration),
                ZoneId.systemDefault()
            )
            dateTime.format(formatter)
        }
    }

    private fun buildPlaceholders(
        playerName: String,
        adminName: String,
        reason: String,
        type: String,
        duration: Long
    ): Map<String, String> {
        return mapOf(
            "player" to playerName,
            "operator" to adminName,
            "reason" to reason,
            "type" to type.uppercase(),
            "time" to formatDuration(duration),
        )
    }

    private fun resolveString(path: String, fallback: String?, placeholders: Map<String, String>): String? {
        val raw = when {
            path.contains(".") -> embedSection?.getString(path)
            else -> embedSection?.getString(path)
        }
        val value = raw?.takeIf { it.isNotBlank() } ?: fallback
        if (value.isNullOrBlank()) return null
        return applyPlaceholders(value, placeholders)
    }

    private fun applyPlaceholders(text: String, placeholders: Map<String, String>): String {
        var output = text
        placeholders.forEach { (key, value) ->
            output = output.replace("{$key}", value)
        }
        return output
    }

    private fun resolveTimestamp(): String? {
        val raw = embedSection?.getString("timestamp")?.trim().orEmpty()
        if (raw.isBlank()) {
            return Instant.now().toString()
        }
        if (raw.equals("now", ignoreCase = true)) {
            return Instant.now().toString()
        }
        return try {
            Instant.parse(raw).toString()
        } catch (e: Exception) {
            log.debug("Invalid webhook timestamp format '$raw', falling back to now.")
            Instant.now().toString()
        }
    }

    private fun resolveFields(placeholders: Map<String, String>): JsonArray {
        val fields = JsonArray()
        val rawFields = embedSection?.get("fields")
        if (rawFields is List<*>) {
            for (entry in rawFields) {
                val fieldMap = (entry as? Map<*, *>)?.mapKeys { it.key.toString() } ?: continue
                val name = fieldMap["name"]?.toString()?.takeIf { it.isNotBlank() } ?: continue
                val value = fieldMap["value"]?.toString()?.takeIf { it.isNotBlank() } ?: continue
                val inline = (fieldMap["inline"] as? Boolean) ?: false
                fields.add(createField(applyPlaceholders(name, placeholders), applyPlaceholders(value, placeholders), inline))
            }
            if (fields.size() > 0) {
                return fields
            }
        }

        val fieldsSection = embedSection?.getConfigurationSection("fields")
        if (fieldsSection?.getBoolean("player", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "player"), placeholders.getValue("player"), true))
        }

        if (fieldsSection?.getBoolean("operator", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "operator"), placeholders.getValue("operator"), true))
        }

        if (fieldsSection?.getBoolean("type", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "type"), placeholders.getValue("type"), true))
        }

        if (fieldsSection?.getBoolean("reason", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "reason"), placeholders.getValue("reason"), false))
        }

        if (fieldsSection?.getBoolean("time", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "time"), placeholders.getValue("time"), true))
        }

        return fields
    }

    private fun getColorValue(type: String): Int? {
        val value = colorSection?.get(type) ?: return null

        return when (value) {
            is Number -> value.toInt()
            is String -> parseColorString(value)
            else -> null
        }
    }

    private fun parseColorString(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase()
        namedColors[lower]?.let { return it }

        val normalized = lower
            .removePrefix("#")
            .removePrefix("0x")

        normalized.toIntOrNull(16)?.let { return it }
        normalized.toIntOrNull()?.let { return it }

        return null
    }

    private fun resolveImageUrl(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (uuidRegex.matches(trimmed) || nameRegex.matches(trimmed)) {
            return "https://mc-heads.net/avatar/$trimmed"
        }
        val normalized = trimmed.replace("\n", "").replace("\r", "")
        return if (normalized.startsWith("data:")) {
            normalized
        } else {
            "data:image/png;base64,$normalized"
        }
    }

    private fun sendWebhook(content: String) {
        CompletableFuture.runAsync {
            try {
                log.debug("Attempting to send webhook asynchronously...")
                val uri = URI(webhookUrl)
                val connection = uri.toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(content)
                }

                val responseCode = connection.responseCode
                log.debug("Response code: $responseCode")

                if (responseCode != 204) {
                    log.debug("Error sending webhook. Response code: $responseCode")
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    log.debug("Error content: $errorStream")
                }

                connection.disconnect()
            } catch (e: Exception) {
                log.debug("Error occurred while sending webhook: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
