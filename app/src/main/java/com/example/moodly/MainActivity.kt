package com.example.moodly

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moodly.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationListener


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 뷰 바인딩을 통해 bottomNavigationView 참조
        binding.bottomNavigationView.setupWithNavController(navController)

        // NavController에 addOnDestinationChangedListener를 추가하여 프래그먼트 이동 시 동작 처리
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.expressFragment, R.id.savedDiaryFragment, R.id.diaryFragment -> {
                    // 숨겨야 할 프래그먼트
                    binding.bottomNavigationView.visibility = View.GONE
                }
                else -> {
                    // 그 외의 프래그먼트에서는 네비게이션 바를 표시
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
        // 알람 설정
        setAlarm()
        // 알림 권한 요청
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
    }
    // 알람을 설정하는 메소드
    private fun setAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val triggerTime = System.currentTimeMillis() + 60 * 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAlarm()
                Log.d("Alarm", "Alarm is set")
            } else {
                Log.d("AlarmPermission", "알림 권한이 거부되었습니다.")
            }
        }
    }
}
