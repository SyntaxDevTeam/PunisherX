package pl.syntaxdevteam.punisher.compatibility

import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData

interface PaperModernFeatures {
    fun loginListener(): Listener

    fun openPunishmentList(
        player: Player,
        title: Component,
        subtitle: Component,
        entries: List<PunishmentData>,
        page: Int,
        hasNext: Boolean,
        loadPage: (Int) -> Unit
    )

    fun openChangeReason(player: Player, preselectedId: Int?)
    fun openReport(player: Player, preselectedTarget: OfflinePlayer?): Boolean
    fun openReportInbox(player: Player, page: Int, closed: Boolean): Boolean
    fun openReportDetails(player: Player, id: Int): Boolean

    companion object {
        // The calling feature checks its server-version gate before loading the implementation.
        fun load(plugin: PunisherX): PaperModernFeatures =
            Class.forName("pl.syntaxdevteam.punisher.compatibility.PaperModernFeaturesImpl")
                .asSubclass(PaperModernFeatures::class.java)
                .getConstructor(PunisherX::class.java)
                .newInstance(plugin)
    }
}
