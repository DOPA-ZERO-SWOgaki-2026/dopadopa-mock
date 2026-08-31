package com.example.dopadopa.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dopadopa.MainActivity
import com.example.dopadopa.R

/**
 * アプリがフォアグラウンドでない間も `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` を
 * 受け取り続けるための常駐サービス。iOS 版 README にある
 * 「Android の ACTION_SCREEN_OFF のような画面が消灯したことを直接教えてくれる公開 API」を
 * そのまま使い、デバイス全体の画面ロック時間を計測する。
 */
class ScreenTrackingService : Service() {

    private val receiver = ScreenBroadcastReceiver()
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        ScreenOffTracker.init(applicationContext)
        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(receiver, filter)
        isReceiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // プロセスが再起動されても計測を続けられるよう START_STICKY で自動再起動させる。
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = ensureChannel()
        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("DopaDopa")
            .setContentText("スマホの画面ロック時間を計測中です")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "デジタルデトックス計測",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "画面ロック時間を計測するための常駐通知です"
            }
            manager.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }

    companion object {
        private const val CHANNEL_ID = "dopadopa_screen_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ScreenTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
