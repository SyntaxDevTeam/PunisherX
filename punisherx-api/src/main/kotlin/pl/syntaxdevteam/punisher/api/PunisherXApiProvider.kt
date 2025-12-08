package pl.syntaxdevteam.punisher.api

/**
 * Provider for sharing the current PunisherXApi implementation.
 */
object PunisherXApiProvider {
    @Volatile
    private var provider: PunisherXApi? = null

    fun get(): PunisherXApi = requireNotNull(provider) {
        "PunisherXApi has not been initialized"
    }

    fun set(api: PunisherXApi) {
        provider = api
    }
}
