package pl.syntaxdevteam.punisher.loader

import org.bukkit.Server
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.ServicesManager
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginRegistrationsTest {
    private fun owner(name: String = "PunisherX"): Plugin = mock<Plugin>().also {
        whenever(it.name).thenReturn(name)
    }

    private class ChatEvent(private val list: HandlerList) : Event(true) {
        override fun getHandlers() = list
    }

    private fun listener(owner: Plugin, action: () -> Unit) = RegisteredListener(
        mock<Listener>(), { _, _ -> action() }, EventPriority.HIGHEST, owner, false
    )

    @Test
    fun `new instance removes old chat listeners without removing current or other plugins`() {
        val current = owner()
        val previous = owner()
        val other = owner("OtherPlugin")
        val server = mock<Server>()
        val services = mock<ServicesManager>()
        whenever(current.server).thenReturn(server)
        whenever(server.servicesManager).thenReturn(services)
        whenever(services.knownServices).thenReturn(emptyList())
        val handlers = HandlerList()
        var oldCalls = 0
        var currentCalls = 0
        var otherCalls = 0
        handlers.register(listener(previous) { oldCalls++ })
        handlers.register(listener(previous) { oldCalls++ })
        handlers.register(listener(current) { currentCalls++ })
        handlers.register(listener(other) { otherCalls++ })
        val cancelledOwners = mutableListOf<Plugin>()
        try {
            assertEquals(1, PluginRegistrations.clearStale(current) { cancelledOwners += it })
            handlers.registeredListeners.forEach { it.callEvent(ChatEvent(handlers)) }
            assertEquals(0, oldCalls)
            assertEquals(1, currentCalls)
            assertEquals(1, otherCalls)
            assertEquals(listOf(previous), cancelledOwners)
            verify(services).unregisterAll(previous)
            verify(services, never()).unregisterAll(current)
            verify(services, never()).unregisterAll(other)
        } finally {
            handlers.unregister(previous)
            handlers.unregister(current)
            handlers.unregister(other)
        }
    }

    @Test
    fun `stale service provider is removed even without remaining event listeners`() {
        val current = owner()
        val previous = owner()
        val services = mock<ServicesManager>()
        val server = mock<Server>()
        whenever(current.server).thenReturn(server)
        whenever(server.servicesManager).thenReturn(services)
        whenever(services.knownServices).thenReturn(listOf(Runnable::class.java))
        whenever(services.getRegistrations(Runnable::class.java)).thenReturn(listOf(
            RegisteredServiceProvider(Runnable::class.java, Runnable {}, ServicePriority.Normal, previous),
            RegisteredServiceProvider(Runnable::class.java, Runnable {}, ServicePriority.Normal, current)
        ))
        val cancelledOwners = mutableListOf<Plugin>()
        assertEquals(1, PluginRegistrations.clearStale(current) { cancelledOwners += it })
        assertEquals(listOf(previous), cancelledOwners)
        verify(services).unregisterAll(previous)
        verify(services, never()).unregisterAll(current)
    }
}
