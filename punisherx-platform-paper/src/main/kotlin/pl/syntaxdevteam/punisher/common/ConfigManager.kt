package pl.syntaxdevteam.punisher.common

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ConfigManager(private val plugin: PunisherX) {

    companion object {
        private const val FILE_NAME = "config.yml"
        private const val VERSION_KEY = "config-version"
        private const val V_104 = 104
        private const val V_141 = 141
        private const val V_160 = 160
    }

    lateinit var config: YamlDocument
    private var rawUserDoc: YamlDocument? = null
    private val dataFile: File get() = File(plugin.dataFolder, FILE_NAME)

    fun load() {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()

        val defaultsStream = plugin.getResource(FILE_NAME)
            ?: error("Missing $FILE_NAME in resources (template 1.6.0 with comments required).")

        plugin.logger.debug("[Config] Loading $FILE_NAME (round-trip, auto-update)…")
        rawUserDoc = if (dataFile.exists()) {
            YamlDocument.create(
                dataFile,
                GeneralSettings.builder().setUseDefaults(false).build(),
                LoaderSettings.builder().setAutoUpdate(false).build(),
                DumperSettings.builder().setIndentation(2).build()
            )
        } else null

        val sourceVersion = detectSourceVersion(rawUserDoc)

        if (sourceVersion < V_160 && dataFile.exists()) {
            val bak = File(dataFile.parentFile, "$FILE_NAME.$sourceVersion.bak")
            try {
                Files.copy(dataFile.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING)
                plugin.logger.debug("[Config] Backup before migration: ${bak.name}")
            } catch (t: Throwable) {
                plugin.logger.warning("[Config] Failed to create backup: ${t.message}")
            }
        }

        config = YamlDocument.create(
            dataFile,
            defaultsStream,
            GeneralSettings.builder().setUseDefaults(true).build(),
            LoaderSettings.builder().setAutoUpdate(true).build(),
            DumperSettings.builder().setIndentation(2).build()
        )

        if (!config.contains(VERSION_KEY)) {
            plugin.logger.debug("[Config] config-version not found – treating as 1.4.1")
            config.set(VERSION_KEY, V_141)
        }

        migrateFrom(sourceVersion)

        config.set(VERSION_KEY, V_160)
        config.save()
        plugin.logger.success("[Config] Done. Current version: ${config.getInt(VERSION_KEY)}")
    }

    fun reload() {
        plugin.logger.debug("[Config] Reload…")
        load()
    }

    // ================= VERSIONS AND MIGRATIONS =================

    private fun detectSourceVersion(doc: YamlDocument?): Int {
        val fromKey = doc?.get(VERSION_KEY)?.let { parseVersionValue(it) }
        if (fromKey != null) return fromKey

        val guessed = guessVersionFromComment()
        if (guessed != null) return guessed

        return if (doc == null) V_160 else V_141
    }

    private fun migrateFrom(sourceVersion: Int) {
        if (sourceVersion >= V_160) return

        if (sourceVersion <= V_104) {
            plugin.logger.debug("[Config] Migrating $sourceVersion -> $V_104 …")
            migrate104to160()
        }
        if (sourceVersion <= V_141) {
            plugin.logger.debug("[Config] Migrating $sourceVersion -> $V_160 …")
            migrate141to160()
        }
    }

    private fun migrate104to160() {
        val oldWarn = readSectionMapRaw(rawUserDoc, "WarnActions")
        if (oldWarn != null && oldWarn.isNotEmpty()) {
            val dst = "actions.warn.count"
            val it = oldWarn.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val keyStr = e.key
                config.set("$dst.$keyStr", e.value)
            }
            config.remove("WarnActions")
            plugin.logger.debug("[Config] WarnActions → $dst (server values overwrote defaults)")
        }

        if (rawUserDoc?.contains("mute_pm") == true) {
            val mutePm = rawUserDoc?.getBoolean("mute_pm") ?: false
            config.set("mute.pm", mutePm)
            plugin.logger.debug("[Config] mute_pm → mute.pm = $mutePm (server)")
            config.remove("mute_pm")
        }

        val muteCmdRaw = rawUserDoc?.get("mute_cmd")
        if (muteCmdRaw is List<*>) {
            config.set("mute.cmd", muteCmdRaw)
            plugin.logger.debug("[Config] mute_cmd → mute.cmd (server values overwrote defaults)")
            config.remove("mute_cmd")
        }

        val checkForUpdates = rawUserDoc?.getBoolean("checkForUpdates")
        if (checkForUpdates != null) {
            config.set("update.check-for-updates", checkForUpdates)
            plugin.logger.debug("[Config] checkForUpdates → update.check-for-updates = $checkForUpdates (server)")
            config.remove("checkForUpdates")
        }

        val autoDownloadUpdates = rawUserDoc?.getBoolean("autoDownloadUpdates")
        if (autoDownloadUpdates != null) {
            config.set("update.auto-download", autoDownloadUpdates)
            plugin.logger.debug("[Config] autoDownloadUpdates → update.auto-download = $autoDownloadUpdates (server)")
            config.remove("autoDownloadUpdates")
        }
    }

    private fun migrate141to160() {

        var oldWarn = readSectionMapRaw(rawUserDoc, "warn.actions")

        if (oldWarn == null || oldWarn.isEmpty()) {
            oldWarn = readSectionMapRaw(rawUserDoc, "actions.warn.count")
        }

        if (oldWarn != null && oldWarn.isNotEmpty()) {
            val dst = "actions.warn.count"

            val it = oldWarn.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val keyStr = e.key
                config.set("$dst.$keyStr", e.value)
            }

            plugin.logger.debug("[Config] warn/actions → $dst (server values overwrote defaults)")
        }

        if (config.contains("warn.actions")) {
            config.set("warn.actions", null)
        }

        val oldSpawnLoc = readSectionMapRaw(rawUserDoc, "spawn.location")
        if (oldSpawnLoc != null && oldSpawnLoc.isNotEmpty()) {
            config.set("unjail.unjail_location", oldSpawnLoc)
            plugin.logger.debug("[Config] spawn.location → unjail.unjail_location (server)")
        }

        val hadUseExternal =
            rawUserDoc?.contains("spawn.use_external_set.enabled") == true ||
                    rawUserDoc?.contains("spawn.use_external_set.set") == true

        if (hadUseExternal) {
            val enabled = rawUserDoc?.getBoolean("spawn.use_external_set.enabled") ?: false
            val setRaw = rawUserDoc?.getString("spawn.use_external_set.set")
            val mapped: String = if (!enabled) {
                "unjail"
            } else {
                val sr = setRaw?.trim()?.lowercase() ?: ""
                when (sr) {
                    "essx" -> "essx"
                    "world" -> "world"
                    else -> "world"
                }
            }
            config.set("unjail.spawn_type_select.set", mapped)
            plugin.logger.debug("[Config] spawn.use_external_set → unjail.spawn_type_select.set = $mapped (server)")
        }

        if (config.contains("spawn.location")) config.set("spawn.location", null)
        if (config.contains("spawn.use_external_set")) config.set("spawn.use_external_set", null)

        val spawn = readSectionMap("spawn")
        if (spawn != null && spawn.isEmpty()) config.set("spawn", null)

        if (!config.contains("server")) {
            config.set("server", "network")
            plugin.logger.debug("[Config] server = \"network\" set (default)")
        }
    }

    // ================= HELPERS =================

    private fun parseVersionValue(raw: Any?): Int? {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: raw.filter(Char::isDigit).toIntOrNull()
            else -> null
        }
    }

    private fun guessVersionFromComment(): Int? {
        if (!dataFile.exists()) return null
        return try {
            dataFile.useLines { lines ->
                val firstMeaningful = lines.firstOrNull { it.isNotBlank() } ?: return@useLines null
                val match = Regex("(\\d+\\.(?:\\d+\\.)*\\d+)").find(firstMeaningful)
                val number = match?.value?.filter(Char::isDigit) ?: return@useLines null
                number.toIntOrNull()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readSectionMap(path: String): Map<String, Any?>? {
        val raw = config.get(path) ?: return null
        if (raw is Map<*, *>) {
            return raw.asLinkedStringMap()
        }
        if (raw is Section) {
            return raw.asLinkedStringMap()
        }
        val section = config.getOptionalSection(path).orElse(null)
        return section?.asLinkedStringMap()
    }

    private fun readSectionMapRaw(doc: YamlDocument?, path: String): Map<String, Any?>? {
        if (doc == null) return null
        val section = doc.getOptionalSection(path).orElse(null)
        if (section != null) {
            return section.asLinkedStringMap()
        }
        val raw = doc.get(path) ?: return null
        if (raw is Map<*, *>) {
            return raw.asLinkedStringMap()
        }
        if (raw is Section) {
            return raw.asLinkedStringMap()
        }
        return null
    }

    private fun Section.asLinkedStringMap(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for (key in this.keys) {
            if (key == null) continue
            val keyStr = key.toString()
            out[keyStr] = this.get(keyStr)
        }
        return out
    }

    private fun Map<*, *>.asLinkedStringMap(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        val it = this.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val k = e.key ?: continue
            out[k.toString()] = e.value
        }
        return out
    }
}
