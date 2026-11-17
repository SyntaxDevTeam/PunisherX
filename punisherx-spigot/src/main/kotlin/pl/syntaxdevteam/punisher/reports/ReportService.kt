package pl.syntaxdevteam.punisher.reports

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.sendMessage
import pl.syntaxdevteam.punisher.databases.ReportSubmissionResult as DatabaseReportSubmissionResult
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

enum class ReportSubmissionStatus {
    SUCCESS,
    ALREADY_REPORTED_TARGET,
    OPEN_REPORT_LIMIT_REACHED,
    SELF_REPORT,
    INVALID_REASON,
    DATABASE_ERROR
}

data class ReportSubmissionResult(
    val status: ReportSubmissionStatus,
    val targetReportCount: Int = 0
)

/**
 * Owns report validation and notifications so every report entry point
 * applies the same rules.
 */
class ReportService(private val plugin: PunisherX) {
    private val maxOpenReports: Int
        get() = plugin.config.getInt("reports.max-open-per-reporter", 3).coerceAtLeast(1)

    fun hasReachedOpenReportLimit(reporter: Player): Boolean =
        plugin.databaseHandler.countOpenReportsByReporter(reporter.uniqueId) >= maxOpenReports

    fun submit(reporter: Player, target: OfflinePlayer, rawReason: String): ReportSubmissionResult {
        val reason = rawReason.trim()
        if (reporter.uniqueId == target.uniqueId) {
            return ReportSubmissionResult(ReportSubmissionStatus.SELF_REPORT)
        }
        if (reason.length !in 3..255 || reason.any { it.isISOControl() }) {
            return ReportSubmissionResult(ReportSubmissionStatus.INVALID_REASON)
        }

        return when (
            val result = plugin.databaseHandler.submitReport(
                reporter.uniqueId,
                target.uniqueId,
                reason,
                maxOpenReports
            )
        ) {
            is DatabaseReportSubmissionResult.Accepted ->
                ReportSubmissionResult(ReportSubmissionStatus.SUCCESS, result.suspectReportCount)
            DatabaseReportSubmissionResult.ReporterAlreadyReportedSuspect ->
                ReportSubmissionResult(ReportSubmissionStatus.ALREADY_REPORTED_TARGET)
            DatabaseReportSubmissionResult.ReporterReachedOpenReportLimit ->
                ReportSubmissionResult(ReportSubmissionStatus.OPEN_REPORT_LIMIT_REACHED)
            DatabaseReportSubmissionResult.DatabaseError ->
                ReportSubmissionResult(ReportSubmissionStatus.DATABASE_ERROR)
        }
    }

    fun submitAndNotify(reporter: Player, target: OfflinePlayer, rawReason: String): ReportSubmissionResult {
        val result = submit(reporter, target, rawReason)
        val targetName = target.name ?: target.uniqueId.toString()
        val reason = rawReason.trim()

        when (result.status) {
            ReportSubmissionStatus.SUCCESS -> {
                val placeholders = mapOf(
                    "target" to targetName,
                    "reason" to reason,
                    "count" to result.targetReportCount.toString()
                )
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponent("reports", "report-sent", placeholders)
                )
                plugin.server.onlinePlayers
                    .filter {
                        PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_REPORTS)
                    }
                    .forEach { staff ->
                        staff.sendMessage(
                            plugin.messageHandler.stringMessageToComponentNoPrefix(
                                "reports",
                                "admin-notify",
                                placeholders + mapOf("reporter" to reporter.name)
                            )
                        )
                    }
            }

            ReportSubmissionStatus.ALREADY_REPORTED_TARGET ->
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponent("reports", "already-reported-target")
                )

            ReportSubmissionStatus.OPEN_REPORT_LIMIT_REACHED ->
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponent(
                        "reports",
                        "report-limit-reached",
                        mapOf("limit" to maxOpenReports.toString())
                    )
                )

            ReportSubmissionStatus.SELF_REPORT ->
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponent("reports", "cannot-report-self")
                )

            ReportSubmissionStatus.INVALID_REASON ->
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponent("reports", "invalid-reason")
                )

            ReportSubmissionStatus.DATABASE_ERROR ->
                reporter.sendMessage(
                    plugin.messageHandler.stringMessageToComponentNoPrefix("error", "db_error")
                )
        }
        return result
    }
}
