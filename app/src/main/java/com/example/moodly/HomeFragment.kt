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
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodly.databinding.FragmentHomeBinding
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
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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
                            recommendationAdapter.submitData(loadedBooks ?: listOf(), loadedVideos!!)
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
                override fun onResponse(call: Call<List<YoutubeVideo>>, response: Response<List<YoutubeVideo>>) {
                    if (response.isSuccessful) {
                        loadedVideos = response.body()
                        if (loadedBooks != null) {
                            recommendationAdapter.submitData(loadedBooks!!, loadedVideos ?: listOf())
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
        }
    }

    private fun setCalendarHeader() {
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        binding.calendarHeader.text = currentDate
    }

    private fun setupCalendar() {
        // 현재 날짜로 초기화할 때는 Calendar가 0-based이므로 +1 필요
        val currentDate = Calendar.getInstance()
        fetchDiariesForMonth(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH) + 1
        )

        // MaterialCalendarView에서 받은 month에는 +1할 필요 없음 (이미 보정되어 있음)
        binding.calendarView.setOnMonthChangedListener { widget, date ->
            fetchDiariesForMonth(date.year, date.month)
        }

        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                fetchDiariesForDay(date.year, date.month, date.day)
            }
        }
    }

    private fun fetchDiariesForMonth(year: Int, month: Int) {
        // MainApiService를 가져와서 사용
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForMonth(year, month)
            .enqueue(object : Callback<DaysResponse> {
                override fun onResponse(
                    call: Call<DaysResponse>,
                    response: Response<DaysResponse>
                ) {
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
        // MainApiService를 가져와서 사용
        val mainApiService = RetrofitClient.getMainApiService(requireContext())

        mainApiService.getDiariesForDay(year, month, day)
            .enqueue(object : Callback<List<DayDiary>> {
                override fun onResponse(
                    call: Call<List<DayDiary>>,
                    response: Response<List<DayDiary>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { diaries ->
                            binding.contentCard.visibility = View.VISIBLE
                            if (diaries.isEmpty()) {
                                // 일기가 없는 경우
                                binding.emptyDiaryText.visibility = View.VISIBLE
                                binding.diaryRecyclerView.visibility = View.GONE
                            } else {
                                // 일기가 있는 경우
                                binding.emptyDiaryText.visibility = View.GONE
                                binding.diaryRecyclerView.visibility = View.VISIBLE
                                diaryAdapter.updateDiaries(diaries)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<List<DayDiary>>, t: Throwable) {
                    Log.e("Diary", "Failed to fetch diaries", t)
                    binding.contentCard.visibility = View.VISIBLE
                    binding.emptyDiaryText.text = "Failed to load diaries"
                    binding.emptyDiaryText.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                }
            })
    }

    private fun highlightDiaryDates(year: Int, month: Int, days: List<Int>) {
        binding.calendarView.removeDecorators()

        val decorator = object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return day.year == year &&
                        day.month == month - 1 && // MaterialCalendarView는 0-based month를 사용
                        days.contains(day.day)
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
                R.id.menu_profile -> {
                    val navController = findNavController()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .setLaunchSingleTop(true)
                        .build()
                    navController.navigate(R.id.action_homeFragment_to_profileFragment, null, navOptions)
                    true
                }
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
                else -> false
            }
        }
        popupMenu.show()
    }

    inner class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {
        private var diaries: List<DayDiary> = listOf()

        inner class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleText: TextView = itemView.findViewById(R.id.titleText)
            val emotionText: TextView = itemView.findViewById(R.id.emotionText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_diary, parent, false)
            return DiaryViewHolder(view)
        }

        override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
            val diary = diaries[position]
            holder.titleText.text = diary.title
            val emoticons = diary.emotion_categories.map { emotion ->
                when (emotion.name) {
                    "Happy" -> "😊"
                    "Excited" -> "😎"
                    "Soso" -> "😐"
                    "Sad" -> "😕"
                    "Angry" -> "😠"
                    "Tired" -> "😪"
                    else -> ""
                }
            }
            holder.emotionText.text = emoticons.joinToString(" ")
        }

        override fun getItemCount() = diaries.size

        fun updateDiaries(newDiaries: List<DayDiary>) {
            diaries = newDiaries
            notifyDataSetChanged()
        }
    }

    // 새로운 어댑터 클래스 추가
    class RecommendationAdapter : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

        // 음악(질문 + YoutubeVideo)와 책(Book) 데이터를 담는 리스트
        private val items = mutableListOf<Any>() // Pair<String, YoutubeVideo> 또는 Book

        // ViewHolder 정의
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleText: TextView = itemView.findViewById(R.id.titleText)
            val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        }

        // ViewHolder 생성
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recommendation, parent, false)
            return ViewHolder(view)
        }

        // ViewHolder 데이터 바인딩
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            when (item) {
                is Pair<*, *> -> { // 음악 추천
                    val question = item.first as String
                    val video = item.second as YoutubeVideo
                    holder.titleText.text = question // 질문 텍스트 설정
                    holder.subtitleText.text = "Listen to Music" // 버튼 텍스트
                    holder.subtitleText.visibility = View.VISIBLE
                    holder.itemView.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.link))
                        holder.itemView.context.startActivity(intent) // 유튜브 링크 열기
                    }
                }
                is Book -> { // 책 추천
                    holder.titleText.text = item.title // 책 제목 설정
                    holder.subtitleText.text = "Find Book" // 버튼 텍스트
                    holder.subtitleText.visibility = View.VISIBLE
                    holder.itemView.setOnClickListener {
                        val url = "https://www.google.com/search?q=${item.title}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        holder.itemView.context.startActivity(intent) // 구글 검색 링크 열기
                    }
                }
            }
        }

        // RecyclerView 아이템 개수 반환ㄴ
        override fun getItemCount() = items.size

        // 데이터 교차 배치 후 RecyclerView 업데이트
        fun submitData(books: List<Book>, videos: List<YoutubeVideo>) {
            items.clear()

            // 음악과 책 데이터를 교차 배치
            for (i in 0 until 6) { // 음악은 항상 6개
                // 1. 음악 추가
                val video = videos[i]
                val emotionText = when (video.emotionCategory) { // emotionCategory에 따라 질문 텍스트 설정
                    1 -> "How about this music when you're happy?"
                    2 -> "How about this music when you're excited?"
                    3 -> "How about this music when you're feeling so-so?"
                    4 -> "How about this music when you're sad?"
                    5 -> "How about this music when you're angry?"
                    6 -> "How about this music when you're tired?"
                    else -> "How about this music?"
                }
                items.add(Pair(emotionText, video)) // 음악 데이터 추가

                // 2. 책 추가 (책이 5개로 고정되었으므로 순환적으로 추가)
                val bookIndex = i % books.size
                val book = books[bookIndex]
                items.add(book) // 책 데이터 추가
            }

            notifyDataSetChanged() // RecyclerView 갱신
        }
    }
}