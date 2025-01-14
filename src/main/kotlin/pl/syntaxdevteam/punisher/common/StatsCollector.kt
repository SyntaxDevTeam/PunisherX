package pl.syntaxdevteam.punisher.common

import pl.syntaxdevteam.punisher.PunisherX
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
            plugin.logger.debug("PLUGIN_API_TOKEN: ${pluginUUID.take(5)}*****")
        }

        if (plugin.config.getBoolean("stats.enabled")) {
            sendPing()
        } else {
            plugin.logger.info("StatsCollector is disabled in the configuration.")
        }
    }

    private fun sendPing() {
        try {
            val uri = URI(statsUrl)
            with(uri.toURL().openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000

                val data = mapOf(
                    "pluginName" to pluginName,
                    "serverIP" to serverIP,
                    "serverPort" to serverPort.toString(),
                    "serverVersion" to serverVersion,
                    "serverName" to serverName,
                    "pluginUUID" to pluginUUID
                ).entries.joinToString("&") { "${it.key}=${it.value}" }

                outputStream.use {
                    it.write(data.toByteArray())
                    it.flush()
                }

                val responseCode = responseCode
                if (responseCode in 200..299) {
                    inputStream.bufferedReader().use {
                        val response = it.readText()
                        plugin.logger.info("Stats sent successfully: $response")
                    }
                } else {
                    errorStream?.bufferedReader()?.use {
                        val errorResponse = it.readText()
                        plugin.logger.warning("Failed to send stats. Response code: $responseCode. Error: $errorResponse")
                    } ?: plugin.logger.warning("Failed to send stats. Response code: $responseCode. No error message.")
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("An error occurred while sending stats: ${e.message}")
        }
    }

    private fun getExternalIP(): String {
        val apis = listOf("https://api.ipify.org", "https://ifconfig.me/ip")
        for (api in apis) {
            try {
                return URI(api).toURL().readText()
            } catch (e: Exception) {
                plugin.logger.warning("Failed to fetch IP from $api: ${e.message}")
            }
        }
        return "unknown"
    }
}
