package pl.syntaxdevteam.punisher.loader

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        return compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    companion object {
        private val VERSION_PATTERN = Regex("\\d+(?:\\.\\d+){0,2}")

        fun parse(raw: String): SemanticVersion {
            val clean = VERSION_PATTERN.find(raw)?.value ?: "0.0.0"
            val parts = clean.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return SemanticVersion(major, minor, patch)
        }
    }
}

fun SemanticVersion.isBetween(start: SemanticVersion, endInclusive: SemanticVersion?): Boolean {
    if (this < start) return false
    if (endInclusive == null) return true
    return this <= endInclusive
}