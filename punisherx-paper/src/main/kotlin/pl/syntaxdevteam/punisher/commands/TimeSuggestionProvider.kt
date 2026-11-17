package pl.syntaxdevteam.punisher.commands

object TimeSuggestionProvider {
    fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        return (1..999).flatMap { i -> units.map { unit -> "$i$unit" } }
    }
}
