package pl.syntaxdevteam.punisher.loader

import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

/** Removes registrations left by a previous classloader after a plugin hot reload. */
internal object PluginRegistrations {
    /** Triumph's listeners are registered by a static initializer, not TriumphGui.init(). */
    fun unregisterForReload(plugin: Plugin) {
        HandlerList.getHandlerLists().forEach { handlers ->
            handlers.registeredListeners.filter { registered ->
                registered.plugin === plugin && !isInfrastructure(registered.listener.javaClass.name)
            }.forEach { handlers.unregister(it) }
        }
    }

    internal fun isInfrastructure(name: String): Boolean = name in setOf(
        "dev.triumphteam.gui.guis.GuiListener",
        "dev.triumphteam.gui.guis.InteractionModifierListener"
    ) || name.startsWith("pl.syntaxdevteam.core.SyntaxCore" + '$')

    fun clearStale(plugin: Plugin, cancelTasks: (Plugin) -> Unit): Int {
        val services = plugin.server.servicesManager
        val owners = HandlerList.getHandlerLists()
            .flatMap { it.registeredListeners.toList() }
            .map { it.plugin }
            .plus(services.knownServices.flatMap { service ->
                services.getRegistrations(service).map { it.plugin }
            })
            .filter { it !== plugin && it.name == plugin.name }
            .distinct()
        owners.forEach { owner ->
            HandlerList.unregisterAll(owner)
            services.unregisterAll(owner)
            cancelTasks(owner)
        }
        return owners.size
    }
}
