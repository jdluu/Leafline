package com.jdluu.leafline.opds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.opds.OpdsConfigStore.config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface OpdsUiState {
    data object Idle : OpdsUiState
    data object Loading : OpdsUiState
    data class ConfigSaved(val config: OpdsServerConfig) : OpdsUiState
    data class Loaded(val navigation: List<OpdsNavigationEntry>) : OpdsUiState
    data class Error(val message: String) : OpdsUiState
}

class OpdsViewModel : ViewModel() {
    private val service = OpdsCatalogService()

    private val _uiState = MutableStateFlow<OpdsUiState>(OpdsUiState.Idle)
    val uiState: StateFlow<OpdsUiState> = _uiState

    fun saveConfig(url: String, username: String, password: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            _uiState.value = OpdsUiState.Error("Catalog URL is required")
            return
        }
        val newConfig = OpdsServerConfig(
            catalogUrl = trimmedUrl,
            username = username.trim(),
            password = password
        )
        config = newConfig
        _uiState.value = OpdsUiState.ConfigSaved(newConfig)
    }

    fun loadRootNavigation() {
        val current = config ?: run {
            _uiState.value = OpdsUiState.Error("Configure the OPDS catalog first")
            return
        }
        _uiState.value = OpdsUiState.Loading
        viewModelScope.launch {
            val result = service.fetchRootNavigation(current)
            val value = result.getOrNull()
            if (value != null) {
                _uiState.value = OpdsUiState.Loaded(value)
            } else {
                val failure = result.failureOrNull()
                _uiState.value = OpdsUiState.Error(failure?.message ?: "Failed to fetch catalog")
            }
        }
    }

    fun reset() {
        _uiState.value = OpdsUiState.Idle
    }
}
