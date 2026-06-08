package pl.syntaxdevteam.punisher.hooks

data class PlayerSnapshot(
    val location: String,
    val world: String,
    val health: String,
    val food: String,
    val level: String,
    val ping: String,
    val uuid: String,
    val playTime: String,
    val lastSeen: String
)
