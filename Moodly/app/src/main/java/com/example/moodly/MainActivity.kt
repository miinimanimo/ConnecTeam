package com.example.moodly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import com.example.moodly.BottomNavigationBar
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title section
            Image(
                painter = painterResource(id = R.drawable.title),
                contentDescription = "Moodly Title",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "How are you feeling right now?" text and button
            Text(
                text = "How are you feeling right now?",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { /* 버튼 클릭 이벤트 */ },
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(text = "Express")
            }

            // Calendar section
            CalendarView()

            // List of entries below the calendar
            Spacer(modifier = Modifier.height(16.dp))
            SampleEntriesList()
        }
    }
}

@Composable
fun CalendarView() {
    val currentMonth = YearMonth.now()
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Header with days of the week
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            daysOfWeek.forEach {
                Text(text = it, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }

        // Calendar dates
        val startDayOffset = firstDayOfMonth.dayOfWeek.value % 7 // 월요일 시작 기준
        var dayCounter = 1

        for (week in 0..5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 0..6) {
                    if ((week == 0 && day < startDayOffset) || dayCounter > lastDayOfMonth.dayOfMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(dayCounter)
                        Text(
                            text = dayCounter.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .clickable { selectedDate = date }
                                .background(if (selectedDate == date) Color(0xFFD81B60) else Color.Transparent)
                                .padding(8.dp),
                            textAlign = TextAlign.Center,
                            color = if (selectedDate == date) Color.White else Color.Black
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
fun SampleEntriesList() {
    // PNG 파일을 사용한 예시 데이터
    val entries = listOf(
        "feeling sad......" to R.drawable.sad, // sad.png 사용
        "better" to R.drawable.happy // happy.png 사용
    )

    Column {
        entries.forEach { (text, iconRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { /* 항목 클릭 시 이벤트 */ },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Emoji",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

