package dev.joetul.tao.model

data class TimerData(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0
) {
    fun toTotalSeconds(): Long {
        return hours * 3600L + minutes * 60L + seconds
    }

    companion object
}