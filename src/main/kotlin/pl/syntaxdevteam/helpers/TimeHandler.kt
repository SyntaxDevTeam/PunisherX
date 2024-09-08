package pl.syntaxdevteam.helpers

import io.papermc.paper.plugin.configuration.PluginMeta
import pl.syntaxdevteam.PunisherX

@Suppress("UnstableApiUsage")
class TimeHandler(plugin: PunisherX, pluginMetas: PluginMeta, private val language: String) {
    private val messageHandler = MessageHandler(plugin, pluginMetas)

    fun parseTime(time: String): Long {
        val amount = time.substring(0, time.length - 1).toLong()
        val unit = time.last()

        return when (unit) {
            's' -> amount
            'm' -> amount * 60
            'h' -> amount * 60 * 60
            'd' -> amount * 60 * 60 * 24
            else -> 0
        }
    }

    fun formatTime(time: String?): String {
        if (time == null) return messageHandler.getLogMessage("formatTime", "undefined")

        val isNumeric = time.all { it.isDigit() }
        if (isNumeric) {
            val totalSeconds = time.toLong()
            val days = totalSeconds / (60 * 60 * 24)
            val hours = (totalSeconds % (60 * 60 * 24)) / (60 * 60)
            val minutes = (totalSeconds % (60 * 60)) / 60
            val seconds = totalSeconds % 60

            val dayMessage = getLocalizedMessage("day", days)
            val hourMessage = getLocalizedMessage("hour", hours)
            val minuteMessage = getLocalizedMessage("minute", minutes)
            val secondMessage = getLocalizedMessage("second", seconds)

            val timeComponents = mutableListOf<String>()
            if (days > 0) timeComponents.add("$days $dayMessage")
            if (hours > 0) timeComponents.add("$hours $hourMessage")
            if (minutes > 0) timeComponents.add("$minutes $minuteMessage")
            if (seconds > 0) timeComponents.add("$seconds $secondMessage")

            return timeComponents.joinToString(", ")
        }

        val amount = time.substring(0, time.length - 1).toLong()
        val unit = time.last()

        return when (unit) {
            's' -> "$amount ${getLocalizedMessage("second", amount)}"
            'm' -> "$amount ${getLocalizedMessage("minute", amount)}"
            'h' -> "$amount ${getLocalizedMessage("hour", amount)}"
            'd' -> "$amount ${getLocalizedMessage("day", amount)}"
            else -> messageHandler.getLogMessage("formatTime", "undefined")
        }
    }

    private fun getLocalizedMessage(unit: String, amount: Long): String {
        return if (language == "PL") {
            when {
                amount == 1L -> messageHandler.getLogMessage("formatTime.pl.$unit", "one")
                amount in 2..4 -> messageHandler.getLogMessage("formatTime.pl.$unit", "few")
                else -> messageHandler.getLogMessage("formatTime.pl.$unit", "many")
            }
        } else {
            when {
                amount == 1L -> messageHandler.getLogMessage("formatTime.$unit", "one")
                amount in 2..4 -> messageHandler.getLogMessage("formatTime.$unit", "few")
                else -> messageHandler.getLogMessage("formatTime.$unit", "many")
            }
        }
    }
}
