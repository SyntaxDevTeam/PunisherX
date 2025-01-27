package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage", "DEPRECATION")
class PunishesXCommands(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (!stack.sender.hasPermission("punisherx.help") ||
            !stack.sender.hasPermission("punisherx.version") ||
            !stack.sender.hasPermission("punisherx.reload") ||
            !stack.sender.hasPermission("punisherx.export") ||
            !stack.sender.hasPermission("punisherx.import")) {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        val pluginMeta = (plugin as LifecycleEventOwner).pluginMeta
        val pdf = plugin.description
        if (args.isNotEmpty()) {
            when {
                args[0].equals("help", ignoreCase = true) -> {
                    if (stack.sender.hasPermission("punisherx.help")) {
                        val page = args.getOrNull(1)?.toIntOrNull() ?: 1
                        sendHelp(stack, page)
                    } else {
                        stack.sender.sendRichMessage("<red>You do not have permission to use this command.</red>")
                    }
                }
                args[0].equals("version", ignoreCase = true) -> {
                    if (stack.sender.hasPermission("punisherx.version")) {
                        stack.sender.sendRichMessage("\n<gray>-------------------------------------------------\n" +
                                " <gray>|\n" +
                                " <gray>|   <gold>→ <bold>" + pluginMeta.name + "</bold> ←\n" +
                                " <gray>|   <white>Author: <bold><gold>" + pdf.authors + "</gold></bold>\n" +
                                " <gray>|   <white>Website: <bold><gold><click:open_url:'" + pdf.website + "'>"  + pdf.website + "</click></gold></bold>\n" +
                                " <gray>|   <white>Version: <bold><gold>" + pluginMeta.version + "</gold></bold>\n" +
                                " <gray>|" +
                                "\n-------------------------------------------------")
                    } else {
                        stack.sender.sendRichMessage("<red>You do not have permission to use this command.</red>")
                    }
                }
                args[0].equals("reload", ignoreCase = true) -> {
                    if (stack.sender.hasPermission("punisherx.reload")) {
                        plugin.onReload()
                        stack.sender.sendRichMessage("<green>The configuration file has been reloaded.</green>")
                    } else {
                        stack.sender.sendRichMessage("<red>You do not have permission to use this command.</red>")
                    }
                }
                args[0].equals("export", ignoreCase = true) -> {
                    if (stack.sender.hasPermission("punisherx.export")) {
                        plugin.databaseHandler.exportDatabase()
                    } else {
                        stack.sender.sendRichMessage("<red>You do not have permission to use this command.</red>")
                    }
                }
                args[0].equals("import", ignoreCase = true) -> {
                    if (stack.sender.hasPermission("punisherx.import")) {
                        plugin.databaseHandler.importDatabase()
                    } else {
                        stack.sender.sendRichMessage("<red>You do not have permission to use this command.</red>")
                    }
                }
            }
        } else {
            stack.sender.sendRichMessage("<green>Type </green><gold>/punisherx help</gold> <green>to see available commands</green>")
        }
    }

    private fun sendHelp(stack: CommandSourceStack, page: Int) {
        val commands = listOf(
            "  <gold>/punisherx help <gray>- <white>Displays this prompt.",
            "  <gold>/punisherx version <gray>- <white>Shows plugin info.",
            "  <gold>/punisherx reload <gray>- <white>Reloads the configuration file.",
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
            "  <gold>/setjail radius <number> <gray>- <white>Setting up the jail location.",
            " ",
            " ",
            " ",
            " ",
            " ",
            " "
        )

        val itemsPerPage = 12
        val totalPages = (commands.size + itemsPerPage - 1) / itemsPerPage
        val currentPage = page.coerceIn(1, totalPages)

        stack.sender.sendRichMessage(" <gray>+-------------------------------------------------")
        stack.sender.sendRichMessage(" <gray>|    <gold>Available commands for ${plugin.pluginMeta.name}:")
        stack.sender.sendRichMessage(" <gray>|")

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = (currentPage * itemsPerPage).coerceAtMost(commands.size)
        for (i in startIndex until endIndex) {
            stack.sender.sendRichMessage(" <gray>|  ${commands[i]}")
        }

        val prevPage = if (currentPage > 1) currentPage - 1 else totalPages
        val nextPage = if (currentPage < totalPages) currentPage + 1 else 1
        stack.sender.sendRichMessage(" <gray>|")
        stack.sender.sendRichMessage(" <gray>|")
        stack.sender.sendRichMessage(
            " <gray>| (Page $currentPage/$totalPages) <click:run_command:'/prx help $prevPage'><white>[Previous]</white></click>   " +
                    "<click:run_command:'/prx help $nextPage'><white>[Next]</white></click>"
        )
        stack.sender.sendRichMessage(" <gray>|")
        stack.sender.sendRichMessage(" <gray>+-------------------------------------------------")
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!stack.sender.hasPermission("punisherx.help") ||
            !stack.sender.hasPermission("punisherx.version") ||
            !stack.sender.hasPermission("punisherx.reload") ||
            !stack.sender.hasPermission("punisherx.export") ||
            !stack.sender.hasPermission("punisherx.import")) {
            return emptyList()
        }
        return when (args.size) {
            1 -> listOf("help", "version", "reload", "export", "import")
            else -> emptyList()
        }
    }
}
