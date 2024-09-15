package pl.syntaxdevteam.players

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import pl.syntaxdevteam.PunisherX
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

        val cityUri = URI("https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz")

        val connection = cityUri.toURL().openConnection() as HttpURLConnection
        try {
            connection.inputStream.use { input ->
                GZIPInputStream(input).use { gzip ->
                    TarArchiveInputStream(gzip).use { tar ->
                        var entry = tar.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith(".mmdb")) {
                                FileOutputStream(cityDatabaseFile).use { output ->
                                    tar.copyTo(output)
                                }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        } catch (e: IOException) {
            if (connection.responseCode == 401) {
                plugin.logger.severe("Unauthorized access. Please check your license key.")
            } else {
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
