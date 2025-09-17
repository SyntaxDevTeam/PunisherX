package pl.syntaxdevteam.punisher.basic

import pl.syntaxdevteam.punisher.PunisherX
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimeHandler(
    private val messageProvider: MessageProvider,
    private val currentTime: () -> Long = System::currentTimeMillis
) {

    constructor(plugin: PunisherX) : this(
        MessageProvider { path, key -> plugin.messageHandler.getCleanMessage(path, key) },
        System::currentTimeMillis
    )

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parseTime(time: String): Long {
        val amount = time.dropLast(1).toLong()
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
        if (time == null) return messageProvider.getCleanMessage("formatTime", "undefined")

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

        val amount = time.dropLast(1).toLong()
        val unit = time.last()

        return when (unit) {
            's' -> "$amount ${getLocalizedMessage("second", amount)}"
            'm' -> "$amount ${getLocalizedMessage("minute", amount)}"
            'h' -> "$amount ${getLocalizedMessage("hour", amount)}"
            'd' -> "$amount ${getLocalizedMessage("day", amount)}"
            else -> messageProvider.getCleanMessage("formatTime", "undefined")
        }
    }

    fun parseDate(date: String): Long? = try {
        LocalDateTime.parse(date, dateFormatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }

    fun getOfflineDuration(lastUpdated: String): String {
        val ts = parseDate(lastUpdated) ?: return messageProvider.getCleanMessage("error", "no_data")
        var seconds = ((currentTime() - ts) / 1000).coerceAtLeast(0)
        val years = seconds / (60 * 60 * 24 * 365)
        seconds %= 60 * 60 * 24 * 365
        val months = seconds / (60 * 60 * 24 * 30)
        seconds %= 60 * 60 * 24 * 30
        val weeks = seconds / (60 * 60 * 24 * 7)
        seconds %= 60 * 60 * 24 * 7
        val days = seconds / (60 * 60 * 24)
        seconds %= 60 * 60 * 24
        val hours = seconds / (60 * 60)
        seconds %= 60 * 60
        val minutes = seconds / 60
        seconds %= 60
        val parts = mutableListOf<String>()
        if (years > 0) parts.add("${years}Y")
        if (months > 0) parts.add("${months}M")
        if (weeks > 0) parts.add("${weeks}W")
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")
        return parts.joinToString(" ")
    }

    private fun getLocalizedMessage(unit: String, amount: Long): String {
        val unitPath = "formatTime.$unit"
        return when (amount) {
            1L -> messageProvider.getCleanMessage(unitPath, "one")
            in 2..4 -> messageProvider.getCleanMessage(unitPath, "few")
            else -> messageProvider.getCleanMessage(unitPath, "many")
        }
    }

    fun interface MessageProvider {
        fun getCleanMessage(path: String, key: String): String
    }
}
