package pl.syntaxdevteam.punisher.bridge.velocity

data class BridgeEvent(
    val id: Long,
    val action: String,
    val target: String,
    val reason: String,
    val end: Long
)
