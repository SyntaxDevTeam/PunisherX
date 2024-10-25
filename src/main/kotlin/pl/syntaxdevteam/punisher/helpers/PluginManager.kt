package pl.syntaxdevteam.punisher.helpers

import com.google.gson.Gson
import com.google.gson.JsonArray
import pl.syntaxdevteam.punisher.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

data class PluginInfo(val name: String, val uuid: String, val prior: Int)

@Suppress("UnstableApiUsage")
class PluginManager(private val plugin: PunisherX) {

    private val gson = Gson()

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
                val jsonArray = gson.fromJson(responseBody, JsonArray::class.java)
                jsonArray.map { jsonElement ->
                    val json = jsonElement.asJsonObject
                    PluginInfo(
                        name = json.get("name").asString,
                        uuid = json.get("uuid").asString,
                        prior = json.get("prior").asInt
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
                plugins.add(Pair(plugin.pluginMeta.name, plugin.pluginMeta.version))
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
