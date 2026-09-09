package com.creditdb.pro.ui.compare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.WorkItem
import com.creditdb.pro.data.WorksSortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val items: List<WorkItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val selectedVerdict: String = "all",
    val selectedTier: String = "all",
    val selectedEra: String = "all",
    val sortOption: WorksSortOption = WorksSortOption.RESIDUAL_DESC,
    val hasMore: Boolean = true,
    val showFilterSheet: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = selectedVerdict != "all" || selectedTier != "all" || selectedEra != "all" || sortOption != WorksSortOption.RESIDUAL_DESC
}

class CompareViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CreditRepository(application)

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val pageSize = 40
    private var currentOffset = 0

    init {
        fetchCompareItems(reset = true)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            fetchCompareItems(reset = true)
        }
    }

    fun onVerdictSelect(verdict: String) {
        if (_uiState.value.selectedVerdict == verdict) return
        _uiState.update { it.copy(selectedVerdict = verdict) }
        fetchCompareItems(reset = true)
    }

    fun onTierSelect(tier: String) {
        if (_uiState.value.selectedTier == tier) return
        _uiState.update { it.copy(selectedTier = tier) }
        fetchCompareItems(reset = true)
    }

    fun onEraSelect(era: String) {
        if (_uiState.value.selectedEra == era) return
        _uiState.update { it.copy(selectedEra = era) }
        fetchCompareItems(reset = true)
    }

    fun onSortOptionSelect(option: WorksSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.update { it.copy(sortOption = option) }
        fetchCompareItems(reset = true)
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showFilterSheet = visible) }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedVerdict = "all",
                selectedTier = "all",
                selectedEra = "all",
                sortOption = WorksSortOption.RESIDUAL_DESC
            )
        }
        fetchCompareItems(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        fetchCompareItems(reset = false)
    }

    private fun fetchCompareItems(reset: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            if (reset) {
                currentOffset = 0
                _uiState.update { it.copy(isLoading = true) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val list = repository.getWorks(
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

            currentOffset += list.size
            val hasMoreItems = list.size >= pageSize

            _uiState.update {
                it.copy(
                    items = if (reset) list else it.items + list,
                    totalCount = count,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = hasMoreItems
                )
            }
        }
    }
}
