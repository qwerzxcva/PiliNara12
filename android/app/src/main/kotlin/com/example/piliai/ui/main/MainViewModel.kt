package com.example.piliai.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }
}
