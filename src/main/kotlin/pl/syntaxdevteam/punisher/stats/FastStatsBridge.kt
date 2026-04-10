package pl.syntaxdevteam.punisher.stats


import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method

class FastStatsBridge(
    private val plugin: JavaPlugin,
    private val token: String
) {
    private var errorTracker: Any? = null
    private var metrics: Any? = null
    private var available = false

    fun ready() {
        ensureInitialized()
        invokeNoArg(metrics, "ready")
    }

    fun shutdown() {
        invokeNoArg(metrics, "shutdown")
    }

    fun trackError(throwable: Throwable) {
        invokeOneArg(errorTracker, "trackError", Throwable::class.java, throwable)
    }

    private fun ensureInitialized() {
        if (available || metrics != null) {
            return
        }

        try {
            val errorTrackerClass = Class.forName("dev.faststats.core.ErrorTracker")
            val contextAware = errorTrackerClass.getMethod("contextAware")
            errorTracker = contextAware.invoke(null)

            val bukkitMetricsClass = Class.forName("dev.faststats.bukkit.BukkitMetrics")
            val factory = bukkitMetricsClass.getMethod("factory").invoke(null)
            val builderClass = factory.javaClass
            invokeCompatible(builderClass, factory, "token", token)
            invokeCompatible(builderClass, factory, "errorTracker", errorTracker!!)
            metrics = invokeCompatible(builderClass, factory, "create", plugin)
            available = true
        } catch (exception: Exception) {
            plugin.logger.warning("[CleanerX] FastStats is unavailable, metrics disabled: ${exception::class.java.simpleName}: ${exception.message}")
        }
    }

    private fun invokeNoArg(target: Any?, methodName: String) {
        if (!available || target == null) {
            return
        }
        try {
            target.javaClass.getMethod(methodName).invoke(target)
        } catch (_: Exception) {
            // no-op
        }
    }

    private fun invokeOneArg(target: Any?, methodName: String, paramType: Class<*>, argument: Any) {
        if (!available || target == null) {
            return
        }
        try {
            target.javaClass.getMethod(methodName, paramType).invoke(target, argument)
        } catch (_: Exception) {
            // no-op
        }
    }

    private fun invokeCompatible(targetClass: Class<*>, target: Any, methodName: String, argument: Any): Any? {
        val candidate: Method = targetClass.methods.firstOrNull { method ->
            method.name == methodName &&
                    method.parameterCount == 1 &&
                    method.parameters[0].type.isAssignableFrom(argument.javaClass)
        } ?: throw NoSuchMethodException("No compatible '$methodName' method found in ${targetClass.name}")

        candidate.isAccessible = true
        return candidate.invoke(target, argument)
    }
}
