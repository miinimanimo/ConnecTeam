package com.example.moodly

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var diaryCountText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        diaryCountText = view.findViewById(R.id.diaryCountText)

        loadDataAndVisualize()
    }

    private fun loadDataAndVisualize() {
        lifecycleScope.launch {
            val db = DiaryDatabase.getDatabase(requireContext())
            val diaryDao = db.diaryDao()

            // 다이어리 총 개수 가져오기
            val totalDiaries = diaryDao.getDiaryCount()
            diaryCountText.text = "Total Diaries: $totalDiaries"

            // 감정별 카운트 가져오기
            val emotionCounts = diaryDao.getEmotionCounts()

            if (emotionCounts.isNotEmpty()) {
                val pieEntries = emotionCounts.map {
                    PieEntry(it.count.toFloat(), it.emotion)
                }

                // 데이터셋 생성
                val pieDataSet = PieDataSet(pieEntries, "Emotion Distribution")
                pieDataSet.colors = listOf(
                    Color.rgb(244, 67, 54), // Red
                    Color.rgb(33, 150, 243), // Blue
                    Color.rgb(76, 175, 80), // Green
                    Color.rgb(255, 193, 7), // Yellow
                    Color.rgb(156, 39, 176), // Purple
                    Color.rgb(121, 85, 72) // Brown
                )
                pieDataSet.sliceSpace = 2f // 조각 간 간격
                pieDataSet.valueTextSize = 12f // 값 텍스트 크기

                // PieData 생성 및 설정
                val pieData = PieData(pieDataSet)
                pieChart.data = pieData
                pieChart.description.isEnabled = false // 설명 제거
                pieChart.isDrawHoleEnabled = true // 가운데 홀
                pieChart.setHoleColor(Color.WHITE)
                pieChart.setTransparentCircleColor(Color.LTGRAY)
                pieChart.animateY(1000) // 애니메이션 효과
                pieChart.invalidate() // 새로고침
            }
        }
    }
}
