package pl.syntaxdevteam.punisher.compatibility

import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.dialogs.ChangeReasonDialogService
import pl.syntaxdevteam.punisher.dialogs.PunishmentListDialogService
import pl.syntaxdevteam.punisher.dialogs.ReportDialogService
import pl.syntaxdevteam.punisher.dialogs.ReportManagementDialogService
import pl.syntaxdevteam.punisher.listeners.ModernLoginListener

class PaperModernFeaturesImpl(private val plugin: PunisherX) : PaperModernFeatures {
    override fun loginListener() = ModernLoginListener(plugin)

    override fun openPunishmentList(
        player: Player,
        title: Component,
        subtitle: Component,
        entries: List<PunishmentData>,
        page: Int,
        hasNext: Boolean,
        loadPage: (Int) -> Unit
    ) = PunishmentListDialogService(plugin).open(player, title, subtitle, entries, page, hasNext, loadPage)

    override fun openChangeReason(player: Player, preselectedId: Int?) =
        ChangeReasonDialogService(plugin).open(player, preselectedId)

    override fun openReport(player: Player, preselectedTarget: OfflinePlayer?) =
        ReportDialogService(plugin).open(player, preselectedTarget)

    override fun openReportInbox(player: Player, page: Int, closed: Boolean) =
        ReportManagementDialogService(plugin).openInbox(player, page, closed)

    override fun openReportDetails(player: Player, id: Int) =
        ReportManagementDialogService(plugin).openDetails(player, id)
}
