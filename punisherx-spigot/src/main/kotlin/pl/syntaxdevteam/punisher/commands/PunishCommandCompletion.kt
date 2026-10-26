package pl.syntaxdevteam.punisher.commands

/** Completion and quoted argument handling for the basic command/Spigot entry points. */
internal object PunishCommandCompletion {
    private val flags = listOf("--force", "--silent")
    private data class Token(val value: String, val start: Int, val quoted: Boolean, val closed: Boolean = true)

    private fun tokens(input: String): List<Token> {
        val result = mutableListOf<Token>()
        var position = 0
        while (position < input.length) {
            if (input[position].isWhitespace()) {
                position++
                continue
            }
            val start = position
            val quoted = input[position] == '"'
            var closed = !quoted
            val value = StringBuilder()
            if (quoted) position++
            while (position < input.length) {
                val char = input[position++]
                if (quoted && char == '"') { closed = true; break }
                if (!quoted && char.isWhitespace()) { position--; break }
                if (quoted && char == '\\' && position < input.length &&
                    input[position] in charArrayOf('"', '\\')) {
                    value.append(input[position++])
                } else value.append(char)
            }
            result += Token(value.toString(), start, quoted, closed)
        }
        return result
    }

    fun arguments(args: Array<String>): Array<String> =
        if (args.any { it.startsWith('"') }) tokens(args.joinToString(" ")).map { it.value }.toTypedArray()
        else args

    fun templates(names: Collection<String>, input: String): List<String> {
        val prefix = if (input.startsWith('"')) tokens(input).firstOrNull()?.value.orEmpty() else input
        return names.filter { it.startsWith(prefix, ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .map { name ->
                if (input.startsWith('"') || name.isEmpty() || name.any {
                    !(it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "_-.+")
                }) "\"${name.replace("\\", "\\\\").replace("\"", "\\\"")}\"" else name
            }
    }

    fun suggest(
        args: Array<String>,
        players: Collection<String>,
        names: Collection<String>,
        levels: (String) -> Collection<Int>
    ): List<String> {
        val input = args.joinToString(" ")
        val parsed = tokens(input)
        // A trailing space inside an unfinished quoted name still belongs to that name.
        val last = parsed.lastOrNull()
        val inQuote = last?.quoted == true && !last.closed
        val current = if (last == null || (input.endsWith(' ') && !inQuote)) Token("", input.length, false) else last
        val completed = if (current === last) parsed.dropLast(1) else parsed
        val positional = completed.filter { it.value !in flags }
        val candidates = when (positional.size) {
            0 -> players.filter { it.startsWith(current.value, ignoreCase = true) }.sorted()
            1 -> templates(names, input.substring(current.start))
            2 -> (levels(positional[1].value).sorted().map { it.toString() } + flags)
                .filter { it.startsWith(current.value) && it !in completed.map { token -> token.value } }
            3 -> flags.filter { it.startsWith(current.value) && it !in completed.map { token -> token.value } }
            else -> emptyList()
        }
        // Bukkit replaces only the last space-delimited word, even inside quoted names.
        val previousLength = (input.length - args.lastOrNull().orEmpty().length - current.start).coerceAtLeast(0)
        return candidates.map { it.drop(previousLength) }
    }
}
