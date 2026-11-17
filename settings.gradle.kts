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

private val availableModules = linkedMapOf(
    "paper" to "punisherx-paper",
    "spigot" to "punisherx-spigot",
    "velocity-bridge" to "punisherx-velocity-bridge",
    "bungee-bridge" to "punisherx-bungee-bridge",
)

private fun normalizeModuleAlias(raw: String): String? {
    return when (raw.trim().lowercase().replace('_', '-')) {
        "paper", "punisherx-paper" -> "paper"
        "spigot", "punisherx-spigot" -> "spigot"
        "velocity-bridge", "velocity", "punisherx-velocity-bridge" -> "velocity-bridge"
        "bungee-bridge", "bungee", "bungeecord-bridge", "punisherx-bungee-bridge",
        "punisherx-bungeecord-bridge" -> "bungee-bridge"
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
    include(availableModules.getValue(module))
}
