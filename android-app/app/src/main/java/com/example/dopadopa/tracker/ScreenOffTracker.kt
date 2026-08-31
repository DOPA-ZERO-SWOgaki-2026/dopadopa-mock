package com.example.dopadopa.tracker

import android.content.Context
import android.content.SharedPreferences
import com.example.dopadopa.data.ScreenOffEvent
import com.example.dopadopa.data.toJsonArray
import com.example.dopadopa.data.toScreenOffEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * 画面オフ（デバイスのロック）時間を計測し、累積時間を SharedPreferences に保存するシングルトン。
 *
 * SW_ogaki / iOS 版 ScreenOffTracker.swift 相当。ただし Android では
 * [Intent.ACTION_SCREEN_OFF] / [Intent.ACTION_SCREEN_ON] という「画面が消灯／点灯した」ことを
 * 直接教えてくれる公開ブロードキャストがあるため、iOS 版のような近似（アプリロック検出での代替）
 * ではなく、実際の画面点消灯そのものを検出できる。[ScreenTrackingService] がアプリの
 * ライフサイクルに関わらず常駐してこのブロードキャストを受け取り続けることで、
 * 「アプリを開いていない間の画面オフ時間」も取りこぼさず計測する。
 */
object ScreenOffTracker {

    data class TrackerState(
        val totalScreenOffSeconds: Double = 0.0,
        val isScreenOff: Boolean = false,
        val offStartMillis: Long? = null,
        val events: List<ScreenOffEvent> = emptyList(),
    ) {
        fun currentOffSeconds(nowMillis: Long = System.currentTimeMillis()): Double =
            if (offStartMillis != null) maxOf(0.0, (nowMillis - offStartMillis) / 1000.0) else 0.0

        fun combinedSeconds(nowMillis: Long = System.currentTimeMillis()): Double =
            totalScreenOffSeconds + currentOffSeconds(nowMillis)
    }

    private object Keys {
        const val PREFS_NAME = "dopadopa_tracker"
        const val TOTAL_SECONDS = "screenOffTracker.totalSeconds"
        const val OFF_START_MILLIS = "screenOffTracker.offStartMillis"
        const val EVENTS = "screenOffTracker.events"
    }

    private const val MAX_STORED_EVENTS = 200

    private val _state = MutableStateFlow(TrackerState())
    val state: StateFlow<TrackerState> = _state.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val sharedPrefs = context.applicationContext
            .getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPrefs

        val total = sharedPrefs.getString(Keys.TOTAL_SECONDS, null)?.toDoubleOrNull() ?: 0.0
        val events = try {
            sharedPrefs.getString(Keys.EVENTS, null)
                ?.let { JSONArray(it).toScreenOffEvents() }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        var resolvedTotal = total
        // 前回、画面オフの記録中にプロセスが終了していた場合は、
        // ここでその分を累積時間に合算してから改めて計測を再開する。
        val savedOffStart = sharedPrefs.getLong(Keys.OFF_START_MILLIS, -1L)
        if (savedOffStart > 0) {
            resolvedTotal += (System.currentTimeMillis() - savedOffStart) / 1000.0
            sharedPrefs.edit().remove(Keys.OFF_START_MILLIS).apply()
            persistTotal(resolvedTotal)
        }

        _state.value = TrackerState(
            totalScreenOffSeconds = resolvedTotal,
            isScreenOff = false,
            offStartMillis = null,
            events = events,
        )
    }

    fun handleScreenOff() {
        val current = _state.value
        if (current.offStartMillis != null) return
        val now = System.currentTimeMillis()
        prefs?.edit()?.putLong(Keys.OFF_START_MILLIS, now)?.apply()
        _state.value = current.copy(isScreenOff = true, offStartMillis = now)
    }

    fun handleScreenOn() {
        val current = _state.value
        val start = current.offStartMillis ?: return
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - start) / 1000.0
        val newTotal = current.totalScreenOffSeconds + elapsedSeconds
        persistTotal(newTotal)

        val newEvents = (listOf(ScreenOffEvent(offAtMillis = start, onAtMillis = now)) + current.events)
            .take(MAX_STORED_EVENTS)
        persistEvents(newEvents)

        prefs?.edit()?.remove(Keys.OFF_START_MILLIS)?.apply()
        _state.value = current.copy(
            totalScreenOffSeconds = newTotal,
            isScreenOff = false,
            offStartMillis = null,
            events = newEvents,
        )
    }

    /** 累積計測時間・履歴をリセットする。 */
    fun reset() {
        persistTotal(0.0)
        persistEvents(emptyList())
        prefs?.edit()?.remove(Keys.OFF_START_MILLIS)?.apply()
        _state.value = TrackerState()
    }

    private fun persistTotal(total: Double) {
        prefs?.edit()?.putString(Keys.TOTAL_SECONDS, total.toString())?.apply()
    }

    private fun persistEvents(events: List<ScreenOffEvent>) {
        prefs?.edit()?.putString(Keys.EVENTS, events.toJsonArray().toString())?.apply()
    }
}
