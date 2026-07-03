package pl.syntaxdevteam.punisher.compatibility

import net.kyori.adventure.text.Component

internal fun multilineComponent(lines: Iterable<Component>): Component {
    var message: Component = Component.empty()
    lines.forEachIndexed { index, line ->
        if (index > 0) {
            message = message.append(Component.newline())
        }
        message = message.append(line)
    }
    return message
}
