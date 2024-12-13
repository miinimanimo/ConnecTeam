package com.example.moodly

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Base64
import java.io.ByteArrayOutputStream

class SavedDiaryFragment : Fragment(R.layout.fragment_saved_diary) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 요소 초기화
        val diaryLatitude = view.findViewById<TextView>(R.id.diaryLatitude)
        val diaryLongitude = view.findViewById<TextView>(R.id.diaryLongitude)
        val diaryTime = view.findViewById<TextView>(R.id.diaryTime)
        val diaryTitle = view.findViewById<TextView>(R.id.diaryTitle)
        val diaryText = view.findViewById<TextView>(R.id.diaryText)
        val diaryEmotion = view.findViewById<TextView>(R.id.diaryEmotion)

        // Argument에서 데이터 가져오기
        val savedDiaryLatitude = arguments?.getDouble("diaryLatitude") ?: "0.0"
        val lat= savedDiaryLatitude.toString()
        diaryLatitude.text = savedDiaryLatitude.toString()
        val savedDiaryLongitude = arguments?.getDouble("diaryLongitude") ?: "0.0"
        val lon= savedDiaryLongitude.toString()
        diaryLongitude.text = savedDiaryLongitude.toString()

        val timestamp = arguments?.getLong("timestamp") ?: 0L
        val date = Date(timestamp)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        diaryTime.text = dateFormat.format(date)

        val savedDiaryTitle = arguments?.getString("diaryTitle") ?: "제목 없음"
        diaryTitle.text = savedDiaryTitle

        val savedDiaryEmotion = arguments?.getString("diaryEmotion") ?: "감정 없음"
        diaryEmotion.text = savedDiaryEmotion

        // 다이어리 emotion ID 가져오기
        val savedDiaryEmotionID = arguments?.getInt("diaryEmotionID") ?: 0

        // 다이어리 내용 가져오기
        val savedDiaryContent = arguments?.getString("diaryContent") ?: "내용 없음"
        diaryText.text = savedDiaryContent

        // Bitmap을 Base64로 변환하여 저장
        val diaryImageView = view.findViewById<ImageView>(R.id.diaryImage)
        val savedDiaryImage: Bitmap? = arguments?.getParcelable("diaryImage")
        if (savedDiaryImage != null) {
            diaryImageView.setImageBitmap(savedDiaryImage)
        }

        // 데이터베이스에 저장할 다이어리 객체 생성
        val diary = Diary(
            title = savedDiaryTitle,
            emotion = savedDiaryEmotion,
            emotion_id = savedDiaryEmotionID, // 감정 ID 추가
            content = savedDiaryContent,
            latitude = lat.toDoubleOrNull() ?: 0.0,
            longitude = lon.toDoubleOrNull() ?: 0.0,
            timestamp = timestamp,
            image = savedDiaryImage?.let { bitmapToBase64(it) } ?: ""
        )

        // 데이터베이스에 저장
        val database = DiaryDatabase.getDatabase(requireContext())
        val diaryDao = database.diaryDao()

        lifecycleScope.launch {
            try {
                diaryDao.insert(diary)
                Toast.makeText(requireContext(), "Diary is stored.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error, Can't store the diary: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 뒤로가기 버튼 처리
        val backButton: ImageButton = view.findViewById(R.id.myVectorImageView)
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_savedDiaryFragment_to_homeFragment)
        }

        // 물리적 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_savedDiaryFragment_to_homeFragment)
        }

        // 메뉴 버튼 처리
        val menuButton: ImageButton = view.findViewById(R.id.menuButton)
        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), menuButton)
            requireActivity().menuInflater.inflate(R.menu.popup_menu_saved_diary, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_diary -> {
                        // 편집 기능 구현
                        Toast.makeText(requireContext(), "Editing diary...", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    // Bitmap을 Base64 문자열로 변환하는 함수
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
