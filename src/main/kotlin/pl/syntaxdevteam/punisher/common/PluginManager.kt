package pl.syntaxdevteam.punisher.common

import com.google.gson.Gson
import com.google.gson.JsonArray
import pl.syntaxdevteam.punisher.PunisherX
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

@Suppress("UnstableApiUsage")
class PluginManager(private val plugin: PunisherX) {

    private val gson = Gson()

    init {
        val externalPlugins = fetchPluginsFromExternalSource()
        val loadedPlugins = fetchLoadedPlugins()
        val highestPriorityPlugin = getHighestPriorityPlugin(externalPlugins, loadedPlugins)
        if (highestPriorityPlugin == plugin.pluginMeta.name) {
            val syntaxDevTeamPlugins = loadedPlugins.filter { it.first != plugin.pluginMeta.name }
            plugin.logger.pluginStart(syntaxDevTeamPlugins)
        }
    }

    private fun fetchPluginsFromExternalSource(): List<PluginInfo> {
        return try {
            val uri = URI("https://raw.githubusercontent.com/SyntaxDevTeam/plugins-list/main/plugins.json")
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

    private fun fetchLoadedPlugins(): List<Pair<String, String>> {
        val plugins = mutableListOf<Pair<String, String>>()
        for (plugin in plugin.server.pluginManager.plugins) {
            if (plugin.pluginMeta.authors.contains("SyntaxDevTeam")) {
                plugins.add(Pair(plugin.pluginMeta.name, plugin.pluginMeta.version))
            }
        }
        return plugins
    }

    private fun getHighestPriorityPlugin(externalPlugins: List<PluginInfo>, loadedPlugins: List<Pair<String, String>>): String? {
        val matchedPlugins = externalPlugins.filter { externalPlugin ->
            loadedPlugins.any { it.first == externalPlugin.name }
        }
        return matchedPlugins.maxByOrNull { it.prior }?.name
    }

    fun getPluginUUID(pluginName: String): String? {
        val plugins = fetchPluginsFromExternalSource()
        val plugin = plugins.find { it.name == pluginName }
        return plugin?.uuid
    }
}
