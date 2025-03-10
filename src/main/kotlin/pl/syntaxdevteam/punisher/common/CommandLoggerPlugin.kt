package pl.syntaxdevteam.punisher.common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CommandLoggerPlugin(private val plugin: PunisherX) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val logFile: File = File(plugin.dataFolder, "commands.json")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }
        if (!logFile.exists()) {
            logFile.createNewFile()
            saveLogs(mutableMapOf())
        }
    }

    fun logCommand(executor: String, command: String, target: String, reason: String) {
        val logs = loadLogs().toMutableMap()
        logs[executor] = CommandEntry(command, target, reason, dateFormat.format(Date()))
        saveLogs(logs)
    }

    private fun loadLogs(): Map<String, CommandEntry> {
        return try {
            FileReader(logFile).use { reader ->
                gson.fromJson(reader, object : TypeToken<Map<String, CommandEntry>>() {}.type) ?: emptyMap()
            }
        } catch (e: Exception) {
            plugin.logger.warning("Nie można odczytać logs.json: ${e.message}")
            emptyMap()
        }
    }

    private fun saveLogs(logs: Map<String, CommandEntry>) {
        try {
            FileWriter(logFile).use { writer ->
                gson.toJson(logs, writer)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Błąd zapisu logs.json: ${e.message}")
        }
    }

    data class CommandEntry(val command: String, val target: String, val reason: String, val time: String)
}