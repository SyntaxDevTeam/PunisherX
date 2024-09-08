package pl.syntaxdevteam.helpers

import io.papermc.paper.plugin.configuration.PluginMeta
import pl.syntaxdevteam.PunisherX

@Suppress("UnstableApiUsage")
class TimeHandler(plugin: PunisherX, pluginMetas: PluginMeta) {
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

            val dayMessage = if (days == 1L) messageHandler.getLogMessage("formatTime", "day") else messageHandler.getLogMessage("formatTime", "days")
            val hourMessage = if (hours == 1L) messageHandler.getLogMessage("formatTime", "hour") else messageHandler.getLogMessage("formatTime", "hours")
            val minuteMessage = if (minutes == 1L) messageHandler.getLogMessage("formatTime", "minute") else messageHandler.getLogMessage("formatTime", "minutes")
            val secondMessage = if (seconds == 1L) messageHandler.getLogMessage("formatTime", "second") else messageHandler.getLogMessage("formatTime", "seconds")

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
            's' -> "$amount ${if (amount == 1L) messageHandler.getLogMessage("formatTime", "second") else messageHandler.getLogMessage("formatTime", "seconds")}"
            'm' -> "$amount ${if (amount == 1L) messageHandler.getLogMessage("formatTime", "minute") else messageHandler.getLogMessage("formatTime", "minutes")}"
            'h' -> "$amount ${if (amount == 1L) messageHandler.getLogMessage("formatTime", "hour") else messageHandler.getLogMessage("formatTime", "hours")}"
            'd' -> "$amount ${if (amount == 1L) messageHandler.getLogMessage("formatTime", "day") else messageHandler.getLogMessage("formatTime", "days")}"
            else -> messageHandler.getLogMessage("formatTime", "undefined")
        }
    }
}
