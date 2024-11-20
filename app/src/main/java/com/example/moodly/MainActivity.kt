package com.example.moodly

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moodly.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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
                R.id.expressFragment, R.id.savedDiaryFragment -> {
                    // 숨겨야 할 프래그먼트
                    binding.bottomNavigationView.visibility = View.GONE
                }
                else -> {
                    // 그 외의 프래그먼트에서는 네비게이션 바를 표시
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
    }
}
