package pl.syntaxdevteam.punisher.players

import pl.syntaxdevteam.punisher.PunisherX
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import org.apache.tools.tar.TarEntry
import org.apache.tools.tar.TarInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.util.zip.GZIPInputStream

class GeoIPHandler(private val plugin: PunisherX, pluginFolder: String, private val licenseKey: String?) {

    private val cityDatabaseFile = File(pluginFolder, "GeoLite2-City.mmdb")

    init {
        if (licenseKey == null) {
            plugin.logger.warning("License key not found. GeoIP functionality will be disabled.")
        } else {
            val folder = File(pluginFolder)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            try {
                downloadAndExtractDatabase()
            } catch (e: IOException) {
                plugin.logger.severe("Failed to download GeoIP database: ${e.message}")
            }
        }
    }

    private fun downloadAndExtractDatabase() {
        if (licenseKey == null) return

        if (cityDatabaseFile.exists()) {
            plugin.logger.info("GeoIP database already exists. Skipping download.")
            return
        }

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
        if (!cityDatabaseFile.exists()) return "Unknown country"
        return try {
            DatabaseReader.Builder(cityDatabaseFile).build().use { reader ->
                val response = reader.city(InetAddress.getByName(ip))
                response.country.name
            }
        } catch (e: AddressNotFoundException) {
            "Unknown country"
        }
    }

    fun getCity(ip: String): String? {
        if (!cityDatabaseFile.exists()) return "Unknown city"
        return try {
            DatabaseReader.Builder(cityDatabaseFile).build().use { reader ->
                val response = reader.city(InetAddress.getByName(ip))
                response.city.name
            }
        } catch (e: AddressNotFoundException) {
            "Unknown city"
        }
    }
}
