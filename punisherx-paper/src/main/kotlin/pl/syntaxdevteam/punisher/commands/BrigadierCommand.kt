package pl.syntaxdevteam.punisher.commands
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
interface BrigadierCommand {
    fun build(name: String): LiteralCommandNode<CommandSourceStack>
}
