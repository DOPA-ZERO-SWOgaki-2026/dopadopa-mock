package com.example.dopadopa.data

import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * state オブジェクト（画面オフ時間を除く、ゴール・週間ポイントなどの値）の永続化用モデル。
 * iOS 版 Persistence.swift の `PersistedState` に対応する。
 * 秒数はすべて Double 秒（iOS の TimeInterval と同じ単位）で保持する。
 */
data class PersistedState(
    val goalSeconds: Double,
    val weeklyPoints: Int,
    val weeklyStartDate: String,
    val screenTimeSeconds: Double,
    // このアプリを操作していた時間でも、画面がロックされていた時間でもない、
    // 「他のアプリを使っていたと推定される」時間。
    val otherAppSeconds: Double = 0.0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("goalSeconds", goalSeconds)
        put("weeklyPoints", weeklyPoints)
        put("weeklyStartDate", weeklyStartDate)
        put("screenTimeSeconds", screenTimeSeconds)
        put("otherAppSeconds", otherAppSeconds)
    }

    companion object {
        const val DAILY_GOAL_SECONDS = 180.0 * 60.0

        fun initial(): PersistedState = PersistedState(
            goalSeconds = DAILY_GOAL_SECONDS,
            weeklyPoints = 0,
            weeklyStartDate = currentWeekKey(),
            screenTimeSeconds = 0.0,
            otherAppSeconds = 0.0,
        )

        fun fromJson(json: JSONObject): PersistedState = PersistedState(
            goalSeconds = json.optDouble("goalSeconds", DAILY_GOAL_SECONDS),
            weeklyPoints = json.optInt("weeklyPoints", 0),
            weeklyStartDate = json.optString("weeklyStartDate", currentWeekKey()),
            screenTimeSeconds = json.optDouble("screenTimeSeconds", 0.0),
            otherAppSeconds = json.optDouble("otherAppSeconds", 0.0),
        )

        /** 月曜始まりの週キー（例: "2026-08-24"）。script.js の getWeekKey() と同じ考え方。 */
        fun currentWeekKey(atMillis: Long = System.currentTimeMillis()): String {
            val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = atMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Calendar.DAY_OF_WEEK: 1 = Sunday ... 7 = Saturday
            val weekday = calendar.get(Calendar.DAY_OF_WEEK)
            val daysSinceMonday = (weekday + 5) % 7
            calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            return "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
        }
    }
}

/** 1 台のデバイス上での累積値。Web 版のマルチデバイス同期構想を単純化したもの。 */
data class DeviceMetrics(
    val screenTimeSeconds: Double,
    val digitalDetoxSeconds: Double,
    val otherAppSeconds: Double,
    val weeklyPoints: Int,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("screenTimeSeconds", screenTimeSeconds)
        put("digitalDetoxSeconds", digitalDetoxSeconds)
        put("otherAppSeconds", otherAppSeconds)
        put("weeklyPoints", weeklyPoints)
    }

    companion object {
        fun fromJson(json: JSONObject): DeviceMetrics = DeviceMetrics(
            screenTimeSeconds = json.optDouble("screenTimeSeconds", 0.0),
            digitalDetoxSeconds = json.optDouble("digitalDetoxSeconds", 0.0),
            otherAppSeconds = json.optDouble("otherAppSeconds", 0.0),
            weeklyPoints = json.optInt("weeklyPoints", 0),
        )
    }
}

data class AccountProfile(
    val devices: MutableMap<String, DeviceMetrics> = mutableMapOf(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        val devicesJson = JSONObject()
        devices.forEach { (deviceId, metrics) -> devicesJson.put(deviceId, metrics.toJson()) }
        put("devices", devicesJson)
    }

    companion object {
        fun fromJson(json: JSONObject): AccountProfile {
            val devices = mutableMapOf<String, DeviceMetrics>()
            val devicesJson = json.optJSONObject("devices")
            devicesJson?.keys()?.forEach { deviceId ->
                devices[deviceId] = DeviceMetrics.fromJson(devicesJson.getJSONObject(deviceId))
            }
            return AccountProfile(devices)
        }
    }
}

/** 1 回分の「画面オフ→オン」の記録（消えた時刻・ついた時刻・その差分）。 */
data class ScreenOffEvent(
    val id: String = UUID.randomUUID().toString(),
    /** 画面が消えた（ロックされた）時刻（epoch millis） */
    val offAtMillis: Long,
    /** 画面がついた（ロック解除された）時刻（epoch millis） */
    val onAtMillis: Long,
) {
    /** offAt から onAt までの差分（秒） */
    val durationSeconds: Double get() = (onAtMillis - offAtMillis) / 1000.0

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("offAtMillis", offAtMillis)
        put("onAtMillis", onAtMillis)
    }

    companion object {
        fun fromJson(json: JSONObject): ScreenOffEvent = ScreenOffEvent(
            id = json.optString("id", UUID.randomUUID().toString()),
            offAtMillis = json.optLong("offAtMillis"),
            onAtMillis = json.optLong("onAtMillis"),
        )
    }
}

fun List<ScreenOffEvent>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { array.put(it.toJson()) }
    return array
}

fun JSONArray.toScreenOffEvents(): List<ScreenOffEvent> = buildList {
    for (i in 0 until length()) {
        add(ScreenOffEvent.fromJson(getJSONObject(i)))
    }
}
