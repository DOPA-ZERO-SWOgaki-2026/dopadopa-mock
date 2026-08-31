package com.example.dopadopa.state

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.dopadopa.data.Persistence
import com.example.dopadopa.data.PersistedState
import com.example.dopadopa.tracker.ScreenOffTracker
import com.example.dopadopa.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * アプリ全体の状態を保持するビューモデル。iOS 版 AppState.swift / script.js の
 * state / tick() / updateUI() に相当する。
 *
 * 「デジタルデトックスタイム」は [ScreenOffTracker]（画面ロック検出）を唯一の情報源とする。
 * ポイントが貯まるのは、実際に **スマホの画面が消えている（ロックされている）時間** に対してのみ。
 * 「スクリーンタイム」は画面がついていたトータルの時間（このアプリを見ていた時間 +
 * 他のアプリを使っていたと推定される時間）で、デジタルデトックスタイムの対になる値。
 */
class AppState(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {

    data class UiState(
        val username: String? = null,
        val goalSeconds: Double = PersistedState.DAILY_GOAL_SECONDS,
        val weeklyPoints: Int = 0,
        val weeklyStartDate: String = PersistedState.currentWeekKey(),
        val screenTimeSeconds: Double = 0.0,
        val otherAppSeconds: Double = 0.0,
        val isForeground: Boolean = true,
        val digitalDetoxSeconds: Double = 0.0,
    ) {
        val totalScreenOnSeconds: Double get() = screenTimeSeconds + otherAppSeconds
        val totalPoints: Int get() = pointsFor(digitalDetoxSeconds)
        val goalRatio: Double get() = (digitalDetoxSeconds / maxOf(goalSeconds, 1.0)).coerceIn(0.0, 1.0)
        val remainingSeconds: Double get() = maxOf(goalSeconds - digitalDetoxSeconds, 0.0)
        val isGoalAchieved: Boolean get() = remainingSeconds <= 0.0
        val goalMinutes: Int get() = (goalSeconds / 60).toInt()

        /** 内訳グラフ用の3分類（このアプリ／画面オフ／他のアプリ推定）。 */
        val usageBreakdown: List<UsageSlice>
            get() = listOf(
                UsageSlice("DopaDopa", screenTimeSeconds, Theme.primary),
                UsageSlice("画面オフ（デトックス）", digitalDetoxSeconds, Theme.accent),
                UsageSlice("他のアプリ（推定）", otherAppSeconds, Theme.subtleInk),
            )
    }

    private val persistence = Persistence(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastPointsSnapshot: Int

    init {
        val stored = persistence.loadState()
        val initialDetox = ScreenOffTracker.state.value.combinedSeconds()
        _uiState.value = UiState(
            username = persistence.currentUser(),
            goalSeconds = stored.goalSeconds,
            weeklyPoints = stored.weeklyPoints,
            weeklyStartDate = stored.weeklyStartDate,
            screenTimeSeconds = stored.screenTimeSeconds,
            otherAppSeconds = stored.otherAppSeconds,
            isForeground = true,
            digitalDetoxSeconds = initialDetox,
        )
        lastPointsSnapshot = pointsFor(initialDetox)
        resetWeeklyPointsIfNeeded()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // 前回フォアグラウンドを離れたまま（バックグラウンドで一時停止、またはタスクキル）
        // だった場合、起動直後にその間の未確定分を計上する。
        reconcileBackgroundGap()

        viewModelScope.launch {
            ScreenOffTracker.state.collect { trackerState ->
                handleDetoxChange(trackerState.combinedSeconds())
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        super.onCleared()
    }

    // MARK: - Process lifecycle (script.js の visibilitychange / iOS の scenePhase 相当)

    override fun onStart(owner: LifecycleOwner) = handleForegroundChange(isForeground = true)
    override fun onStop(owner: LifecycleOwner) = handleForegroundChange(isForeground = false)

    private fun handleForegroundChange(isForeground: Boolean) {
        val wasForeground = _uiState.value.isForeground
        _uiState.update { it.copy(isForeground = isForeground) }

        if (wasForeground && !isForeground) {
            // フォアグラウンドを離れた瞬間を永続化しておく。アプリが完全終了させられても
            // 次回起動時に読み直せるように、メモリ上の変数ではなく永続化した値を使う。
            persistence.setPendingBackgroundStart(System.currentTimeMillis())
        } else if (!wasForeground && isForeground) {
            reconcileBackgroundGap()
        }
    }

    /**
     * バックグラウンドにいた間の経過時間のうち、画面ロック（ScreenOffTracker が計測済み）と
     * 重ならない残りを「他のアプリを使っていた時間」として概算計上する。
     */
    private fun reconcileBackgroundGap() {
        val start = persistence.pendingBackgroundStart() ?: return
        persistence.setPendingBackgroundStart(null)

        val now = System.currentTimeMillis()
        val elapsed = (now - start) / 1000.0
        if (elapsed <= 0) return

        val lockedDuringGap = ScreenOffTracker.state.value.events
            .filter { it.offAtMillis >= start && it.onAtMillis <= now }
            .sumOf { it.durationSeconds }

        val otherSeconds = maxOf(0.0, elapsed - lockedDuringGap)
        if (otherSeconds > 0) {
            _uiState.update { it.copy(otherAppSeconds = it.otherAppSeconds + otherSeconds) }
            persist()
        }
    }

    // MARK: - Ticking (script.js の setInterval(tick, 1000) 相当)

    private fun tick() {
        _uiState.update { current ->
            if (current.isForeground) current.copy(screenTimeSeconds = current.screenTimeSeconds + 1) else current
        }
        resetWeeklyPointsIfNeeded()
        persist()
    }

    /** ScreenOffTracker の累積値が変化した（＝画面ロックが解除された）タイミングで反映する。 */
    private fun handleDetoxChange(combinedSeconds: Double) {
        val newPoints = pointsFor(combinedSeconds)
        val gained = newPoints - lastPointsSnapshot
        _uiState.update { it.copy(digitalDetoxSeconds = combinedSeconds) }
        if (gained > 0) {
            resetWeeklyPointsIfNeeded()
            _uiState.update { it.copy(weeklyPoints = it.weeklyPoints + gained) }
        }
        lastPointsSnapshot = newPoints
        persist()
    }

    private fun resetWeeklyPointsIfNeeded() {
        val currentWeek = PersistedState.currentWeekKey()
        if (_uiState.value.weeklyStartDate != currentWeek) {
            _uiState.update { it.copy(weeklyStartDate = currentWeek, weeklyPoints = 0) }
        }
    }

    // MARK: - Account

    fun login(usernameRaw: String) {
        val trimmed = usernameRaw.trim()
        if (trimmed.isEmpty()) return
        persistence.ensureAccountProfile(trimmed)
        persistence.setCurrentUser(trimmed)
        _uiState.update { it.copy(username = trimmed) }
        persist()
    }

    fun logout() {
        persist()
        persistence.setCurrentUser(null)
        _uiState.update { it.copy(username = null) }
    }

    // MARK: - Goal

    fun updateGoalMinutes(minutes: Int) {
        if (minutes <= 0) return
        _uiState.update { it.copy(goalSeconds = minutes * 60.0) }
        persist()
    }

    // MARK: - Persistence

    private fun persist() {
        val current = _uiState.value
        persistence.saveState(
            PersistedState(
                goalSeconds = current.goalSeconds,
                weeklyPoints = current.weeklyPoints,
                weeklyStartDate = current.weeklyStartDate,
                screenTimeSeconds = current.screenTimeSeconds,
                otherAppSeconds = current.otherAppSeconds,
            ),
        )
        current.username?.let { username ->
            persistence.syncCurrentDeviceMetrics(
                username = username,
                screenTimeSeconds = current.screenTimeSeconds,
                digitalDetoxSeconds = current.digitalDetoxSeconds,
                otherAppSeconds = current.otherAppSeconds,
                weeklyPoints = current.weeklyPoints,
            )
        }
    }

    companion object {
        fun pointsFor(seconds: Double): Int = floor(maxOf(0.0, seconds) / 60).toInt()
    }
}

/** 内訳グラフの1スライス分。 */
data class UsageSlice(
    val label: String,
    val seconds: Double,
    val color: Color,
)
