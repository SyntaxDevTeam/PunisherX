package pl.syntaxdevteam.punisher.commands.arguments

import com.google.common.net.InetAddresses
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import pl.syntaxdevteam.core.database.DatabaseType
import java.util.Locale
import java.util.concurrent.CompletableFuture

private fun suggestMatching(values: Iterable<String>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
    val remaining = builder.remainingLowerCase
    values
        .filter { it.lowercase(Locale.ROOT).startsWith(remaining) }
        .forEach { builder.suggest(it) }
    return builder.buildFuture()
}

data class PunishmentDuration(val raw: String, val seconds: Long)

enum class PunishmentCheckType {
    ALL,
    BAN,
    JAIL,
    MUTE,
    WARN
}

class PunishmentDurationArgumentType private constructor() : ArgumentType<PunishmentDuration> {
    override fun parse(reader: StringReader): PunishmentDuration {
        val raw = reader.readUnquotedString()
        return parseRaw(raw) ?: throw INVALID_DURATION.createWithContext(reader)
    }

    override fun getExamples(): Collection<String> = listOf("10m", "1h", "2d")

    companion object {
        private val INVALID_DURATION = SimpleCommandExceptionType(LiteralMessage("Invalid duration"))
        private val TIME_REGEX = Regex("^(\\d+)([smhd])$")

        fun duration(): PunishmentDurationArgumentType = PunishmentDurationArgumentType()

        fun getDuration(context: CommandContext<*>, name: String): PunishmentDuration {
            return context.getArgument(name, PunishmentDuration::class.java)
        }

        fun parseRaw(raw: String): PunishmentDuration? {
            val match = TIME_REGEX.matchEntire(raw) ?: return null
            val amount = match.groupValues[1].toLongOrNull() ?: return null
            val unit = match.groupValues[2].first()
            val seconds = when (unit) {
                's' -> amount
                'm' -> amount * 60
                'h' -> amount * 60 * 60
                'd' -> amount * 60 * 60 * 24
                else -> return null
            }
            return PunishmentDuration(raw, seconds)
        }
    }
}

class ReasonArgumentType private constructor() : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val remaining = reader.remaining
        reader.cursor = reader.totalLength
        return remaining
    }

    override fun getExamples(): Collection<String> = listOf("spamming", "cheating in chat")

    companion object {
        fun reason(): ReasonArgumentType = ReasonArgumentType()

        fun getReason(context: CommandContext<*>, name: String): String {
            return context.getArgument(name, String::class.java)
        }
    }
}

class IpAddressArgumentType private constructor() : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val raw = reader.readUnquotedString()
        if (!InetAddresses.isInetAddress(raw)) {
            throw INVALID_IP.createWithContext(reader)
        }
        return raw
    }

    override fun getExamples(): Collection<String> = listOf("127.0.0.1", "192.168.1.5")

    companion object {
        private val INVALID_IP = SimpleCommandExceptionType(LiteralMessage("Invalid IP address"))

        fun ipAddress(): IpAddressArgumentType = IpAddressArgumentType()

        fun getAddress(context: CommandContext<*>, name: String): String {
            return context.getArgument(name, String::class.java)
        }
    }
}

class DatabaseTypeArgumentType private constructor(private val values: Array<DatabaseType>) : ArgumentType<DatabaseType> {
    override fun parse(reader: StringReader): DatabaseType {
        val raw = reader.readUnquotedString()
        return values.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: throw INVALID_DB_TYPE.createWithContext(reader)
    }

    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return suggestMatching(values.map { it.name.lowercase(Locale.ROOT) }, builder)
    }

    override fun getExamples(): Collection<String> = values.take(3).map { it.name.lowercase(Locale.ROOT) }

    companion object {
        private val INVALID_DB_TYPE = SimpleCommandExceptionType(LiteralMessage("Unknown database type"))

        fun databaseType(): DatabaseTypeArgumentType = DatabaseTypeArgumentType(DatabaseType.entries.toTypedArray())

        fun getDatabaseType(context: CommandContext<*>, name: String): DatabaseType {
            return context.getArgument(name, DatabaseType::class.java)
        }
    }
}

class PunishmentCheckTypeArgumentType private constructor(private val values: Array<PunishmentCheckType>) : ArgumentType<PunishmentCheckType> {
    override fun parse(reader: StringReader): PunishmentCheckType {
        val raw = reader.readUnquotedString()
        return values.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: throw INVALID_CHECK_TYPE.createWithContext(reader)
    }

    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return suggestMatching(values.map { it.name.lowercase(Locale.ROOT) }, builder)
    }

    override fun getExamples(): Collection<String> = values.map { it.name.lowercase(Locale.ROOT) }

    companion object {
        private val INVALID_CHECK_TYPE = SimpleCommandExceptionType(LiteralMessage("Unknown check type"))

        fun checkType(): PunishmentCheckTypeArgumentType = PunishmentCheckTypeArgumentType(PunishmentCheckType.entries.toTypedArray())

        fun getCheckType(context: CommandContext<*>, name: String): PunishmentCheckType {
            return context.getArgument(name, PunishmentCheckType::class.java)
        }
    }
}

class TemplateNameArgumentType private constructor(private val templateNames: () -> Collection<String>) : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readUnquotedString()

    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return suggestMatching(templateNames(), builder)
    }

    override fun getExamples(): Collection<String> = listOf("default", "cheat")

    companion object {
        fun templateName(templateNames: () -> Collection<String>): TemplateNameArgumentType {
            return TemplateNameArgumentType(templateNames)
        }

        fun getTemplateName(context: CommandContext<*>, name: String): String {
            return context.getArgument(name, String::class.java)
        }
    }
}
