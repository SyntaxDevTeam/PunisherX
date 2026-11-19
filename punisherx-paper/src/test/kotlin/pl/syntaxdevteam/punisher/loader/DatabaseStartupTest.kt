package pl.syntaxdevteam.punisher.loader

import org.bukkit.Server
import org.bukkit.scheduler.BukkitScheduler
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import pl.syntaxdevteam.core.logging.Logger
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.identity.IdentitySettings
import java.lang.reflect.InvocationTargetException
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DatabaseStartupTest {
    private fun invokeSetup(plugin: PunisherX) {
        val setup = PluginInitializer::class.java.getDeclaredMethod("setupDatabase")
        setup.isAccessible = true
        setup.invoke(PluginInitializer(plugin))
    }

    @Test
    fun `database must be ready before startup continues for every engine`() {
        DatabaseType.entries.forEach { type ->
            val plugin = mock<PunisherX>()
            val server = mock<Server>()
            val scheduler = mock<BukkitScheduler>()
            whenever(plugin.logger).thenReturn(mock<Logger>())
            whenever(plugin.server).thenReturn(server)
            whenever(server.scheduler).thenReturn(scheduler)
            val ready = AtomicBoolean(false)
            val settings = IdentitySettings.create("network", "auto", false)

            mockConstruction(DatabaseHandler::class.java) { database, _ ->
                whenever(database.databaseType()).thenReturn(type)
                whenever(database.isReady()).thenAnswer { ready.get() }
                doAnswer { ready.set(true); null }.whenever(database).createTables()
                whenever(database.bindIdentityDomain()).thenAnswer {
                    assertTrue(ready.get(), "$type tried to bind before schema initialization")
                    settings
                }
            }.use { databases ->
                invokeSetup(plugin)
                val database = databases.constructed().single()
                assertTrue(database.isReady(), "$type setup returned before the pool and schema were ready")
                inOrder(database, plugin) {
                    verify(database).openConnection()
                    verify(database).createTables()
                    verify(database).bindIdentityDomain()
                    verify(plugin).resetPlayerIdentities(settings)
                }
                verifyNoInteractions(scheduler)
            }
        }
    }

    @Test
    fun `failed pool or schema initialization closes database and propagates failure`() {
        listOf(false, true).forEach { failSchema ->
            val plugin = mock<PunisherX>()
            whenever(plugin.logger).thenReturn(mock<Logger>())
            val connectionError = SQLException("test database unavailable")
            mockConstruction(DatabaseHandler::class.java) { database, _ ->
                whenever(database.databaseType()).thenReturn(DatabaseType.MYSQL)
                if (failSchema) doThrow(connectionError).whenever(database).createTables()
                else doThrow(connectionError).whenever(database).openConnection()
            }.use { databases ->
                val failure = assertFailsWith<InvocationTargetException> { invokeSetup(plugin) }
                val initializationError = assertIs<IllegalStateException>(failure.cause)
                assertSame(connectionError, initializationError.cause)
                val database = databases.constructed().single()
                verify(database).closeConnection()
                if (!failSchema) verify(database, never()).createTables()
                verify(database, never()).bindIdentityDomain()
            }
        }
    }

    @Test
    fun `identity binding failure closes database without initializing player identities`() {
        val plugin = mock<PunisherX>()
        whenever(plugin.logger).thenReturn(mock<Logger>())
        val bindingError = IllegalStateException("Identity domain mismatch")
        val ready = AtomicBoolean(false)
        mockConstruction(DatabaseHandler::class.java) { database, _ ->
            whenever(database.databaseType()).thenReturn(DatabaseType.SQLITE)
            whenever(database.isReady()).thenAnswer { ready.get() }
            doAnswer { ready.set(true); null }.whenever(database).createTables()
            whenever(database.bindIdentityDomain()).thenThrow(bindingError)
            doAnswer { ready.set(false); null }.whenever(database).closeConnection()
        }.use { databases ->
            val failure = assertFailsWith<InvocationTargetException> { invokeSetup(plugin) }
            val initializationError = assertIs<IllegalStateException>(failure.cause)
            assertSame(bindingError, initializationError.cause)
            val database = databases.constructed().single()
            inOrder(database) {
                verify(database).openConnection()
                verify(database).createTables()
                verify(database).bindIdentityDomain()
                verify(database).closeConnection()
            }
            verify(plugin, never()).resetPlayerIdentities(org.mockito.kotlin.any<IdentitySettings>())
            assertFalse(database.isReady())
        }
    }
}
