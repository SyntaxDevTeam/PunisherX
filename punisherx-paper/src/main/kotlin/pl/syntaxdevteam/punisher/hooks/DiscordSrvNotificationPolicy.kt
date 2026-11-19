package pl.syntaxdevteam.punisher.hooks

import org.bukkit.configuration.ConfigurationSection

internal object DiscordSrvNotificationPolicy {
    const val ROOT = "integration.discordsrv"
    private val supported = setOf("warn", "mute", "kick", "ban", "banip", "jail", "unwarn", "unmute", "unban", "unjail")
    private val nickname = Regex("[A-Za-z0-9_]{1,16}")
    private val ipv4 = Regex("""(?<![0-9])(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?![0-9])""")
    private val ipv6 = Regex("""(?i)(?<![0-9a-f:])(?:[0-9a-f]{0,4}:){2,}[0-9a-f:.%a-z0-9_-]*(?![0-9a-f:])""")
    private fun redact(value: String): String =
        ipv6.replace(ipv4.replace(value, "[hidden]"), "[hidden]")
            .replace(Regex("[\\p{Cc}\\p{Cf}]"), " ")

    fun render(config: ConfigurationSection, type: String, values: Map<String, String>, silent: Boolean,
               localize: (String, Map<String, String>) -> String): String? {
        if (silent || type !in supported || !config.getBoolean("$ROOT.enabled", true) ||
            !config.getBoolean("$ROOT.notifications.enabled", true) ||
            !config.getBoolean("$ROOT.notifications.punishments.$type", true)) return null
        val player = values["player"]?.takeIf(nickname::matches) ?: localize("hidden-target", emptyMap())
        val lines = mutableListOf(localize(type, mapOf("player" to player)))
        if (!type.startsWith("un") && type != "kick")
            values["time"]?.takeIf { it.isNotBlank() }?.let { lines += localize("time", mapOf("time" to redact(it))) }
        if (config.getBoolean("$ROOT.notifications.show-reason", true) && !type.startsWith("un"))
            values["reason"]?.takeIf { it.isNotBlank() }?.let { lines += localize("reason", mapOf("reason" to redact(it))) }
        if (config.getBoolean("$ROOT.notifications.show-operator", false))
            values["operator"]?.takeIf { it.isNotBlank() }?.let { lines += localize("operator", mapOf("operator" to redact(it))) }
        return lines.joinToString("\n") { redact(it) }.replace("@", "@\u200B")
            .replace(Regex("""([\\*_`~|<>])""")) { "\\" + it.value }
            .take(1900).takeIf { it.isNotBlank() }
    }
}
