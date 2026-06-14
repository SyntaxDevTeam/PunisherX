pluginManagement {
    repositories {
        // Twoje repozytoria z pluginami:
        maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
        maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")

        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "PunisherX"

private data class ModuleDefinition(
    val projectName: String,
    val directory: String? = null,
)

private val availableModules = linkedMapOf(
    "paper" to ModuleDefinition("punisherx-paper"),
    "spigot" to ModuleDefinition("punisherx-spigot"),
    "velocity-bridge" to ModuleDefinition("PunisherX-Velocity-Bridge", "velocity-bridge"),
    "bungee-bridge" to ModuleDefinition("PunisherX-BungeeCord-Bridge", "bungee-bridge"),
)

private fun normalizeModuleAlias(raw: String): String? {
    return when (raw.trim().lowercase().replace('_', '-')) {
        "paper", "punisherx-paper" -> "paper"
        "spigot", "punisherx-spigot" -> "spigot"
        "velocity-bridge", "velocity", "punisherx-velocity-bridge" -> "velocity-bridge"
        "bungee-bridge", "bungee", "bungeecord-bridge", "punisherx-bungeecord-bridge" -> "bungee-bridge"
        else -> null
    }
}

private fun resolveEnabledModules(): Set<String> {
    val configured = providers.gradleProperty("punisherx.build.modules")
        .orElse("all")
        .get()
        .trim()

    if (configured.equals("all", ignoreCase = true)) {
        return availableModules.keys
    }

    val enabled = linkedSetOf<String>()
    val unknown = linkedSetOf<String>()

    configured.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { alias ->
            val normalized = normalizeModuleAlias(alias)
            if (normalized == null) {
                unknown += alias
            } else {
                enabled += normalized
            }
        }

    require(unknown.isEmpty()) {
        "Unknown punisherx.build.modules entries: ${unknown.joinToString(", ")}. " +
            "Available modules: ${availableModules.keys.joinToString(", ")}, all"
    }
    require(enabled.isNotEmpty()) {
        "punisherx.build.modules must contain at least one module or be set to 'all'."
    }

    return enabled
}

val enabledModules = resolveEnabledModules()
logger.lifecycle("PunisherX enabled build modules: ${enabledModules.joinToString(", ")}")

enabledModules.forEach { module ->
    val definition = availableModules.getValue(module)
    include(definition.projectName)
    definition.directory?.let { directory ->
        project(":${definition.projectName}").projectDir = file(directory)
    }
}
