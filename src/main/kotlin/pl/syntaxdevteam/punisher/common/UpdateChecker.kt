package pl.syntaxdevteam.punisher.common

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Suppress("UnstableApiUsage")
class UpdateChecker(private val plugin: PunisherX) {

    private val hangarApiUrl = "https://hangar.papermc.io/api/v1/projects/${plugin.pluginMeta.name}/versions"
    private val pluginUrl = "https://hangar.papermc.io/SyntaxDevTeam/${plugin.pluginMeta.name}"
    private val gson = Gson()

    fun checkForUpdates() {
        if (!plugin.config.getBoolean("checkForUpdates", true)) {
            plugin.logger.debug("Update check is disabled in the config.")
            return
        }

        CompletableFuture.runAsync {
            try {
                val uri = URI(hangarApiUrl)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                    val versions = jsonObject.getAsJsonArray("result")
                    val latestVersion = versions.firstOrNull()?.asJsonObject
                    if (latestVersion != null && isNewerVersion(latestVersion.get("name").asString, plugin.pluginMeta.version)) {
                        notifyUpdate(latestVersion)
                        if (plugin.config.getBoolean("autoDownloadUpdates", false)) {
                            downloadUpdate(latestVersion)
                        }
                    } else {
                        plugin.logger.success("Your version is up to date")
                    }
                } else {
                    plugin.logger.warning("Failed to check for updates: $responseCode")
                }
            } catch (e: Exception) {
                plugin.logger.warning("An error occurred while checking for updates: ${e.message}")
            }
        }.orTimeout(10, TimeUnit.SECONDS).exceptionally { e ->
            plugin.logger.warning("Update check timed out or failed: ${e.message}")
            null
        }
    }

    fun checkForUpdatesForPlayer(player: Player) {
        if (!plugin.config.getBoolean("checkForUpdates", true)) {
            plugin.logger.debug("Update check is disabled in the config.")
            return
        }

        CompletableFuture.runAsync {
            try {
                val uri = URI(hangarApiUrl)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                    val versions = jsonObject.getAsJsonArray("result")
                    val latestVersion = versions.firstOrNull()?.asJsonObject

                    if (latestVersion != null
                        && isNewerVersion(latestVersion.get("name").asString, plugin.pluginMeta.version)
                    ) {

                        val versionName = latestVersion.get("name").asString
                        val channel = latestVersion.getAsJsonObject("channel").get("name").asString
                        val prefix = plugin.messageHandler.getPrefix()
                        val message = when (channel) {
                            "Release" -> "$prefix <green>New release version <bold>$versionName</bold> is available on <u><click:open_url:'$pluginUrl'>Hangar</click>!"
                            "Snapshot" -> "$prefix <yellow>New snapshot version <bold>$versionName</bold> is available on <u><click:open_url:'$pluginUrl'>Hangar</click>!"
                            else -> "$prefix <blue>New version <bold>$versionName</bold> is available on <u><click:open_url:'$pluginUrl'>Hangar</click>!"
                        }
                        val component = MiniMessage.miniMessage().deserialize(message)

                        player.sendMessage(component)

                        if (plugin.config.getBoolean("autoDownloadUpdates", false)) {
                            downloadUpdate(latestVersion)
                        }
                    }
                } else {
                    plugin.logger.warning("Failed to check for updates (for player): $responseCode")
                }
            } catch (e: Exception) {
                plugin.logger.warning("Błąd podczas sprawdzania aktualizacji (dla gracza ${player.name}): ${e.message}")
            }
        }.orTimeout(10, TimeUnit.SECONDS).exceptionally { e ->
            plugin.logger.warning("Update check (dla gracza ${player.name}) timeout lub nieudane: ${e.message}")
            null
        }
    }

    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split("-", limit = 2)
        val currentParts = currentVersion.split("-", limit = 2)

        val latestBase = latestParts[0].split(".")
        val currentBase = currentParts[0].split(".")

        for (i in latestBase.indices) {
            val latestPart = latestBase.getOrNull(i)?.toIntOrNull() ?: 0
            val currentPart = currentBase.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestPart > currentPart) {
                return true
            } else if (latestPart < currentPart) {
                return false
            }
        }

        if (latestParts.size > 1 && currentParts.size > 1) {
            return latestParts[1] > currentParts[1]
        }

        return latestParts.size > currentParts.size
    }

    private fun notifyUpdate(version: JsonObject) {
        val versionName = version.get("name").asString
        val channel = version.getAsJsonObject("channel").get("name").asString
        val message = when (channel) {
            "Release" -> "<green>New release version <bold>$versionName</bold> is available on <click:open_url:'$pluginUrl'>Hangar</click>!"
            "Snapshot" -> "<yellow>New snapshot version <bold>$versionName</bold> is available on <click:open_url:'$pluginUrl'>Hangar</click>!"
            else -> "<blue>New version <bold>$versionName</bold> is available on <click:open_url:'$pluginUrl'>Hangar</click>!"
        }
        val component = MiniMessage.miniMessage().deserialize(message)
        plugin.logger.success("New release version $versionName is available on $pluginUrl")
        notifyAdmins(component)
    }

    private fun downloadUpdate(version: JsonObject) {
        val versionName = version.get("name").asString
        val downloadUrl = version.getAsJsonObject("downloads").getAsJsonObject("PAPER").get("downloadUrl").asString
        val fileName = version.getAsJsonObject("downloads").getAsJsonObject("PAPER").getAsJsonObject("fileInfo").get("name").asString
        val newFile = File(plugin.dataFolder.parentFile, fileName)
        val currentFile = plugin.getPluginFile()

        try {
            val uri = URI(downloadUrl)
            val url = uri.toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                url.openStream().use { input ->
                    newFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                plugin.logger.success("Downloaded new version $versionName to ${newFile.absolutePath}")
                if (currentFile.exists() && currentFile != newFile) {
                    currentFile.delete()
                    plugin.logger.success("Deleted old version ${currentFile.name}. Restart your server to enjoy the latest plugin version")
                }
            } else {
                plugin.logger.warning("Failed to download new version $versionName: $responseCode")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to download new version $versionName: ${e.message}")
        }
    }

    private fun notifyAdmins(message: Component) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("${plugin.pluginMeta.name}.update.notify")) {
                player.sendMessage(message)
            }
        }
    }
}
