package com.creditdb.pro.ui.works

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.SummaryInfo
import com.creditdb.pro.data.WorkItem
import com.creditdb.pro.data.WorksSortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorksUiState(
    val summary: SummaryInfo? = null,
    val works: List<WorkItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val selectedTier: String = "all",
    val selectedEra: String = "all",
    val selectedVerdict: String = "all",
    val sortOption: WorksSortOption = WorksSortOption.DEVIATION_DESC,
    val hasMore: Boolean = true,
    val showFilterSheet: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = selectedTier != "all" || selectedEra != "all" || selectedVerdict != "all" || sortOption != WorksSortOption.DEVIATION_DESC
}

class WorksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CreditRepository(application)

    private val _uiState = MutableStateFlow(WorksUiState())
    val uiState: StateFlow<WorksUiState> = _uiState.asStateFlow()

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
            fetchWorks(reset = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            fetchWorks(reset = true)
        }
    }

    fun onTierSelect(tier: String) {
        if (_uiState.value.selectedTier == tier) return
        _uiState.update { it.copy(selectedTier = tier) }
        fetchWorks(reset = true)
    }

    fun onEraSelect(era: String) {
        if (_uiState.value.selectedEra == era) return
        _uiState.update { it.copy(selectedEra = era) }
        fetchWorks(reset = true)
    }

    fun onVerdictSelect(verdict: String) {
        if (_uiState.value.selectedVerdict == verdict) return
        _uiState.update { it.copy(selectedVerdict = verdict) }
        fetchWorks(reset = true)
    }

    fun onSortOptionSelect(option: WorksSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.update { it.copy(sortOption = option) }
        fetchWorks(reset = true)
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showFilterSheet = visible) }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedTier = "all",
                selectedEra = "all",
                selectedVerdict = "all",
                sortOption = WorksSortOption.DEVIATION_DESC
            )
        }
        fetchWorks(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        fetchWorks(reset = false)
    }

    private fun fetchWorks(reset: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            if (reset) {
                currentOffset = 0
                _uiState.update { it.copy(isLoading = true) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val items = repository.getWorks(
                query = state.searchQuery,
                tierFilter = state.selectedTier,
                eraFilter = state.selectedEra,
                verdictFilter = state.selectedVerdict,
                sortOption = state.sortOption,
                limit = pageSize,
                offset = currentOffset
            )

            val count = if (reset) {
                repository.getWorksCount(
                    query = state.searchQuery,
                    tierFilter = state.selectedTier,
                    eraFilter = state.selectedEra,
                    verdictFilter = state.selectedVerdict
                )
            } else {
                state.totalCount
            }

            currentOffset += items.size
            val hasMoreItems = items.size >= pageSize

            _uiState.update {
                it.copy(
                    works = if (reset) items else it.works + items,
                    totalCount = count,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = hasMoreItems
                )
            }
        }
    }
}
