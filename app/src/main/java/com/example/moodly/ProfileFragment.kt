package com.example.moodly

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.moodly.databinding.FragmentProfileBinding
import com.example.moodly.databinding.ItemImageBinding
import com.example.moodly.databinding.DialogImageDetailBinding
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.view.Window
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "ProfileFragment"

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ImageAdapter
    private lateinit var mainApiService: MainApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainApiService = RetrofitClient.getMainApiService(requireContext())
        setupRecyclerView()
        fetchImages()
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter(
            onItemClick = { diary ->
                showImageDetail(diary)
            },
            onImageLoadError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@ProfileFragment.adapter
        }
    }

    private fun fetchImages() {
        mainApiService.getFeed().enqueue(object : Callback<List<DiaryEntry>> {
            override fun onResponse(
                call: Call<List<DiaryEntry>>,
                response: Response<List<DiaryEntry>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { diaries ->
                        Log.d(TAG, "Received ${diaries.size} entries")
                        diaries.firstOrNull()?.let { firstDiary ->
                            Log.d(TAG, "First image data length: ${firstDiary.image?.length ?: 0}")
                            Log.d(TAG, "First image preview: ${firstDiary.image?.take(100)}")
                        }
                        adapter.submitList(diaries)
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "Error fetching images: $error")
                    Toast.makeText(context, "Failed to load images", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DiaryEntry>>, t: Throwable) {
                Log.e(TAG, "Network error", t)
                Toast.makeText(context, "Network error occurred", Toast.LENGTH_SHORT).show()
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

            diary.image?.let { base64Image ->
                try {
                    // URL safe base64 디코딩 시도
                    val imageBytes = Base64.decode(base64Image, Base64.URL_SAFE)
                    Glide.with(imageView)
                        .load(imageBytes)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading detail image", e)
                    try {
                        // 일반 base64 디코딩 시도
                        val imageBytes = Base64.decode(base64Image, Base64.NO_WRAP or Base64.NO_PADDING)
                        Glide.with(imageView)
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageView)
                    } catch (e: Exception) {
                        Log.e(TAG, "All decoding attempts failed", e)
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
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
                Log.e(TAG, "Failed to check like status", t)
            }
        })
    }

    private fun toggleLike(diaryId: Int, likeButton: ImageButton, likeCountText: TextView) {
        mainApiService.toggleLike(diaryId).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(
                call: Call<LikeResponse>,
                response: Response<LikeResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { likeResponse ->
                        val isLiked = likeResponse.message == "Liked!"
                        updateLikeButton(likeButton, isLiked)
                        updateLikeCount(diaryId, likeCountText)
                    }
                }
            }

            override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                Log.e(TAG, "Failed to toggle like", t)
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
                Log.e(TAG, "Failed to get like count", t)
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
    private val onItemClick: (DiaryEntry) -> Unit,
    private val onImageLoadError: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    private var diaries = listOf<DiaryEntry>()

    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(diary: DiaryEntry) {
            diary.image?.let { base64Image ->
                try {
                    // URL safe base64 디코딩 시도
                    val imageBytes = Base64.decode(base64Image, Base64.URL_SAFE)
                    Glide.with(binding.imageView)
                        .load(imageBytes)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .centerCrop()
                        .into(binding.imageView)
                } catch (e: Exception) {
                    Log.e("ImageAdapter", "Error loading grid image with URL_SAFE", e)
                    try {
                        // 일반 base64 디코딩 시도
                        val imageBytes = Base64.decode(base64Image, Base64.NO_WRAP or Base64.NO_PADDING)
                        Glide.with(binding.imageView)
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .centerCrop()
                            .into(binding.imageView)
                    } catch (e: Exception) {
                        Log.e("ImageAdapter", "All decoding attempts failed", e)
                        onImageLoadError("Failed to load image")
                    }
                }
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