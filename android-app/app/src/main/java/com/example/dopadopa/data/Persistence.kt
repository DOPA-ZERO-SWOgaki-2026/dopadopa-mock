package com.example.dopadopa.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import org.json.JSONObject

/** Web 版 (`script.js`) の `localStorage` 相当のキー構成を SharedPreferences に移植したもの。 */
private object StorageKey {
    const val PREFS_NAME = "dopadopa_prefs"
    const val USER = "dopadopa-user"
    const val STATE = "dopadopa-state"
    const val ACCOUNTS = "dopadopa-accounts"
    const val DEVICE_ID = "dopadopa-device-id"

    /**
     * フォアグラウンドを離れた時刻（epoch millis）。アプリが完全に終了（タスクキルされる等）
     * されても次回起動時に未確定分を遡って計上できるよう、メモリではなく永続化しておく。
     */
    const val PENDING_BACKGROUND_START = "dopadopa.pendingBackgroundStart"
}

/**
 * アプリ全体の永続化まわり（iOS 版 Persistence.swift 相当）。
 * SharedPreferences をストレージとして使い、Web 版と同じキー構成・意味づけを保つ。
 */
class Persistence(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(StorageKey.PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): PersistedState {
        val raw = prefs.getString(StorageKey.STATE, null) ?: return PersistedState.initial()
        return try {
            PersistedState.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            PersistedState.initial()
        }
    }

    fun saveState(state: PersistedState) {
        prefs.edit().putString(StorageKey.STATE, state.toJson().toString()).apply()
    }

    fun currentUser(): String? = prefs.getString(StorageKey.USER, null)

    fun setCurrentUser(username: String?) {
        prefs.edit().apply {
            if (!username.isNullOrEmpty()) {
                putString(StorageKey.USER, username)
            } else {
                remove(StorageKey.USER)
            }
        }.apply()
    }

    fun pendingBackgroundStart(): Long? {
        val value = prefs.getLong(StorageKey.PENDING_BACKGROUND_START, -1L)
        return if (value > 0) value else null
    }

    fun setPendingBackgroundStart(millis: Long?) {
        prefs.edit().apply {
            if (millis != null) {
                putLong(StorageKey.PENDING_BACKGROUND_START, millis)
            } else {
                remove(StorageKey.PENDING_BACKGROUND_START)
            }
        }.apply()
    }

    fun deviceId(): String {
        prefs.getString(StorageKey.DEVICE_ID, null)?.let { return it }
        val generated = "device-${UUID.randomUUID()}"
        prefs.edit().putString(StorageKey.DEVICE_ID, generated).apply()
        return generated
    }

    fun accountProfiles(): MutableMap<String, AccountProfile> {
        val raw = prefs.getString(StorageKey.ACCOUNTS, null) ?: return mutableMapOf()
        return try {
            val json = JSONObject(raw)
            val result = mutableMapOf<String, AccountProfile>()
            json.keys().forEach { username ->
                result[username] = AccountProfile.fromJson(json.getJSONObject(username))
            }
            result
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    fun saveAccountProfiles(profiles: Map<String, AccountProfile>) {
        val json = JSONObject()
        profiles.forEach { (username, profile) -> json.put(username, profile.toJson()) }
        prefs.edit().putString(StorageKey.ACCOUNTS, json.toString()).apply()
    }

    fun ensureAccountProfile(username: String): AccountProfile {
        val profiles = accountProfiles()
        profiles[username]?.let { return it }
        val fresh = AccountProfile()
        profiles[username] = fresh
        saveAccountProfiles(profiles)
        return fresh
    }

    fun syncCurrentDeviceMetrics(
        username: String,
        screenTimeSeconds: Double,
        digitalDetoxSeconds: Double,
        otherAppSeconds: Double,
        weeklyPoints: Int,
    ) {
        val profiles = accountProfiles()
        val profile = profiles[username] ?: AccountProfile()
        profile.devices[deviceId()] = DeviceMetrics(
            screenTimeSeconds = maxOf(0.0, screenTimeSeconds),
            digitalDetoxSeconds = maxOf(0.0, digitalDetoxSeconds),
            otherAppSeconds = maxOf(0.0, otherAppSeconds),
            weeklyPoints = maxOf(0, weeklyPoints),
        )
        profiles[username] = profile
        saveAccountProfiles(profiles)
    }
}
