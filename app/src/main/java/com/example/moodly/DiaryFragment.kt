package com.example.moodly

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.moodly.databinding.FragmentDiaryBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DiaryFragment : Fragment() {
    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.myVectorImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        arguments?.getInt("diaryId")?.let { diaryId ->
            loadDiaryDetails(diaryId)
        }
    }

    private fun loadDiaryDetails(diaryId: Int) {
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiaryDetails(diaryId)
            .enqueue(object : Callback<DiaryEntry> {
                override fun onResponse(call: Call<DiaryEntry>, response: Response<DiaryEntry>) {
                    if (response.isSuccessful) {
                        response.body()?.let { diary ->
                            updateUI(diary)
                        }
                    }
                }

                override fun onFailure(call: Call<DiaryEntry>, t: Throwable) {
                    Log.e("DiaryFragment", "API call failed", t)
                    view?.let {
                        Snackbar.make(it, "데이터를 불러오는데 실패했습니다", Snackbar.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateUI(diary: DiaryEntry) {
        binding.apply {
            diaryTitle.text = diary.title
            diaryTime.text = formatDateTime(diary.created_at)

            val emotion = when (diary.emotion_category) {
                1 -> "😊"  // Happy
                2 -> "😎"  // Excited
                3 -> "😐"  // Soso
                4 -> "😢"  // Sad
                5 -> "😡"  // Angry
                6 -> "😪"  // Tired
                else -> "😐"
            }
            diaryEmotion.text = emotion
            diaryText.text = diary.content

            if (!diary.image.isNullOrEmpty()) {
                diaryImage.visibility = View.VISIBLE

                Glide.with(requireContext())
                    .load(diary.image) // 수정된 Django에서 절대 URL 제공
                    .centerCrop()
                    .error(android.R.drawable.ic_dialog_alert)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("DiaryFragment", "Image load failed for URL: ${diary.image}")
                            Log.e("DiaryFragment", "Error details: ${e?.rootCauses?.joinToString("\n")}")

                            Snackbar.make(
                                binding.root,
                                "이미지를 불러오는데 실패했습니다",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("DiaryFragment", "Image loaded successfully from: ${diary.image}")
                            return false
                        }
                    })
                    .into(diaryImage)
            } else {
                diaryImage.visibility = View.GONE
            }
        }
    }

    private fun formatDateTime(dateTimeStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            outputFormat.timeZone = TimeZone.getDefault()
            val date = inputFormat.parse(dateTimeStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
