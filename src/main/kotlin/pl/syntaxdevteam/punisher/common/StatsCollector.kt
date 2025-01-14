package pl.syntaxdevteam.punisher.common

import pl.syntaxdevteam.punisher.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

@Suppress("UnstableApiUsage")
class StatsCollector(private var plugin: PunisherX) {

    private val serverIP = getExternalIP()
    private val serverPort = plugin.server.port
    private val serverVersion = plugin.server.version
    private val serverName = plugin.server.name
    private val statsUrl = "https://topminecraft.pl/ping.php"
    private val pluginName = "${plugin.name} ${plugin.pluginMeta.version}"

    private val pluginUUID: String = plugin.config.getString("stats.apiKey") ?: "unknown-token"

    init {
        if (pluginUUID == "unknown-token") {
            plugin.logger.warning("Stats API key is not configured. Please set 'stats.apiKey' in the config.yml.")
        } else {
            plugin.logger.debug("PLUGIN_API_TOKEN: $pluginUUID")
        }

        if (plugin.config.getBoolean("stats.enabled")) {
            sendPing()
        } else {
            plugin.logger.info("StatsCollector is disabled in the configuration.")
        }
    }


    private fun sendPing() {
        val uri = URI(statsUrl)
        with(uri.toURL().openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true

            val data = "pluginName=$pluginName&serverIP=$serverIP&serverPort=$serverPort&serverVersion=$serverVersion&serverName=$serverName&pluginUUID=$pluginUUID"
            outputStream.write(data.toByteArray())
            outputStream.flush()
            outputStream.close()

            val responseCode = responseCode
            plugin.logger.debug("Response Code: $responseCode")

            val inputStream = inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line).append("\n")
            }

            plugin.logger.debug("Response Body: $response")
        }
    }

    private fun getExternalIP(): String {
        return try {
            URI("https://api.ipify.org").toURL().readText()
        } catch (e: Exception) {
            "unknown"
        }
    }
}
