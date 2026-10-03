package com.example.pilinara.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onVideoClick: (String) -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.searchState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        viewModel.setSearchKeyword(searchQuery)
        if (searchQuery.isNotEmpty()) {
            viewModel.performSearch()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索视频、UP主、番剧...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content based on state
            when {
                state.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("搜索失败: ${state.error}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.performSearch() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                state.searchResults.isNotEmpty() -> {
                    // Search results
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.searchResults) { item ->
                            // Render type-specific results (placeholder)
                        }
                    }
                }
                state.suggestions.isNotEmpty() -> {
                    // Suggestions
                    Column {
                        Text("搜索结果", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSuggestion(suggestion) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(suggestion.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                else -> {
                    // Default state with hot searches
                    Column {
                        Text("热门搜索", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val hotSearches = listOf(
                            "原神", "崩坏：星穹铁道", "英雄联盟", "进击的巨人",
                            "咒术回战", "间谍过家家", "鬼灭之刃", "eva"
                        )
                        
                        hotSearches.forEach { term ->
                            Text(
                                text = term,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        searchQuery = term
                                        viewModel.setSearchKeyword(term)
                                        viewModel.performSearch()
                                    }
                                    .padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
