package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChatViewModel

enum class MainTab {
    CHAT,
    TEMPLATES,
    HISTORY,
    SETTINGS
}

@Composable
fun MainScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.CHAT) }
    var prefilledTextForChat by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.CHAT,
                    onClick = { selectedTab = MainTab.CHAT },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Conversations Tab") },
                    label = { Text("Chat", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_btn_chat")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.TEMPLATES,
                    onClick = { selectedTab = MainTab.TEMPLATES },
                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = "Templates Tab") },
                    label = { Text("Patterns", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_btn_templates")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.HISTORY,
                    onClick = { selectedTab = MainTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History Tracker Tab") },
                    label = { Text("Sessions", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_btn_history")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Preferences Tab") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_btn_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CHAT -> {
                    ChatScreen(
                        viewModel = viewModel,
                        prefilledText = prefilledTextForChat,
                        onClearPrefilled = { prefilledTextForChat = "" }
                    )
                }
                MainTab.TEMPLATES -> {
                    TemplatesScreen(
                        onSelectTemplate = { promptText ->
                            prefilledTextForChat = promptText
                            selectedTab = MainTab.CHAT
                        }
                    )
                }
                MainTab.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onSelectAndNavigate = {
                            selectedTab = MainTab.CHAT
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
