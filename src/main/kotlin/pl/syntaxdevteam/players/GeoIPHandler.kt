package pl.syntaxdevteam.players

import com.maxmind.geoip2.DatabaseReader
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import pl.syntaxdevteam.PunisherX
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.util.zip.GZIPInputStream

class GeoIPHandler(plugin: PunisherX, pluginFolder: String, private val licenseKey: String?) {

    private val cityDatabaseFile = File(pluginFolder, "GeoLite2-City.mmdb")

    init {
        if (licenseKey == null) {
            plugin.logger.err("License key not found. GeoIP functionality will be disabled.")
        } else {
            val folder = File(pluginFolder)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            downloadAndExtractDatabase()
        }
    }

    private fun downloadAndExtractDatabase() {
        if (licenseKey == null) return

        val cityUri = URI("https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz")

        val connection = cityUri.toURL().openConnection() as HttpURLConnection
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
    }

    fun getCountry(ip: String): String? {
        if (!cityDatabaseFile.exists()) return null
        DatabaseReader.Builder(cityDatabaseFile).build().use { reader ->
            val response = reader.city(InetAddress.getByName(ip))
            return response.country.name
        }
    }

    fun getCity(ip: String): String? {
        if (!cityDatabaseFile.exists()) return null
        DatabaseReader.Builder(cityDatabaseFile).build().use { reader ->
            val response = reader.city(InetAddress.getByName(ip))
            return response.city.name
        }
    }
}
