package com.pawsup.apppicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawsup.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPickerUiState(
    val allApps: List<InstalledApp> = emptyList(),
    val monitoredPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val filteredApps: List<InstalledApp>
        get() = if (searchQuery.isBlank()) allApps
                else allApps.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
}

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val appsRepo: InstalledAppsRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AppPickerUiState())
    val state: StateFlow<AppPickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.monitoredPackages.collect { monitored ->
                _state.update { it.copy(monitoredPackages = monitored) }
            }
        }
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val apps = appsRepo.getInstalledApps()
            _state.update { it.copy(allApps = apps, isLoading = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleMonitored(packageName: String) {
        viewModelScope.launch {
            prefs.togglePackage(packageName)
        }
    }
}
