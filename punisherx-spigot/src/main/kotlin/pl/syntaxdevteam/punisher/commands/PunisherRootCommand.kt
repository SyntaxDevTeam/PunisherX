package pl.syntaxdevteam.punisher.commands

import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

/**
 * Thin safety wrapper around the historical PunisherX root command.
 * It keeps the existing implementation untouched while exposing /prx validate.
 */
class PunisherRootCommand(private val plugin: PunisherX) : BasicCommand {
    private val delegate = PunishesXCommands(plugin)

    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        if (args.firstOrNull()?.equals("validate", ignoreCase = true) != true) {
            delegate.execute(stack, args)
            return
        }

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }

        val report = plugin.validateConfiguration()
        val prefix = plugin.messageHandler.getPrefix()
        if (report.valid) {
            stack.sender.sendMessage(
                plugin.messageHandler.miniMessageFormat(
                    "$prefix <green>Configuration validation passed. No YAML syntax errors were found.</green>"
                )
            )
            return
        }

        stack.sender.sendMessage(
            plugin.messageHandler.miniMessageFormat(
                "$prefix <red>Configuration validation failed. No files were modified.</red>"
            )
        )
        report.issues.forEach { issue ->
            val path = report.relativePath(issue.file).safeForMiniMessage()
            val error = issue.error.safeForMiniMessage()
            stack.sender.sendMessage(
                plugin.messageHandler.miniMessageFormat("<red> • $path:</red> <gray>$error</gray>")
            )
        }
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        val suggestions = delegate.suggest(stack, args).toMutableSet()
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            when {
                args.isEmpty() -> suggestions += "validate"
                args.size == 1 && "validate".startsWith(args[0], ignoreCase = true) -> suggestions += "validate"
            }
        }
        return suggestions.sorted()
    }

    private fun String.safeForMiniMessage(): String =
        replace('<', '[').replace('>', ']').replace('&', '＆')
}
