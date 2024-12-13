package com.example.moodly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryListFragment : Fragment(R.layout.fragment_diary_list) {

    private lateinit var diaryListView: ListView
    private lateinit var diaryAdapter: ArrayAdapter<String>
    private lateinit var diaries: List<Diary>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_diary_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diaryListView = view.findViewById(R.id.diaryListView)
        diaryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        diaryListView.adapter = diaryAdapter

        loadDiaries()

        diaryListView.setOnItemClickListener { parent, view, position, id ->
            val selectedDiary = diaries[position]
            val bundle = Bundle().apply {
                putString("diaryTitle", selectedDiary.title)
                putString("diaryEmotion", selectedDiary.emotion)
                putString("diaryContent", selectedDiary.content)
                putDouble("latitude", selectedDiary.latitude)
                putDouble("longitude", selectedDiary.longitude)
                putLong("timestamp", selectedDiary.timestamp)
                putString("diaryImage", selectedDiary.image) // Base64 이미지 문자열
                putInt("diaryEmotionID", selectedDiary.emotion_id) // 감정 ID
            }
            findNavController().navigate(R.id.action_diaryListFragment_to_savedDiaryListFragment, bundle)
        }
    }

    private fun loadDiaries() {
        lifecycleScope.launch {
            val db = DiaryDatabase.getDatabase(requireContext())
            diaries = db.diaryDao().getAllDiaries()

            // 날짜 포맷 설정
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 다이어리 제목과 함께 날짜를 포맷팅
            val diaryTitles = diaries.map {
                val formattedDate = dateFormat.format(Date(it.timestamp.toLong())) // timestamp를 Date로 변환
                "${it.title} - ${it.emotion} - $formattedDate" // 포맷팅된 날짜 사용
            }

            diaryAdapter.clear()
            diaryAdapter.addAll(diaryTitles)
            diaryAdapter.notifyDataSetChanged()
        }
    }
}
