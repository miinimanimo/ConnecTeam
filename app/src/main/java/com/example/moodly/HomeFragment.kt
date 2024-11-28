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
        val token = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("jwt_token", null)

        if (token != null) {
            val authToken = "Bearer $token"
            RetrofitClient.instance.getDiariesForMonth(authToken, year, month)
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
    }

    private fun fetchDiariesForDay(year: Int, month: Int, day: Int) {
        val token = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("jwt_token", null)

        if (token != null) {
            val authToken = "Bearer $token"
            RetrofitClient.instance.getDiariesForDay(authToken, year, month, day)
                .enqueue(object : Callback<List<DayDiary>> {
                    override fun onResponse(
                        call: Call<List<DayDiary>>,
                        response: Response<List<DayDiary>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { diaries ->
                                if (diaries.isEmpty()) {
                                    // 일기가 없는 경우
                                    binding.emptyMessageCard.visibility = View.VISIBLE
                                    binding.diaryRecyclerView.visibility = View.GONE
                                } else {
                                    // 일기가 있는 경우
                                    binding.emptyMessageCard.visibility = View.GONE
                                    binding.diaryRecyclerView.visibility = View.VISIBLE
                                    diaryAdapter.updateDiaries(diaries)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<DayDiary>>, t: Throwable) {
                        Log.e("Diary", "Failed to fetch diaries", t)
                        // 에러 발생 시에도 메시지 표시
                        binding.emptyDiaryText.text = "Failed to load diaries"
                        binding.emptyMessageCard.visibility = View.VISIBLE
                        binding.diaryRecyclerView.visibility = View.GONE
                    }
                })
        }
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
}