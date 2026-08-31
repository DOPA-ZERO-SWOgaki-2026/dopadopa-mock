package com.example.dopadopa.data

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object Formatters {
    /** 秒数を `00:00:00` 形式にフォーマットする（script.js の formatDuration と同じ）。 */
    fun duration(totalSeconds: Double): String {
        val seconds = max(0, totalSeconds.roundToInt())
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remaining = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, remaining)
    }

    /** ポイントを `1,234P` 形式にフォーマットする（script.js の formatPoints と同じ）。 */
    fun points(value: Int): String {
        val text = NumberFormat.getNumberInstance(Locale.US).format(value)
        return "${text}P"
    }

    fun hoursAndMinutes(totalSeconds: Double): String {
        val seconds = max(0, totalSeconds.roundToInt())
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }
}
