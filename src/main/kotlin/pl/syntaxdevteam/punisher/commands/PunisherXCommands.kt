package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.core.database.DatabaseType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class PunishesXCommands(private val plugin: PunisherX) : BasicCommand {
    private val mH = plugin.messageHandler

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val prefix = plugin.messageHandler.getPrefix()
        if (args.isNotEmpty() && args[0].equals("help", ignoreCase = true)) {
            val page = args.getOrNull(1)?.toIntOrNull() ?: 1
            sendHelp(stack, page)
            return
        }

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
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
                plugin.onReload()
                stack.sender.sendMessage(
                    mH.miniMessageFormat("$prefix <green>PunisherX has been reloaded.</green>")
                )
            }

            args[0].equals("export", ignoreCase = true) -> {
                plugin.databaseHandler.exportDatabase()
            }

            args[0].equals("import", ignoreCase = true) -> {
                plugin.databaseHandler.importDatabase()
            }

            args[0].equals("migrate", ignoreCase = true) -> {
                val from = args.getOrNull(1)
                val to = args.getOrNull(2)
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
                plugin.databaseHandler.migrateDatabase(fromType, toType)
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

    private fun sendHelp(stack: CommandSourceStack, page: Int) {
        val commands = listOf(
            "  <gold>/prx help <gray>- <white>Displays this prompt.", // zmienione /punisherx → /prx
            "  <gold>/prx version <gray>- <white>Shows plugin info.",
            "  <gold>/prx reload <gray>- <white>Reloads the configuration file.",
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
            "  <gold>/setspawn <gray>- <white>Sets the respawn location",
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

        if (args.size == 1) {
            val baseSuggestions = mutableListOf<String>()
            if ("help".startsWith(args[0], ignoreCase = true)) {
                baseSuggestions.add("help")
            }

            if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
                listOf("version", "reload", "export", "import").forEach { cmd ->
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
        if (args.size in 2..3 && args[0].equals("migrate", ignoreCase = true)) {
            //val types = DatabaseType.values().map { it.name.lowercase() }
            val types = DatabaseType::class.java.enumConstants
                ?.map { it.name.lowercase() }
                ?: emptyList()
            val current = args[args.size - 1]
            return types.filter { it.startsWith(current.lowercase()) }
        }
        return emptyList()
    }
}
