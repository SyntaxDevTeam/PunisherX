package pl.syntaxdevteam.punisher.players

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import org.apache.tools.tar.TarEntry
import org.apache.tools.tar.TarInputStream
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream
import kotlin.jvm.Volatile

class GeoIPHandler(private val plugin: PunisherX) {

    private val licenseKey = plugin.config.getString("geoDatabase.licenseKey") ?: throw IllegalArgumentException("License key not found in config.yml. GeoIP functionality will be disabled.")
    private val pluginFolder = "${plugin.dataFolder.path}/geodata/"

    private val cityDatabaseFile = File(pluginFolder, "GeoLite2-City.mmdb")

    @Volatile
    private var databaseReader: DatabaseReader? = null

    private val initializationFuture = CompletableFuture<Unit>()

    init {
        val folder = File(pluginFolder)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        plugin.schedulerAdapter.runAsync(Runnable {
            try {
                prepareDatabase()
                initializationFuture.complete(Unit)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to prepare GeoIP database: ${e.message}")
                initializationFuture.completeExceptionally(e)
            }
        })
    }

    private fun prepareDatabase() {
        if (!cityDatabaseFile.exists()) {
            downloadAndExtractDatabase()
        }
        if (!cityDatabaseFile.exists()) {
            plugin.logger.severe("GeoIP database file is missing after initialization.")
            return
        }
        databaseReader = DatabaseReader.Builder(cityDatabaseFile).build()
    }

    private fun downloadAndExtractDatabase() {

        if (cityDatabaseFile.exists()) return

        val cityUri = URI("https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz")

        val connection = cityUri.toURL().openConnection() as HttpURLConnection
        try {
            connection.inputStream.use { input ->
                GZIPInputStream(input).use { gzip ->
                    TarInputStream(gzip).use { tar ->
                        var entry: TarEntry? = tar.nextEntry
                        while (entry != null) {
                            plugin.logger.debug("Found entry: ${entry.name}")
                            if (entry.name.endsWith(".mmdb")) {
                                plugin.logger.debug("Extracting MMDB file: ${entry.name}")

                                if (!cityDatabaseFile.parentFile.exists()) {
                                    plugin.logger.debug("Creating directories: ${cityDatabaseFile.parentFile.absolutePath}")
                                    cityDatabaseFile.parentFile.mkdirs()
                                }
                                try {
                                    FileOutputStream(cityDatabaseFile).use { output ->
                                        tar.copyTo(output)
                                    }
                                    plugin.logger.debug("Extracted file size: ${cityDatabaseFile.length()} bytes")
                                    if (cityDatabaseFile.length() == 0L) {
                                        plugin.logger.severe("[GeoLite2] Extracted MMDB file is empty!")
                                    } else {
                                        plugin.logger.debug("MMDB file saved successfully.")
                                    }
                                } catch (e: IOException) {
                                    plugin.logger.severe("[GeoLite2] Failed to write MMDB file: ${e.message}")
                                    throw e
                                }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        } catch (e: IOException) {
            if (connection.responseCode == 401) {
                plugin.logger.severe("[GeoLite2] Unauthorized access. Please check your license key.")
            } else {
                plugin.logger.severe("[GeoLite2] Failed to download GeoIP database: ${e.message}")
                throw e
            }
        }
    }

    fun getCountry(ip: String): String? {
        if (!initializationFuture.isDone) return "Unknown country"
        val reader = databaseReader ?: return "Unknown country"
        return try {
            val response = reader.city(InetAddress.getByName(ip))
            response.country.name
        } catch (e: AddressNotFoundException) {
            "Unknown country"
        } catch (e: Exception) {
            plugin.logger.severe("Failed to get country for IP $ip: ${e.message} [Exception]")
            "Unknown country"
        } catch (e: UnknownHostException) {
            plugin.logger.severe("Failed to get country for IP $ip: ${e.message} [UnknownHostException]")
            "Unknown country"
        }
    }

    fun getCity(ip: String): String? {
        if (!initializationFuture.isDone) return "Unknown city"
        val reader = databaseReader ?: return "Unknown city"
        return try {
            val response = reader.city(InetAddress.getByName(ip))
            response.city.name
        } catch (e: AddressNotFoundException) {
            "Unknown city"
        } catch (e: Exception) {
            plugin.logger.severe("Failed to get city for IP $ip: ${e.message} [Exception]")
            "Unknown city"
        } catch (e: UnknownHostException) {
            plugin.logger.severe("Failed to get city for IP $ip: ${e.message} [UnknownHostException]")
            "Unknown city"
        }
    }
}
