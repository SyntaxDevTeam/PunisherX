package pl.syntaxdevteam.punisher.config

import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.identity.IdentitySettings
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
        "Configuration validation passed."
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

/** An expected configuration refusal, not an internal plugin failure. */
class ConfigurationValidationException(message: String) : IllegalStateException(message)

/**
 * Performs a read-only validation pass over PunisherX YAML files.
 * No file is created, rewritten, renamed or deleted by this service.
 */
class ConfigurationPreflight(private val plugin: PunisherX) {
    fun validate(includeLanguageFiles: Boolean = true): ConfigurationValidationReport {
        val issues = mutableListOf<ConfigurationValidationIssue>()
        val configFile = File(plugin.dataFolder, "config.yml")
        val config = validateYaml(configFile, issues)
        if (config != null) {
            validateIdentity(configFile, config, issues)
        } else if (!configFile.exists()) {
            issues += ConfigurationValidationIssue(configFile, "Missing config.yml. Start PunisherX once to generate the default configuration.")
        }

        validateYaml(File(plugin.dataFolder, "punish-templates.yml"), issues)
        validateYaml(File(plugin.dataFolder, "DBAPI_config.yml"), issues)

        if (includeLanguageFiles && config != null) {
            validateLanguageFiles(config, issues)
        }

        return ConfigurationValidationReport(plugin.dataFolder, issues)
    }

    private fun validateIdentity(
        file: File,
        config: YamlConfiguration,
        issues: MutableList<ConfigurationValidationIssue>
    ) {
        val onlineMode = plugin.server.onlineMode
        val realm = config.getString("identity.realm")
        val authority = config.getString("identity.authority")
        try {
            IdentitySettings.create(realm, authority, onlineMode)
        } catch (error: IllegalArgumentException) {
            val guidance = "Check identity.realm and identity.authority in config.yml " +
                "(server online-mode=$onlineMode). " +
                "For an offline server use authority: auto (or offline) and a stable, explicit realm, " +
                "e.g. realm: my-server. For authenticated proxy forwarding only, use authority: trusted-proxy " +
                "and the explicitly configured shared realm. For online-mode=true, auto or online may use the default realm. " +
                "Reuse the existing database's realm; never choose a different one to bypass a mismatch."
            issues += ConfigurationValidationIssue(file, "${error.message}. $guidance")
        }
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
