package com.example.dopadopa.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 画面の点灯/消灯を知らせるシステムブロードキャストを受け取り [ScreenOffTracker] に伝える。
 * `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` は manifest では受信できない「暗黙的ブロードキャスト」
 * のため、常駐サービス（[ScreenTrackingService]）内で動的に registerReceiver する。
 */
class ScreenBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> ScreenOffTracker.handleScreenOff()
            Intent.ACTION_SCREEN_ON -> ScreenOffTracker.handleScreenOn()
        }
    }
}
