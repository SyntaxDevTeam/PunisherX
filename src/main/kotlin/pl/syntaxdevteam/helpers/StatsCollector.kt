package pl.syntaxdevteam.helpers

import pl.syntaxdevteam.PunisherX
import java.net.HttpURLConnection
import java.net.URI

class StatsCollector(plugin: PunisherX) {

    private val serverIP = getExternalIP()
    private val serverPort = plugin.server.port
    private val serverVersion = plugin.server.version
    private val serverName = plugin.server.name
    private val statsUrl = "https://syntaxdevteam.pl/ping.php"
    private val pluginName = plugin.name

    init {
        if (plugin.config.getBoolean("stats.enabled")) {
            sendPing()
        }
    }

    private fun sendPing() {
        val uri = URI(statsUrl)
        with(uri.toURL().openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true
            outputStream.write("pluginName=$pluginName&serverIP=$serverIP&serverPort=$serverPort&serverVersion=$serverVersion&serverName=$serverName".toByteArray())
            outputStream.flush()
            outputStream.close()
            responseCode // To trigger the request
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
