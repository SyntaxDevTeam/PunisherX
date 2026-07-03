package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner
import org.jetbrains.annotations.NotNull
import org.yaml.snakeyaml.Yaml
import java.lang.management.ManagementFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.core.database.DatabaseType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

private data class ConnectivityCheckResult(
    val ok: Boolean,
    val message: String,
    val durationMs: Long
)

private data class LibraryCoordinate(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    val artifactKey: String = "$groupId:$artifactId"

    companion object {
        fun parse(coordinate: String): LibraryCoordinate? {
            val parts = coordinate.split(":")
            if (parts.size < 3) {
                return null
            }
            return LibraryCoordinate(parts[0], parts[1], parts.subList(2, parts.size).joinToString(":"))
        }
    }
}

private data class LibraryProbe(
    val artifactKey: String,
    val className: String
)

private data class LibraryDiagnostic(
    val artifactKey: String,
    val declaredVersion: String?,
    val runtimeVersion: String?,
    val source: String?,
    val error: String?
)

class PunishesXCommands(private val plugin: PunisherX) : BasicCommand {
    private val mH = plugin.messageHandler
    private var pendingMigration: Pair<DatabaseType, DatabaseType>? = null

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val prefix = plugin.messageHandler.getPrefix()
        if (args.isNotEmpty() && args[0].equals("help", ignoreCase = true)) {
            val page = args.getOrNull(1)?.toIntOrNull() ?: 1
            sendHelp(stack, page)
            return
        }

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(
                mH.miniMessageFormat("$prefix <green>Type </green><gold>/prx help</gold> <green>to see available commands</green>")
            )
            return
        }

        val pluginMeta = (plugin as LifecycleEventOwner).pluginMeta

        when {
            args[0].equals("version", ignoreCase = true) -> {
                stack.sender.sendMessage(
                    mH.miniMessageFormat(
                        "\n<gray>-------------------------------------------------\n" +
                        " <gray>|\n" +
                        " <gray>|   <gold>→ <bold>${pluginMeta.name}</bold> ←\n" +
                        " <gray>|   <white>Author: <bold><gold>${pluginMeta.authors}</gold></bold>\n" +
                        " <gray>|   <white>Website: <bold><gold><click:open_url:'${pluginMeta.website}'>${pluginMeta.website}</click></gold></bold>\n" +
                        " <gray>|   <white>Version: <bold><gold>${pluginMeta.version}</gold></bold>\n" +
                        " <gray>|" +
                        "\n<gray>-------------------------------------------------"
                    )
                )
            }

            args[0].equals("reload", ignoreCase = true) -> {
                runCatching { plugin.onReload() }
                    .onSuccess {
                        stack.sender.sendMessage(
                            mH.miniMessageFormat("$prefix <green>PunisherX has been reloaded.</green>")
                        )
                    }
                    .onFailure { throwable ->
                        plugin.logger.err("[Reload] Failed to reload PunisherX: ${throwable.message ?: throwable.javaClass}")
                        plugin.reportError(throwable)
                        stack.sender.sendMessage(
                            mH.miniMessageFormat("$prefix <red>Failed to reload PunisherX. Check console for details.</red>")
                        )
                    }
            }

            args[0].equals("export", ignoreCase = true) -> {
                plugin.databaseHandler.exportDatabase()
            }

            args[0].equals("import", ignoreCase = true) -> {
                plugin.databaseHandler.importDatabase()
            }

            args[0].equals("diagnostics", ignoreCase = true) || args[0].equals("diag", ignoreCase = true) -> {
                runDiagnostics(stack)
            }

            args[0].equals("migrate", ignoreCase = true) -> {
                val force = args.any { it.equals("--force", ignoreCase = true) }
                val from = args.getOrNull(1)
                val to = when {
                    args.getOrNull(2)?.equals("--force", ignoreCase = true) == true -> args.getOrNull(3)
                    else -> args.getOrNull(2)
                }
                if (from == null || to == null) {
                    stack.sender.sendMessage(mH.miniMessageFormat("$prefix <red>Usage: /prx migrate <from> <to></red>"))
                    return
                }
                val fromType = runCatching { DatabaseType.valueOf(from.uppercase()) }.getOrNull()
                val toType = runCatching { DatabaseType.valueOf(to.uppercase()) }.getOrNull()
                if (fromType == null || toType == null) {
                    stack.sender.sendMessage(mH.miniMessageFormat("$prefix <red>Unknown database type.</red>"))
                    return
                }
                if (!force) {
                    val pending = pendingMigration
                    if (pending == null || pending.first != fromType || pending.second != toType) {
                        pendingMigration = fromType to toType
                        stack.sender.sendMessage(
                            mH.miniMessageFormat(
                                "$prefix <yellow>Update the connection details for <gold>${toType.name.lowercase()}</gold> in config.yml, then rerun this command. " +
                                        "Use <gold>--force</gold> to skip this confirmation."
                            )
                        )
                        return
                    }
                }

                pendingMigration = null
                stack.sender.sendMessage(
                    mH.miniMessageFormat("$prefix <yellow>Starting migration from <gold>${fromType.name.lowercase()}</gold> to <gold>${toType.name.lowercase()}</gold>...</yellow>")
                )

                val future = plugin.databaseHandler.migrateDatabase(fromType, toType)
                future.whenComplete { result, throwable ->
                    val response = when {
                        throwable != null -> "$prefix <red>Failed to migrate database: ${throwable.message ?: "Unknown error"}</red>"
                        result == null -> "$prefix <red>Migration finished with an unknown state.</red>"
                        result.success -> "$prefix <green>${result.message}</green>"
                        else -> "$prefix <red>${result.message}</red>"
                    }
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        stack.sender.sendMessage(mH.miniMessageFormat(response))
                    })
                }
            }
            /*
            args[0].equals("panel", ignoreCase = true) -> {
                PunisherMain(plugin).open(stack.sender as Player)
            }
*/
            else -> {

                stack.sender.sendMessage(
                    mH.miniMessageFormat("$prefix <green>Type </green><gold>/prx help</gold> <green>to see available commands</green>")
                )
            }
        }
    }

    private fun runDiagnostics(stack: CommandSourceStack) {
        val sender = stack.sender
        val prefix = plugin.messageHandler.getPrefix()
        val pluginMeta = (plugin as LifecycleEventOwner).pluginMeta
        val server = plugin.server
        val javaVersion = System.getProperty("java.version")
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val osVersion = System.getProperty("os.version")
        sender.sendMessage(mH.miniMessageFormat("$prefix <yellow>Running diagnostics, please wait...</yellow>"))

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val databaseCheck = plugin.databaseHandler.runHealthCheck()
            val externalConnectivity = checkExternalConnectivity()
            val runtime = Runtime.getRuntime()
            val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
            val uptime = ManagementFactory.getRuntimeMXBean().uptime
            val safeDatabaseMessage = databaseCheck.message.replace('<', '[').replace('>', ']')
            val safeNetworkMessage = externalConnectivity.message.replace('<', '[').replace('>', ']')
            val libraryReport = formatLibraryDiagnostics(buildLibraryDiagnostics())
            val report = listOf(
                " ",
                "<gray>-+-----------------------------------------------",
                " <gray>| <gold>${pluginMeta.name} diagnostics</gold>",
                "<gray>-+-----------------------------------------------",
                " <gray>| <white>Plugin version: <gold>${pluginMeta.version}</gold>",
                " <gray>| <white>Server brand: <gold>${server.name}</gold> <gray>${server.version}</gray>",
                " <gray>| <white>Java: <gold>$javaVersion</gold>",
                " <gray>| <white>OS: <gold>$osName $osVersion ($osArch)</gold>",
                " <gray>| <white>Uptime: <gold>${uptime / 1000}s</gold>",
                " <gray>| <white>Memory: <gold>${usedMemoryMb}MB</gold> / <gray>${maxMemoryMb}MB max</gray>",
                " <gray>| <white>Database: <gold>${plugin.databaseHandler.databaseType().name.lowercase()}</gold> - " +
                        (if (databaseCheck.ok) "<green>OK</green>" else "<red>FAILED</red>") +
                        " <gray>${databaseCheck.durationMs}ms</gray>",
                " <gray>| <white>DB details: <gray>$safeDatabaseMessage</gray>",
                " <gray>| <white>External connectivity: " +
                        (if (externalConnectivity.ok) "<green>OK</green>" else "<red>FAILED</red>") +
                        " <gray>${externalConnectivity.durationMs}ms</gray>",
                " <gray>| <white>Network details: <gray>$safeNetworkMessage</gray>",
                " <gray>| <white>Data folder: <gray>${plugin.dataFolder.absolutePath}</gray>",
                " <gray>| <white>Libraries:",
                libraryReport,
                " <gray>| <white>Share this output with support if an issue occurs.",
                "<gray>-+-----------------------------------------------",
                " "
            ).joinToString("\n")

            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.sendMessage(mH.miniMessageFormat(report))
            })
        })
    }

    private fun buildLibraryDiagnostics(): List<LibraryDiagnostic> {
        val declaredLibraries = loadDeclaredLibraries()
        val declaredByArtifact = declaredLibraries.associateBy { it.artifactKey }
        val artifactKeys = (declaredByArtifact.keys + runtimeLibraryProbes.map { it.artifactKey }).sorted()

        return artifactKeys.map { artifactKey ->
            val coordinate = declaredByArtifact[artifactKey]
            val probe = runtimeLibraryProbes.firstOrNull { it.artifactKey == artifactKey }
            val runtime = probe?.let { detectRuntimeLibrary(it.className) }
            LibraryDiagnostic(
                artifactKey = artifactKey,
                declaredVersion = coordinate?.version,
                runtimeVersion = runtime?.version,
                source = runtime?.source,
                error = runtime?.error ?: if (probe == null) "no runtime probe" else null
            )
        }
    }

    private fun loadDeclaredLibraries(): List<LibraryCoordinate> {
        return try {
            val input = plugin.javaClass.classLoader.getResourceAsStream("paper-libraries.yml") ?: return emptyList()
            input.use {
                val data = Yaml().load<Map<String, Any>>(it) ?: return emptyList()
                val libraries = data["libraries"] as? List<*> ?: return emptyList()
                libraries.mapNotNull { entry ->
                    when (entry) {
                        is String -> LibraryCoordinate.parse(entry)
                        is Map<*, *> -> (entry["coordinate"] as? String)?.let(LibraryCoordinate::parse)
                        else -> null
                    }
                }
            }
        } catch (exception: Exception) {
            listOf(LibraryCoordinate("diagnostics", "paper-libraries.yml", "error:${exception.message ?: exception.javaClass.simpleName}"))
        }
    }

    private data class RuntimeLibraryResult(
        val version: String?,
        val source: String?,
        val error: String?
    )

    private fun detectRuntimeLibrary(className: String): RuntimeLibraryResult {
        return try {
            val clazz = Class.forName(className, false, plugin.javaClass.classLoader)
            val source = clazz.protectionDomain?.codeSource?.location?.toString()
            val version = clazz.`package`?.implementationVersion ?: inferVersionFromSource(source)
            RuntimeLibraryResult(version, source?.let(::shortenJarSource), null)
        } catch (exception: Throwable) {
            RuntimeLibraryResult(null, null, "not loaded: ${exception.javaClass.simpleName}")
        }
    }

    private fun inferVersionFromSource(source: String?): String? {
        if (source == null) {
            return null
        }
        val fileName = source.substringAfterLast('/').removeSuffix(".jar")
        val decoded = URLDecoder.decode(fileName, StandardCharsets.UTF_8)
        val versionCandidate = decoded.substringAfterLast('-', missingDelimiterValue = "")
        return versionCandidate.takeIf { it.any(Char::isDigit) }
    }

    private fun shortenJarSource(source: String): String {
        val decoded = URLDecoder.decode(source, StandardCharsets.UTF_8)
        return decoded.substringAfterLast('/').ifBlank { decoded }
    }

    private fun formatLibraryDiagnostics(diagnostics: List<LibraryDiagnostic>): String {
        if (diagnostics.isEmpty()) {
            return " <gray>|   <gray>No loader libraries declared.</gray>"
        }

        return diagnostics.joinToString("\n") { diagnostic ->
            val declared = diagnostic.declaredVersion ?: "not declared"
            val runtime = diagnostic.runtimeVersion ?: diagnostic.error ?: "unknown"
            val status = when {
                diagnostic.runtimeVersion == null -> "<yellow>?</yellow>"
                diagnostic.declaredVersion == null -> "<yellow>runtime-only</yellow>"
                diagnostic.declaredVersion == diagnostic.runtimeVersion -> "<green>OK</green>"
                else -> "<red>MISMATCH</red>"
            }
            val source = diagnostic.source?.let { " <dark_gray>[$it]</dark_gray>" } ?: ""
            " <gray>|   <white>${diagnostic.artifactKey}: <gold>declared $declared</gold>, <gray>runtime $runtime</gray> $status$source"
        }
    }

    private fun checkExternalConnectivity(): ConnectivityCheckResult {
        val target = URI.create("https://example.com").toURL()
        val start = System.currentTimeMillis()
        return try {
            val connection = (target.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 3000
                readTimeout = 3000
            }
            val code = connection.responseCode
            val duration = System.currentTimeMillis() - start
            connection.disconnect()
            ConnectivityCheckResult(true, "example.com responded with HTTP $code", duration)
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - start
            ConnectivityCheckResult(false, exception.message ?: "Unknown network error", duration)
        }
    }

    private fun sendHelp(stack: CommandSourceStack, page: Int) {
        val commands = listOf(
            "  <gold>/prx help <gray>- <white>Displays this prompt.", // zmienione /punisherx → /prx
            "  <gold>/prx version <gray>- <white>Shows plugin info.",
            "  <gold>/prx reload <gray>- <white>Reloads the configuration file.",
            "  <gold>/prx diag <gray>- <white>Runs diagnostics and prints results.",
            "  <gold>/kick <player> <reason> <gray>- <white>Kicks a player from the server",
            "  <gold>/warn <player> (time) <reason> <gray>- <white>Warns a player.",
            "  <gold>/unwarn <player> <gray>- <white>Removes a player's warning.",
            "  <gold>/mute <player> (time) <reason> <gray>- <white>Mutes a player.",
            "  <gold>/unmute <player> <gray>- <white>Unmutes a player.",
            "  <gold>/ban <player> (time) <reason> [--force]",
            "         <gray>- <white>Bans a player, optionally ignoring bypass.",
            "  <gold>/banip <player/ip> (time) <reason> [--force] ",
            "         <gray>- <white>Bans a player's IP, optionally ignoring bypass.",
            "  <gold>/unban <player/ip> <gray>- <white>Unbans a player.",
            "  <gold>/check <player> <all/warn/mute/ban> ",
            "    <gray>- <white>Checks and displays the punishments of a given player",
            "  <gold>/clearall <player> <gray>- <white>Clears all active penalties.",
            "  <gold>/jail <player> (time) <reason> <gray>- <white>Sends a player to jail.",
            "  <gold>/unjail <player> <gray>- <white>Releases a player from jail.",
            "  <gold>/setjail <radius> <gray>- <white>Setting up the jail location.",
            "  <gold>/setunjail <gray>- <white>Sets the unjail respawn location",
            "                             <white>after serving a prison sentence.",
            "  <gold>/change-reason <penalty_id> <new_reason> <gray>",
            "                - <white>Changes the reason for the penalty.",
            "  ",
            "  <gold>/history <player> (page) ",
            "               <gray>- <white>Displays the player's punishment history.",
            "  <gold>/banlist <page> --h <gray>- <white>Displays the list of banned players.",
            "      <white>(Using the <gold>--h</gold> parameter will display the ban history)",
            "  ",
            "  <blue>Future:",
            "  <gold>/panel <gray>- <white>Opens the PunisherX GUI with lots of useful",
            "                                   <white> information and commands.",
            "  ",
            "  ",
            "  ",
            " "
        )

        val itemsPerPage = 12
        val totalPages = (commands.size + itemsPerPage - 1) / itemsPerPage
        val currentPage = page.coerceIn(1, totalPages)

        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>+-------------------------------------------------"))
        stack.sender.sendMessage(
            mH.miniMessageFormat(" <gray>|    <gold>Available commands for ${plugin.pluginMeta.name}:")
        )
        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>|"))

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = (currentPage * itemsPerPage).coerceAtMost(commands.size)
        for (i in startIndex until endIndex) {
            stack.sender.sendMessage(mH.miniMessageFormat(" <gray>|  ${commands[i]}"))
        }

        val prevPage = if (currentPage > 1) currentPage - 1 else totalPages
        val nextPage = if (currentPage < totalPages) currentPage + 1 else 1
        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>|"))
        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>|"))
        stack.sender.sendMessage(
            mH.miniMessageFormat(
                " <gray>| (Page $currentPage/$totalPages) " +
                        "<click:run_command:'/prx help $prevPage'><white>[Previous]</white></click>   " +
                        "<click:run_command:'/prx help $nextPage'><white>[Next]</white></click>"
            )
        )
        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>|"))
        stack.sender.sendMessage(mH.miniMessageFormat(" <gray>+-------------------------------------------------"))
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {

        if (args.isEmpty()) {
            val baseSuggestions = mutableListOf("help")
            if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
                baseSuggestions.addAll(listOf("version", "reload", "export", "import", "diag", "migrate"))
            }
            return baseSuggestions
        }

        if (args.size == 1) {
            val baseSuggestions = mutableListOf<String>()
            if ("help".startsWith(args[0], ignoreCase = true)) {
                baseSuggestions.add("help")
            }

            if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
                listOf("version", "reload", "export", "import", "diag", "diagnostics").forEach { cmd ->
                    if (cmd.startsWith(args[0], ignoreCase = true)) {
                        baseSuggestions.add(cmd)
                    }
                }
                if ("migrate".startsWith(args[0], ignoreCase = true)) {
                    baseSuggestions.add("migrate")
                }
            }
            return baseSuggestions
        }
        if (args[0].equals("migrate", ignoreCase = true)) {
            val types = DatabaseType::class.java.enumConstants
                ?.map { it.name.lowercase() }
                ?: emptyList()
            when (args.size) {
                2 -> {
                    val current = args[1]
                    return types.filter { it.startsWith(current.lowercase()) }
                }

                3 -> {
                    val current = args[2]
                    return if (current.startsWith("--")) {
                        listOf("--force").filter { it.startsWith(current.lowercase()) }
                    } else {
                        types.filter { it.startsWith(current.lowercase()) }
                    }
                }

                4 -> {
                    val current = args[3]
                    if (args[2].equals("--force", ignoreCase = true)) {
                        return emptyList()
                    }
                    return listOf("--force").filter { it.startsWith(current.lowercase()) }
                }
            }
        }
        return emptyList()
    }

    private companion object {
        private val runtimeLibraryProbes = listOf(
            LibraryProbe("org.jetbrains.kotlin:kotlin-stdlib", "kotlin.Unit"),
            LibraryProbe("org.eclipse.aether:aether-api", "org.eclipse.aether.artifact.Artifact"),
            LibraryProbe("org.yaml:snakeyaml", "org.yaml.snakeyaml.Yaml"),
            LibraryProbe("com.google.code.gson:gson", "com.google.gson.Gson"),
            LibraryProbe("com.maxmind.geoip2:geoip2", "com.maxmind.geoip2.DatabaseReader"),
            LibraryProbe("org.apache.ant:ant", "org.apache.tools.ant.Project"),
            LibraryProbe("com.github.ben-manes.caffeine:caffeine", "com.github.benmanes.caffeine.cache.Caffeine"),
            LibraryProbe("dev.dejvokep:boosted-yaml", "dev.dejvokep.boostedyaml.YamlDocument"),
            LibraryProbe("dev.faststats.metrics:bukkit", "dev.faststats.bukkit.BukkitMetrics"),
            LibraryProbe("net.luckperms:api", "net.luckperms.api.LuckPerms"),
            LibraryProbe("me.clip:placeholderapi", "me.clip.placeholderapi.PlaceholderAPI"),
            LibraryProbe("io.github.miniplaceholders:miniplaceholders-kotlin-ext", "io.github.miniplaceholders.kotlin.ExpansionExtKt"),
            LibraryProbe("com.github.milkbowl:VaultAPI", "net.milkbowl.vault.chat.Chat"),
            LibraryProbe("net.milkbowl.vault:VaultUnlockedAPI", "net.milkbowl.vault2.chat.Chat"),
            LibraryProbe("net.essentialsx:EssentialsXSpawn", "com.earth2me.essentials.spawn.IEssentialsSpawn"),
            LibraryProbe("pl.syntaxdevteam:syntaxcore", "pl.syntaxdevteam.core.SyntaxCore"),
            LibraryProbe("pl.syntaxdevteam:messageHandler-paper", "pl.syntaxdevteam.message.MessageHandler"),
            LibraryProbe("pl.syntaxdevteam:DscBridgeAPI", "pl.syntaxdevteam.dscbridgeapi.external.model.BridgeSlashCommand")
        )
    }
}
