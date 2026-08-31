package com.example.dopadopa

import android.app.Application
import com.example.dopadopa.tracker.ScreenOffTracker
import com.example.dopadopa.tracker.ScreenTrackingService

/**
 * アプリのエントリーポイント（プロセス起動時に一度だけ生成される）。
 * ここで画面ロック計測（[ScreenTrackingService]）を起動しておくことで、
 * DopaDopa を開いていない間も画面オン/オフを取りこぼさずに計測できる。
 */
class DopaDopaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenOffTracker.init(this)
        ScreenTrackingService.start(this)
    }
}
