package com.example.moodly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
    val navController = rememberNavController()
    var selectedItem = remember { "Home" }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = {
                    selectedItem = it
                    when (it) {
                        "Home" -> navController.navigate("home")
                        "Map" -> navController.navigate("map")
                        "Add" -> navController.navigate("add")
                        "Chatbot" -> navController.navigate("chatbot")
                        "Profile" -> navController.navigate("profile")
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("map") { MapScreen() }
            composable("add") { AddScreen() }
            composable("chatbot") { ChatbotScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}
