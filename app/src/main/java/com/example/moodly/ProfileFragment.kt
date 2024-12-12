package com.example.moodly

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.moodly.databinding.DialogImageDetailBinding
import com.example.moodly.databinding.FragmentProfileBinding
import com.example.moodly.databinding.ItemImageBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "ProfileFragment"
private const val BASE_URL = "http://211.188.50.51:8000"

fun String.toFullImageUrl(): String {
    if (this.startsWith("http")) return this
    return "$BASE_URL$this"
}

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ImageAdapter
    private lateinit var mainApiService: MainApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainApiService = RetrofitClient.getMainApiService(requireContext())
        setupRecyclerView()
        fetchImages()
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter(
            onItemClick = { diary ->
                showImageDetail(diary)
            }
        )
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@ProfileFragment.adapter
        }
    }

    private fun fetchImages() {
        mainApiService.getFeed().enqueue(object : Callback<DiaryResponse> {
            override fun onResponse(
                call: Call<DiaryResponse>,
                response: Response<DiaryResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { diaryResponse ->
                        Log.d(TAG, "받은 일기 개수: ${diaryResponse.diaries.size}")
                        adapter.submitList(diaryResponse.diaries)
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "이미지 가져오기 실패: $error")
                    Toast.makeText(context, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DiaryResponse>, t: Throwable) {
                Log.e(TAG, "네트워크 에러", t)
                Toast.makeText(context, "네트워크 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showImageDetail(diary: DiaryEntry) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImageDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        with(dialogBinding) {
            backButton.setOnClickListener {
                dialog.dismiss()
            }

            diary.image?.let { imageUrl ->
                val fullImageUrl = imageUrl.toFullImageUrl()
                Glide.with(requireContext())
                    .load(fullImageUrl)
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
                            Log.e(TAG, "Image load failed for URL: $fullImageUrl")
                            Log.e(TAG, "Error details: ${e?.rootCauses?.joinToString("\n")}")
                            Snackbar.make(
                                root,
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
                            Log.d(TAG, "Image loaded successfully from: $fullImageUrl")
                            return false
                        }
                    })
                    .into(imageView)
            }

            titleText.text = diary.title
            emotionText.text = getEmotionName(diary.emotion_category)

            checkLikeStatus(diary.id, likeButton)
            updateLikeCount(diary.id, likeCountText)

            likeButton.setOnClickListener {
                toggleLike(diary.id, likeButton, likeCountText)
            }
        }

        dialog.show()
    }

    private fun checkLikeStatus(diaryId: Int, likeButton: ImageButton) {
        mainApiService.getLikeStatus(diaryId).enqueue(object : Callback<LikeStatusResponse> {
            override fun onResponse(
                call: Call<LikeStatusResponse>,
                response: Response<LikeStatusResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { status ->
                        updateLikeButton(likeButton, status.user_liked)
                    }
                }
            }

            override fun onFailure(call: Call<LikeStatusResponse>, t: Throwable) {
                Log.e(TAG, "좋아요 상태 확인 실패", t)
            }
        })
    }

    private fun toggleLike(diaryId: Int, likeButton: ImageButton, likeCountText: TextView) {
        mainApiService.toggleLike(diaryId).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { likeResponse ->
                        val isLiked = likeResponse.message == "Liked!"
                        updateLikeButton(likeButton, isLiked)
                        updateLikeCount(diaryId, likeCountText)
                    }
                }
            }

            override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                Log.e(TAG, "좋아요 토글 실패", t)
            }
        })
    }

    private fun updateLikeButton(likeButton: ImageButton, isLiked: Boolean) {
        likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun updateLikeCount(diaryId: Int, likeCountText: TextView) {
        mainApiService.getLikeCount(diaryId).enqueue(object : Callback<LikeCountResponse> {
            override fun onResponse(
                call: Call<LikeCountResponse>,
                response: Response<LikeCountResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { likeCount ->
                        likeCountText.text = "${likeCount.likes_count}"
                    }
                }
            }

            override fun onFailure(call: Call<LikeCountResponse>, t: Throwable) {
                Log.e(TAG, "좋아요 개수 가져오기 실패", t)
            }
        })
    }

    private fun getEmotionName(emotionCategory: Int): String {
        return when (emotionCategory) {
            1 -> "Happy 😊"
            2 -> "Excited 😎"
            3 -> "Soso 😐"
            4 -> "Sad 😢"
            5 -> "Angry 😠"
            6 -> "Tired 😫"
            else -> "Unknown"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ImageAdapter(
    private val onItemClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    private var diaries = listOf<DiaryEntry>()

    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(diary: DiaryEntry) {
            diary.image?.let { imageUrl ->
                val fullImageUrl = imageUrl.toFullImageUrl()
                Glide.with(itemView.context)
                    .load(fullImageUrl)
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
                            Log.e(TAG, "Image load failed for URL: $fullImageUrl")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d(TAG, "Image loaded successfully from: $fullImageUrl")
                            return false
                        }
                    })
                    .into(binding.imageView)
            }

            binding.root.setOnClickListener {
                onItemClick(diary)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(diaries[position])
    }

    override fun getItemCount() = diaries.size

    fun submitList(newDiaries: List<DiaryEntry>) {
        diaries = newDiaries
        notifyDataSetChanged()
    }
}