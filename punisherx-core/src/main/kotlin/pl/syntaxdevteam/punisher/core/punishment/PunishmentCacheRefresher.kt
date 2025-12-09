package pl.syntaxdevteam.punisher.core.punishment

import pl.syntaxdevteam.punisher.core.platform.SchedulerAdapter

/**
 * Bridges platform events and the shared punishment query service.
 * After any punishment mutation the platform should trigger a refresh so
 * cached data stays in sync with the database before queries are served.
 */
class PunishmentCacheRefresher(
    private val queryService: PunishmentQueryService,
    private val scheduler: SchedulerAdapter,
) {

    /**
     * Invalidates cache for the given target and preloads the latest active
     * punishments asynchronously using the provided scheduler.
     */
    fun refreshAsync(target: String) {
        scheduler.runAsync(Runnable {
            queryService.invalidate(target)
            queryService.getActivePunishments(target)
        })
    }

    /**
     * Only invalidates cache without preloading, useful for revoke flows
     * where the next query should observe an empty set.
     */
    fun evictAsync(target: String) {
        scheduler.runAsync(Runnable { queryService.invalidate(target) })
    }
}
