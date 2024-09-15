package pl.syntaxdevteam.helpers

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import pl.syntaxdevteam.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

data class PluginInfo(val name: String, val uuid: String, val prior: Int)

@Suppress("UnstableApiUsage")
class PluginManager(private val plugin: PunisherX) {

    fun fetchPluginsFromExternalSource(urlString: String): List<PluginInfo> {
        return try {
            val uri = URI(urlString)
            val url = uri.toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val parser = JSONParser()
                val jsonArray = parser.parse(responseBody) as JSONArray
                jsonArray.map { jsonObject ->
                    val json = jsonObject as JSONObject
                    PluginInfo(
                        name = json["name"] as String,
                        uuid = json["uuid"] as String,
                        prior = (json["prior"] as Long).toInt()
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            plugin.logger.warning("An error occurred while fetching plugins: ${e.message}")
            emptyList()
        }
    }

    fun fetchLoadedPlugins(): List<Pair<String, String>> {
        val plugins = mutableListOf<Pair<String, String>>()
        for (plugin in plugin.server.pluginManager.plugins) {
            if (plugin.pluginMeta.authors.contains("SyntaxDevTeam")) {
                plugins.add(Pair(plugin.name, plugin.pluginMeta.version))
            }
        }
        return plugins
    }

    fun getHighestPriorityPlugin(externalPlugins: List<PluginInfo>, loadedPlugins: List<Pair<String, String>>): String? {
        val matchedPlugins = externalPlugins.filter { externalPlugin ->
            loadedPlugins.any { it.first == externalPlugin.name }
        }
        return matchedPlugins.maxByOrNull { it.prior }?.name
    }
}
