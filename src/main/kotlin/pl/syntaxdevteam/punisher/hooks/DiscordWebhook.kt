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
    private val fieldsSection = embedSection?.getConfigurationSection("fields")
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

        val fields = JsonArray()

        if (fieldsSection?.getBoolean("player", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "player"), playerName, true))
        }

        if (fieldsSection?.getBoolean("operator", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "operator"), adminName, true))
        }

        if (fieldsSection?.getBoolean("type", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "type"), type.uppercase(), true))
        }

        if (fieldsSection?.getBoolean("reason", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "reason"), reason, false))
        }

        if (fieldsSection?.getBoolean("time", true) != false) {
            fields.add(createField(mh.stringMessageToStringNoPrefix("webhook", "time"), formatDuration(duration), true))
        }

        val embed = JsonObject().apply {
            addProperty("title", mh.stringMessageToStringNoPrefix("webhook", "title"))
            addProperty("color", getColorForPunishmentType(type))
            addProperty("timestamp", Instant.now().toString())
            add("fields", fields)
            add("footer", JsonObject().apply {
                addProperty("text", "${mh.stringMessageToStringNoPrefix("webhook", "app_name")}${LocalDateTime.now().format(formatter)}")
                embedSection?.getString("footer.icon-url")?.takeIf { it.isNotBlank() }?.let { iconUrl ->
                    addProperty("icon_url", iconUrl)
                }
            })

            embedSection?.getConfigurationSection("author")?.let { authorSection ->
                val authorObject = JsonObject()

                authorSection.getString("name")?.takeIf { it.isNotBlank() }
                    ?.let { authorObject.addProperty("name", it) }
                authorSection.getString("icon-url")?.takeIf { it.isNotBlank() }
                    ?.let { authorObject.addProperty("icon_url", it) }
                if (authorObject.entrySet().isNotEmpty()) {
                    add("author", authorObject)
                }
            }

            embedSection?.getString("thumbnail-url")?.takeIf { it.isNotBlank() }?.let { url ->
                add("thumbnail", JsonObject().apply { addProperty("url", url) })
            }

            embedSection?.getString("image-url")?.takeIf { it.isNotBlank() }?.let { url ->
                add("image", JsonObject().apply { addProperty("url", url) })
            }
        }

        val json = JsonObject().apply {
            webhookSection?.getString("username")?.takeIf { it.isNotBlank() }?.let { addProperty("username", it) }
            webhookSection?.getString("avatar-url")?.takeIf { it.isNotBlank() }?.let { addProperty("avatar_url", it) }
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