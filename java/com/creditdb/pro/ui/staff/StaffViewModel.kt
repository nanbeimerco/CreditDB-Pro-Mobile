package com.creditdb.pro.ui.staff

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.LeaderboardItem
import com.creditdb.pro.data.StaffSortOption
import com.creditdb.pro.data.SummaryInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffUiState(
    val summary: SummaryInfo? = null,
    val items: List<LeaderboardItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedRole: String = "all",
    val searchQuery: String = "",
    val sortOption: StaffSortOption = StaffSortOption.RATING,
    val hasMore: Boolean = true
)

class StaffViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CreditRepository(application)

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val pageSize = 40
    private var currentOffset = 0

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val sum = repository.getSummary()
            _uiState.update { it.copy(summary = sum) }
            fetchLeaderboard(reset = true)
        }
    }

    fun onRoleSelect(role: String) {
        if (_uiState.value.selectedRole == role) return
        _uiState.update { it.copy(selectedRole = role) }
        fetchLeaderboard(reset = true)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200) // デバウンス
            fetchLeaderboard(reset = true)
        }
    }

    fun onSortOptionSelect(option: StaffSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.update { it.copy(sortOption = option) }
        fetchLeaderboard(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        fetchLeaderboard(reset = false)
    }

    private fun fetchLeaderboard(reset: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            if (reset) {
                currentOffset = 0
                _uiState.update { it.copy(isLoading = true) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val fetched = repository.getLeaderboard(
                role = state.selectedRole,
                query = state.searchQuery,
                sortOption = state.sortOption,
                limit = pageSize,
                offset = currentOffset
            )

            val total = if (reset) {
                repository.getLeaderboardCount(
                    role = state.selectedRole,
                    query = state.searchQuery
                )
            } else {
                state.totalCount
            }

            currentOffset += fetched.size
            val hasMore = fetched.size == pageSize && currentOffset < total

            _uiState.update {
                it.copy(
                    items = if (reset) fetched else it.items + fetched,
                    totalCount = total,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = hasMore
                )
            }
        }
    }
}
