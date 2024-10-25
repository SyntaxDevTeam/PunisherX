package pl.syntaxdevteam.punisher.helpers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.io.File

@Suppress("UnstableApiUsage")
class UpdateChecker(private val plugin: PunisherX, private val pluginMetas: PluginMeta, private val config: FileConfiguration) {

    private val hangarApiUrl = "https://hangar.papermc.io/api/v1/projects/${pluginMetas.name}/versions"
    private val pluginUrl = "https://hangar.papermc.io/SyntaxDevTeam/${pluginMetas.name}"
    private val gson = Gson()

    fun checkForUpdates() {
        if (!config.getBoolean("checkForUpdates", true)) {
            plugin.logger.debug("Update check is disabled in the config.")
            return
        }

        try {
            val uri = URI(hangarApiUrl)
            val url = uri.toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val versions = jsonObject.getAsJsonArray("result")
                val latestVersion = versions.firstOrNull()?.asJsonObject
                if (latestVersion != null && isNewerVersion(latestVersion.get("name").asString, pluginMetas.version)) {
                    notifyUpdate(latestVersion)
                    if (config.getBoolean("autoDownloadUpdates", false)) {
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
            if (player.hasPermission("${pluginMetas.name}.update.notify")) {
                player.sendMessage(message)
            }
        }
    }
}
