package com.example.moodly

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeService : Service(), SensorEventListener {

    private var lastShakeTime: Long = 0
    private var shakeCount = 0
    private val shakeThreshold = 7.0f // 흔들림 감도 (m/s²)
    private val timeThreshold = 500   // 두 흔들림 간 최소 간격 (ms)
    private val shakeCountThreshold = 3  // 최소 흔들림 횟수

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    override fun onCreate() {
        super.onCreate()

        // Foreground 서비스 시작
        startForeground(1, createNotification())

        // 센서 관리자 초기화
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        // 센서 이벤트 리스너 등록
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)

        Log.d("ShakeService", "Shake Service started and listening for shakes.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 가속도 계산 (중력 필터링)
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        // 가속도의 변화량을 계산
        val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        val currentTime = System.currentTimeMillis()

        // 흔들림 감지
        if (acceleration > shakeThreshold && (currentTime - lastShakeTime > timeThreshold)) {
            lastShakeTime = currentTime
            shakeCount++

            Log.d("Shake", "Device shaken! Shake count: $shakeCount")

            if (shakeCount >= shakeCountThreshold) {
                Log.d("Shake", "Shake count threshold reached! Triggering onShake callback.")
                onShake()

                shakeCount = 0  // 초기화
            }
        }
    }

    private fun onShake() {
        // 흔들림 감지 후 처리할 로직
        Log.d("ShakeService", "Shake detected!")
        // Broadcast를 통해 흔들림 감지 사실을 HomeFragment에 알림
        val intent = Intent("com.example.moodly.SHAKE_DETECTED")
        sendBroadcast(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변화 시 처리할 로직
    }

    override fun onDestroy() {
        super.onDestroy()

        // 센서 이벤트 리스너 해제
        sensorManager.unregisterListener(this)

        Log.d("ShakeService", "Shake Service stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val channelId = "shake_service_channel"
        val channelName = "Shake Service Channel"

        // Notification 채널 생성 (Android 8.0 이상 필요)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // 알림 생성
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shake Service")
            .setContentText("Listening for shakes…")
            .build()
    }
}