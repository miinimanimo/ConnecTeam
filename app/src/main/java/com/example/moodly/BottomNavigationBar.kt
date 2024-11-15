package com.example.moodly

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    val items = listOf(
        "Home" to R.drawable.home,
        "Map" to R.drawable.map,
        "Add" to R.drawable.plus,
        "Chatbot" to R.drawable.chat,
        "Profile" to R.drawable.profile
    )

    BottomNavigation(
        backgroundColor = Color(0xFFFBEFEF)
    ) {
        items.forEach { (title, icon) ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title
                    )
                },
                label = {
                    Text(text = title, fontSize = 10.sp)
                },
                selected = selectedItem == title,
                onClick = {
                    onItemSelected(title)
                }
            )
        }
    }
}
