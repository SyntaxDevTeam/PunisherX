package pl.syntaxdevteam.punisher.config

import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

data class ConfigurationValidationIssue(
    val file: File,
    val error: String
)

data class ConfigurationValidationReport(
    val dataFolder: File,
    val issues: List<ConfigurationValidationIssue>
) {
    val valid: Boolean
        get() = issues.isEmpty()

    fun compactMessage(): String = if (valid) {
        "All checked YAML files are valid."
    } else {
        issues.joinToString("; ") { issue ->
            "${relativePath(issue.file)}: ${issue.error}"
        }
    }

    fun relativePath(file: File): String = runCatching {
        dataFolder.toPath().toAbsolutePath().normalize()
            .relativize(file.toPath().toAbsolutePath().normalize())
            .toString()
            .replace(File.separatorChar, '/')
    }.getOrElse { file.name }
}

/**
 * Performs a read-only validation pass over PunisherX YAML files before a reload.
 * No file is created, rewritten, renamed or deleted by this service.
 */
class ConfigurationPreflight(private val plugin: PunisherX) {
    fun validate(): ConfigurationValidationReport {
        val issues = mutableListOf<ConfigurationValidationIssue>()
        val configFile = File(plugin.dataFolder, "config.yml")
        val config = validateYaml(configFile, issues)

        validateYaml(File(plugin.dataFolder, "punish-templates.yml"), issues)
        validateYaml(File(plugin.dataFolder, "DBAPI_config.yml"), issues)

        if (config != null) {
            validateLanguageFiles(config, issues)
        }

        return ConfigurationValidationReport(plugin.dataFolder, issues)
    }

    private fun validateLanguageFiles(
        config: YamlConfiguration,
        issues: MutableList<ConfigurationValidationIssue>
    ) {
        val configured = config.getString("language", "EN")
            .orEmpty()
            .trim()
            .lowercase(Locale.ROOT)
        val langDirectory = File(plugin.dataFolder, "lang")

        if (configured == "auto") {
            val fallback = config.getString("fallback-language", "EN")
                .orEmpty()
                .trim()
                .lowercase(Locale.ROOT)
                .takeUnless { it.isBlank() || it == "auto" }
                ?: "en"

            val candidates = linkedSetOf<File>()
            candidates += File(langDirectory, "messages_$fallback.yml")
            langDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("messages_") && file.extension.equals("yml", ignoreCase = true)
            }?.sortedBy { it.name }?.let(candidates::addAll)

            candidates.filter(File::exists).forEach { validateYaml(it, issues) }
            return
        }

        val language = configured.ifBlank { "en" }
        validateYaml(File(langDirectory, "messages_$language.yml"), issues)
    }

    private fun validateYaml(
        file: File,
        issues: MutableList<ConfigurationValidationIssue>
    ): YamlConfiguration? {
        if (!file.exists()) return null

        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            YamlConfiguration().apply { loadFromString(content) }
        } catch (throwable: Throwable) {
            issues += ConfigurationValidationIssue(file, describeError(throwable))
            null
        }
    }

    private fun describeError(throwable: Throwable): String =
        throwable.message
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.takeUnless(String::isBlank)
            ?: throwable.javaClass.simpleName
}
