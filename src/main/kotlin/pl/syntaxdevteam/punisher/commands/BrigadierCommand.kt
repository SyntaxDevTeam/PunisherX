package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack

interface BrigadierCommand {
    fun execute(stack: CommandSourceStack, args: List<String>)

    fun suggest(stack: CommandSourceStack, args: List<String>): List<String> = emptyList()

    fun build(name: String): LiteralCommandNode<CommandSourceStack>
}
