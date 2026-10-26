package pl.syntaxdevteam.punisher.commands

/** Invocation-local flags, removed before interpreting targets, times, reasons or levels. */
internal class PunishmentCommandOptions(
    val arguments: Array<String>,
    val force: Boolean,
    val silent: Boolean
) {
    companion object {
        fun parse(args: Array<String>): PunishmentCommandOptions = PunishmentCommandOptions(
            arguments = args.filterNot { it == "--force" || it == "--silent" }.toTypedArray(),
            force = "--force" in args,
            silent = "--silent" in args
        )
    }
}
