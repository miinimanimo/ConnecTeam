package com.example.moodly

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.graphics.Rect
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodly.databinding.FragmentHomeBinding
import com.example.moodly.databinding.ItemRecommendationBinding
import com.example.moodly.databinding.ItemDiaryBinding
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val diaryAdapter = DiaryAdapter()
    private val recommendationAdapter = RecommendationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Title image 설정
        binding.titleImage.setImageResource(R.drawable.title)

        // 현재 날짜를 설정
        setCalendarHeader()

        // Express 버튼 클릭 시 이동
        binding.expressButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_expressFragment)
        }

        // 달력 초기화
        setupCalendar()

        // 메뉴 버튼 클릭 이벤트 추가
        binding.menuButton.setOnClickListener {
            showPopupMenu(it)
        }

        // RecyclerView 설정
        setupRecyclerView()

        // 추천 RecyclerView 설정
        setupRecommendationRecyclerView()

        // 추천 데이터 로드
        loadRecommendations()

    }

    private fun setupRecommendationRecyclerView() {
        binding.recommendationsRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendationAdapter
        }
    }

    private fun loadRecommendations() {
        val mainApiService = RetrofitClient.getMainApiService(requireContext())
        var loadedBooks: List<Book>? = null
        var loadedVideos: List<YoutubeVideo>? = null

        // 책 데이터 로드
        mainApiService.getBooks()
            .enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        loadedBooks = response.body()
                        if (loadedVideos != null) {
                            recommendationAdapter.submitData(
                                loadedBooks ?: listOf(),
                                loadedVideos!!
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    Log.e("Books", "Failed to fetch books", t)
                }
            })

        // 유튜브 비디오 데이터 로드
        mainApiService.getYoutubeVideos()
            .enqueue(object : Callback<List<YoutubeVideo>> {
                override fun onResponse(
                    call: Call<List<YoutubeVideo>>,
                    response: Response<List<YoutubeVideo>>
                ) {
                    if (response.isSuccessful) {
                        loadedVideos = response.body()
                        if (loadedBooks != null) {
                            recommendationAdapter.submitData(
                                loadedBooks!!,
                                loadedVideos ?: listOf()
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<List<YoutubeVideo>>, t: Throwable) {
                    Log.e("Videos", "Failed to fetch videos", t)
                }
            })
    }

    private fun setupRecyclerView() {
        binding.diaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter

            // 스크롤 설정 변경
            isNestedScrollingEnabled = false  // NestedScrollView 내부에서는 false로 설정

            // 아이템 간격 설정은 유지
            if (itemDecorationCount == 0) {
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.bottom = (12 * resources.displayMetrics.density).toInt()
                    }
                })
            }
        }
    }

    private fun setCalendarHeader() {
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        binding.calendarHeader.text = currentDate
    }

    private fun setupCalendar() {
        val currentDate = Calendar.getInstance()

        // 달력 초기화 시
        fetchDiariesForMonth(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH) + 1  // Calendar는 0-based라서 +1 필요
        )

        // 월 변경 리스너
        binding.calendarView.setOnMonthChangedListener { _, date ->
            fetchDiariesForMonth(
                date.year,
                date.month  // MaterialCalendarView는 이미 정확한 월을 주므로 변환 불필요
            )
        }

        // 날짜 선택 리스너
        binding.calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                fetchDiariesForDay(
                    date.year,
                    date.month,  // 여기도 변환 불필요
                    date.day
                )
            }
        }
    }

    private fun fetchDiariesForMonth(year: Int, month: Int) {
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForMonth(year, month)
            .enqueue(object : Callback<DaysResponse> {
                override fun onResponse(call: Call<DaysResponse>, response: Response<DaysResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { daysResponse ->
                            highlightDiaryDates(year, month, daysResponse.days)
                        }
                    }
                }

                override fun onFailure(call: Call<DaysResponse>, t: Throwable) {
                    Log.e("Calendar", "Failed to fetch diary dates", t)
                }
            })
    }

    private fun fetchDiariesForDay(year: Int, month: Int, day: Int) {
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForDay(year, month, day)
            .enqueue(object : Callback<List<DayDiary>> {
                override fun onResponse(call: Call<List<DayDiary>>, response: Response<List<DayDiary>>) {
                    // 서버 응답 전체를 로깅
                    Log.d("API Response", "Raw response code: ${response.code()}")
                    Log.d("API Response", "Raw response headers: ${response.headers()}")
                    Log.d("API Response", "Raw response body: ${response.body()}")

                    try {
                        if (response.isSuccessful && isAdded) {
                            response.body()?.let { diaries ->
                                // 각 일기 항목의 세부 정보를 로깅
                                diaries.forEach { diary ->
                                    Log.d("API Response", """
                                    Diary Details:
                                    - Title: ${diary.title}
                                    - Emotion Category: ${diary.emotion_categories}
                                    - Raw Data: $diary
                                """.trimIndent())
                                }

                                binding.contentCard.visibility = View.VISIBLE
                                if (diaries.isEmpty()) {
                                    binding.emptyDiaryText.visibility = View.VISIBLE
                                    binding.diaryRecyclerView.visibility = View.GONE
                                    com.google.android.material.snackbar.Snackbar.make(
                                        binding.root,
                                        "No diary entries for this date",
                                        1500
                                    ).show()
                                } else {
                                    binding.emptyDiaryText.visibility = View.GONE
                                    binding.diaryRecyclerView.visibility = View.VISIBLE
                                    diaryAdapter.updateDiaries(diaries)
                                }
                            }
                        } else {
                            // 실패한 응답의 에러 본문도 로깅
                            Log.e("API Response", "Error body: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("Diary", "Error processing diary response", e)
                        Log.e("Diary", "Stack trace:", e)
                        showErrorMessage()
                    }
                }

                override fun onFailure(call: Call<List<DayDiary>>, t: Throwable) {
                    Log.e("Diary", "Failed to fetch diaries", t)
                    Log.e("Diary", "Error message: ${t.message}")
                    Log.e("Diary", "Stack trace:", t)
                    if (isAdded) {
                        showErrorMessage()
                    }
                }

                private fun showErrorMessage() {
                    binding.contentCard.visibility = View.GONE
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "Failed to load diaries",
                        1500
                    ).show()
                }
            })
    }

    private fun highlightDiaryDates(year: Int, month: Int, days: List<Int>) {
        binding.calendarView.removeDecorators()

        Log.d("Calendar", "Highlighting dates for $year/$month: $days")

        val decorator = object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                val shouldDecorate = day.year == year &&
                        day.month == month &&
                        days.contains(day.day)

                Log.d("Calendar", "Checking date ${day.year}/${day.month}/${day.day}: $shouldDecorate")
                return shouldDecorate
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(DotSpan(5f, resources.getColor(R.color.purple_500)))
            }
        }

        binding.calendarView.addDecorator(decorator)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu = PopupMenu(requireContext(), anchor)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.menu_logout -> {
                    findNavController().navigate(R.id.action_homeFragment_to_loginActivity)
                    Toast.makeText(requireContext(), "Logged out.", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.menu_contact -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("회사 Contact")
                        .setMessage("contact@company.com")
                        .setPositiveButton("확인", null)
                        .show()
                    true
                }
                R.id.menu_my_diary_list -> {
                    val navController = findNavController()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .setLaunchSingleTop(true)
                        .build()
                    navController.navigate(
                        R.id.action_homeFragment_to_myDiaryListFragment,
                        null,
                        navOptions
                    )
                    true

                }
                R.id.menu_dashboard -> {
                    val navController = findNavController()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .setLaunchSingleTop(true)
                        .build()
                    navController.navigate(
                        R.id.action_homeFragment_to_dashboardFragment,
                        null,
                        navOptions
                    )
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    inner class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {
        private var diaries: List<DayDiary> = listOf()

        inner class DiaryViewHolder(private val binding: ItemDiaryBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(diary: DayDiary) {
                binding.root.setOnClickListener {
                    val bundle = Bundle().apply {
                        putInt("diaryId", diary.id)
                    }
                    itemView.findNavController().navigate(
                        R.id.action_homeFragment_to_diaryFragment,
                        bundle
                    )
                }
                binding.titleText.text = diary.title

                // 단일 감정 이모티콘
                val emoticon = when (diary.emotion_category) {
                    1 -> "😊"  // Happy
                    2 -> "😎"  // Excited
                    3 -> "😐"  // Soso (무표정)
                    4 -> "😢"  // Sad
                    5 -> "😡"  // Angry (빨간 화난 얼굴)
                    6 -> "😪"  // Tired
                    else -> "😐"
                }
                binding.emotionText.text = emoticon
                Log.d("DiaryAdapter", "Diary: title=${diary.title}, emotion=${diary.emotion_category}")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
            val binding = ItemDiaryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DiaryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
            holder.bind(diaries[position])
        }

        override fun getItemCount() = diaries.size

        fun updateDiaries(newDiaries: List<DayDiary>) {
            diaries = newDiaries
            notifyDataSetChanged()
        }
    }


    class RecommendationAdapter : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {
        private val items = mutableListOf<Any>()

        inner class ViewHolder(private val binding: ItemRecommendationBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Any) {
                when (item) {
                    is Pair<*, *> -> {
                        val video = item.second as YoutubeVideo
                        val emotion = item.first as Int

                        // 이모지와 감정 설정하는 부분은 그대로 유지
                        val (emotionText, emoji) = when (emotion) {
                            1 -> "Happy" to "Happy 😊"
                            2 -> "Excited" to "Excited 😎"
                            3 -> "Soso" to "Soso 😐"
                            4 -> "Sad" to "Sad 😢"
                            5 -> "Angry" to "Angry 😡"
                            6 -> "Tired" to "Tired 😪"
                            else -> "Unknown" to "🎵"
                        }
                        binding.headerText.text = "$emoji"
                        binding.titleText.text = "How about this music?"
                        binding.subtitleText.text = "Listen to Music"
                        binding.subtitleText.visibility = View.VISIBLE

                        // 여기만 수정: 일반 브라우저로 열기
                        binding.root.setOnClickListener {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(video.link)
                                itemView.context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    itemView.context,
                                    "Unable to open link",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    is Book -> { // 책 추천
                        binding.headerText.text = "How about this book?"
                        binding.titleText.text = item.title
                        binding.subtitleText.text = "Find Book"
                        binding.subtitleText.visibility = View.VISIBLE
                        binding.root.setOnClickListener {
                            val url = "https://www.google.com/search?q=${item.title}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            itemView.context.startActivity(intent)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRecommendationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        fun submitData(books: List<Book>, videos: List<YoutubeVideo>) {
            items.clear()
            // 각 감정별로 하나씩
            for (i in 1..6) {
                val video = videos[i - 1]  // 비디오는 그대로 사용
                // emotionCategory를 1부터 6까지 순서대로 지정
                items.add(Pair(i, video))

                val bookIndex = (i - 1) % books.size
                val book = books[bookIndex]
                items.add(book)
            }
            notifyDataSetChanged()
        }
    }
}