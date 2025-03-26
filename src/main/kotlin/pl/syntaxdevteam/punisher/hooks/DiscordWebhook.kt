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
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    private val log = plugin.logger
    private val mh = plugin.messageHandler

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

        val fields = JsonArray().apply {
            add(createField(mh.getCleanMessage("webhook", "player"), playerName, true))
            add(createField(mh.getCleanMessage("webhook", "operator"), adminName, true))
            add(createField(mh.getCleanMessage("webhook", "type"), type.uppercase(), true))
            add(createField(mh.getCleanMessage("webhook", "reason"), reason, false))
            add(createField(mh.getCleanMessage("webhook", "time"), formatDuration(duration), true))
        }

        val embed = JsonObject().apply {
            addProperty("title", mh.getCleanMessage("webhook", "title"))
            addProperty("color", getColorForPunishmentType(type))
            addProperty("timestamp", Instant.now().toString())
            add("fields", fields)
            add("footer", JsonObject().apply {
                addProperty("text", "${mh.getCleanMessage("webhook", "app_name")}${LocalDateTime.now().format(formatter)}")
            })
        }

        val json = JsonObject().apply {
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
        return when (type.lowercase()) {
            "ban" -> 9447935 // Purple
            "mute" -> 15158332 // Red
            "warn" -> 16753920 // Orange
            "kick" -> 16776960 // Yellow
            else -> 8421504 // Gray
        }
    }

    private fun formatDuration(duration: Long): String {
        return if (duration == -1L) "PERMANENT" else {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(duration),
                ZoneId.systemDefault()
            )
            dateTime.format(formatter)
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