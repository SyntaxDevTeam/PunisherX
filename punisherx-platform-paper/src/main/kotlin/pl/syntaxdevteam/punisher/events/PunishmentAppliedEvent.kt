package pl.syntaxdevteam.punisher.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PunishmentAppliedEvent(val target: String) : Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
