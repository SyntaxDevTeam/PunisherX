package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

object TimeSuggestionProvider {
    fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        return (1..999).flatMap { i -> units.map { unit -> "$i$unit" } }
    }
}
