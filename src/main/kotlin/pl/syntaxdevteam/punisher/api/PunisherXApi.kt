package pl.syntaxdevteam.punisher.api

 import pl.syntaxdevteam.punisher.databases.PunishmentData
 import java.util.concurrent.CompletableFuture

 /**
  * API interface for PunisherX plugin.
  */
 interface PunisherXApi {

     /**
      * Retrieves the last ten punishment history records for a given player.
      *
      * @param uuid The UUID of the player.
      * @return A CompletableFuture containing a list of the last ten PunishmentData records.
      */
     fun getLastTenPunishmentHistory(uuid: String): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves the last ten active punishments for a given player.
      *
      * @param uuid The UUID of the player.
      * @return A CompletableFuture containing a list of the last ten active PunishmentData records.
      */
     fun getLastTenActivePunishments(uuid: String): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves all active punishments for a given player, optionally filtered by type.
      *
      * @param uuid The UUID of the player.
      * @param type The type of punishment to filter by (optional).
      * @return A CompletableFuture containing a list of active PunishmentData records.
      */
     fun getActivePunishments(uuid: String, type: String? = "ALL"): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves the punishment history for a given player, optionally filtered by type.
      *
      * @param uuid The UUID of the player.
      * @param type The type of punishment to filter by (optional).
      * @return A CompletableFuture containing a list of PunishmentData records from the history.
      */
     fun getPunishmentHistory(uuid: String, type: String? = "ALL"): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves a list of banned players.
      *
      * @param limit The maximum number of records to return.
      * @param offset The number of records to skip.
      * @return A CompletableFuture containing a list of PunishmentData records for banned players.
      */
     fun getBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves a list of banned players from the history.
      *
      * @param limit The maximum number of records to return.
      * @param offset The number of records to skip.
      * @return A CompletableFuture containing a list of PunishmentData records for banned players from the history.
      */
     fun getHistoryBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>>

     /**
      * Retrieves a list of jailed players.
      *
      * @param limit The maximum number of records to return.
      * @param offset The number of records to skip.
      * @return A CompletableFuture containing a list of PunishmentData records for jailed players.
      */
     fun getJailedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>>
 }