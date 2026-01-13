package id.project.df.dnote.core.common.util

fun Long.formatDate(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    val zone = java.time.ZoneId.systemDefault()
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, zone)

    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return localDateTime.format(formatter)
}
