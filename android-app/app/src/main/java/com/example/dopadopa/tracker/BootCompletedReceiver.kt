package com.example.dopadopa.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 端末再起動後も画面ロック計測サービスを自動的に再開させる。 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScreenTrackingService.start(context)
        }
    }
}
