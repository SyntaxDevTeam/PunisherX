package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import kotlin.test.Test
import kotlin.test.assertEquals

class PunishBrigadierCompletionTest {
    private val names = listOf("Spam", "Chat spam", "Zażółć", "A\"B", "Path\\Name")
    private val dispatcher = CommandDispatcher<Any>().apply {
        register(literal<Any>("punish").then(
            argument<Any, String>("player", StringArgumentType.word()).then(
                argument<Any, String>("template", StringArgumentType.string())
                    .suggests { _, builder ->
                        PunishCommandCompletion.templates(names, builder.remaining).forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }.executes { 1 }
            )
        ))
    }

    @Test fun `completion replaces the template argument and yields an executable command`() {
        for ((input, expected) in listOf(
            "punish Steve sp" to "punish Steve Spam",
            "punish Steve Ch" to "punish Steve \"Chat spam\"",
            "punish Steve \"Chat s" to "punish Steve \"Chat spam\""
        )) {
            val suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, Any())).get().list
            assertEquals(listOf(expected), suggestions.map { it.apply(input) })
            assertEquals(1, dispatcher.execute(expected, Any()))
        }
    }

    @Test fun `suggested names survive Brigadier quoting and escaping`() {
        for (name in names) {
            val completion = PunishCommandCompletion.templates(listOf(name), "").single()
            assertEquals(name, StringArgumentType.string().parse(StringReader(completion)))
        }
    }
}
