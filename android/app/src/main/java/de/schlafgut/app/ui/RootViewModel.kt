package de.schlafgut.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RootUiState(
    val isLoading: Boolean = true,
    val needsOnboarding: Boolean = false,
    val isLocked: Boolean = false,
    val authError: String? = null
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsOnboarding = !settings.onboardingCompleted,
                    isLocked = settings.appLockEnabled
                )
            }
        }
    }

    fun onAuthSuccess() {
        _uiState.update { it.copy(isLocked = false, authError = null) }
    }

    fun onAuthError(error: String) {
        _uiState.update { it.copy(authError = error) }
    }

    fun completeOnboarding(name: String, latency: Int) {
        viewModelScope.launch {
            repository.saveSettings(
                UserSettingsEntity(
                    userName = name,
                    defaultSleepLatency = latency,
                    onboardingCompleted = true
                )
            )
            _uiState.update { it.copy(needsOnboarding = false) }
        }
    }
}
