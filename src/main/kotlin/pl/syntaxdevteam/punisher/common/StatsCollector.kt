package pl.syntaxdevteam.punisher.common

import pl.syntaxdevteam.punisher.PunisherX
import java.util.concurrent.CompletableFuture
import java.net.HttpURLConnection
import java.net.URI

@Suppress("UnstableApiUsage")
class StatsCollector(private var plugin: PunisherX) {

    private val serverIP = getExternalIP()
    private val serverPort = plugin.server.port
    private val serverVersion = plugin.server.version
    private val serverName = plugin.server.name
    private val statsUrl = "https://syntaxdevteam.pl/ping.php"
    private val pluginName = "${plugin.name} ${plugin.pluginMeta.version}"

    private val pluginUUID: String = plugin.pluginsManager.getPluginUUID(plugin.name) ?: "unknown"

    init {
        if (plugin.config.getBoolean("stats.enabled")) {
            sendPing()
        } else {
            plugin.logger.warning("StatsCollector is disabled in the configuration.")
        }
    }

    private fun sendPing() {
        CompletableFuture.runAsync {
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
                        plugin.logger.debug("Stats sent successfully.")
                    } else {
                        plugin.logger.debug("Failed to send stats. Response code: $responseCode.")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("An error occurred while sending stats: ${e.message}")
            }
        }
    }

    private fun getExternalIP(): String {
        val apis = listOf("https://api.ipify.org", "https://ifconfig.me/ip")
        for (api in apis) {
            try {
                return URI(api).toURL().readText()
            } catch (e: Exception) {
                plugin.logger.debug("Failed to fetch IP from $api: ${e.message}")
            }
        }
        return "unknown"
    }
}
