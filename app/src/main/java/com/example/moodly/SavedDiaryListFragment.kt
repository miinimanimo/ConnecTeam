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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SavedDiaryListFragment : Fragment(R.layout.fragment_saved_diary) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // 다이어리 작성 위치, 제목과 내용을 TextView에 표시하기
        val diaryLatitude = view.findViewById<TextView>(R.id.diaryLatitude)
        val diaryLongitude = view.findViewById<TextView>(R.id.diaryLongitude)
        val diaryTime = view.findViewById<TextView>(R.id.diaryTime)
        val diaryTitle = view.findViewById<TextView>(R.id.diaryTitle)
        val diaryText = view.findViewById<TextView>(R.id.diaryText)
        val diaryEmotion = view.findViewById<TextView>(R.id.diaryEmotion)


        // 다이어리 위도, 경도 가져오기 (기본값 설정)
        val savedDiaryLatitude = arguments?.getString("diaryLatitude") ?: "저장된 위도가 없습니다."
        diaryLatitude.text = savedDiaryLatitude
        val savedDiaryLongitude = arguments?.getString("diaryLongitude") ?: "저장된 경도가 없습니다."
        diaryLongitude.text = savedDiaryLongitude

        val timestamp = arguments?.getLong("timestamp") ?: 0L
        val date = Date(timestamp)  // 현재 시간을 Date로 변환

        // 날짜와 시간, 분 포맷
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(date)
        diaryTime.text = formattedDate



        // 다이어리 제목 가져오기 (기본값 설정)
        val savedDiaryTitle = arguments?.getString("diaryTitle") ?: "저장된 제목이 없습니다."
        diaryTitle.text = savedDiaryTitle

        // 다이어리 감정 가져오기
        val savedDiaryEmotion = arguments?.getString("diaryEmotion") ?: "저장된 감정이 없습니다."
        diaryEmotion.text = savedDiaryEmotion

        //다이어리 emotion ID 가져오기
        val savedDiaryEmotionID = arguments?.getInt("diaryEmotionID") ?: 0

        // 다이어리 내용 가져오기 (기본값 설정)
        val savedDiaryContent = arguments?.getString("diaryContent") ?: "저장된 내용이 없습니다."
        diaryText.text = savedDiaryContent

        // Bitmap을 전달받고 ImageView에 설정하기
        val diaryImageView = view.findViewById<ImageView>(R.id.diaryImage)

        val savedDiaryImage = arguments?.getParcelable<Bitmap>("diaryImage")
        if (savedDiaryImage != null) {
            diaryImageView.setImageBitmap(savedDiaryImage)  // Bitmap을 ImageView에 설정
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

        val menuButton: ImageButton = view.findViewById(R.id.menuButton)
        menuButton.setOnClickListener {
            // PopupMenu 객체 생성
            val popupMenu = PopupMenu(requireContext(), menuButton)

            // 메뉴 리소스 추가 (Fragment에서는 requireActivity().menuInflater 사용)
            requireActivity().menuInflater.inflate(R.menu.popup_menu_saved_diary, popupMenu.menu)

            // 메뉴 아이템 클릭 리스너 설정
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_diary -> {
                        if (parentFragmentManager.backStackEntryCount > 0) {
                            parentFragmentManager.popBackStack()
                        } else {
                            requireActivity().finish() // 필요 시 액티비티 종료
                        }
                        // 메뉴 아이템 1 클릭 시 동작
                        Toast.makeText(requireContext(), "Editing diary...", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }

            // 메뉴 표시
            popupMenu.show()
        }

    }
}