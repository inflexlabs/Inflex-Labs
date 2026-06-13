package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseProvider
import com.example.data.StockNews
import com.example.data.SyncConfig
import com.example.data.SyncError
import com.example.api.StockSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketPulseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    private val newsDao = db.stockNewsDao()
    private val errorDao = db.syncErrorDao()
    private val configDao = db.syncConfigDao()

    // Screen navigation state: "AUTH_LOGIN", "AUTH_SIGNUP", "DASHBOARD", "HISTORY", "SIMULATED_SHEETS", "SIMULATED_EMAILS", "SETTINGS"
    private val _currentScreen = MutableStateFlow("AUTH_LOGIN")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Authentication States
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInEmail = MutableStateFlow("")
    val loggedInEmail: StateFlow<String> = _loggedInEmail.asStateFlow()

    // Form inputs for Auth
    var authEmailInput = MutableStateFlow("")
    var authPasswordInput = MutableStateFlow("")
    var authNameInput = MutableStateFlow("")

    // Operation states
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncCompletedTime = MutableStateFlow<Long?>(null)
    val lastSyncCompletedTime: StateFlow<Long?> = _lastSyncCompletedTime.asStateFlow()

    private val _lastSyncStatusMessage = MutableStateFlow<String?>(null)
    val lastSyncStatusMessage: StateFlow<String?> = _lastSyncStatusMessage.asStateFlow()

    private val _activeAlertMessage = MutableStateFlow<String?>(null)
    val activeAlertMessage: StateFlow<String?> = _activeAlertMessage.asStateFlow()

    private val _activeAlertDetails = MutableStateFlow<String?>(null)
    val activeAlertDetails: StateFlow<String?> = _activeAlertDetails.asStateFlow()

    // Room configurations flows
    val allNews: StateFlow<List<StockNews>> = newsDao.getAllNewsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allErrors: StateFlow<List<SyncError>> = errorDao.getAllErrorsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentConfig: StateFlow<SyncConfig?> = configDao.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConfig())

    // Custom test mail dispatch input fields
    var customEmailBodyInput = MutableStateFlow("")
    var customEmailSubjectInput = MutableStateFlow("")

    init {
        // Prepopulate default settings if empty
        viewModelScope.launch {
            val config = configDao.getConfig()
            if (config == null) {
                configDao.insertConfig(SyncConfig())
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // AUTH ACTIONS
    fun login(mode: String, customEmail: String = "") {
        viewModelScope.launch {
            _isLoggedIn.value = true
            val finalEmail = when (mode) {
                "Google" -> "google.user@gmail.com"
                "GitHub" -> "git.developer@github.com"
                else -> if (customEmail.isNotEmpty()) customEmail else authEmailInput.value.ifEmpty { "user@marketpulse.com" }
            }
            _loggedInEmail.value = finalEmail
            
            // Save email setup config
            val config = configDao.getConfig() ?: SyncConfig()
            configDao.insertConfig(config.copy(userEmail = finalEmail))
            
            // Clear credentials
            authEmailInput.value = ""
            authPasswordInput.value = ""
            
            _currentScreen.value = "DASHBOARD"
        }
    }

    fun signUp() {
        viewModelScope.launch {
            if (authEmailInput.value.isNotEmpty()) {
                login("Email", authEmailInput.value)
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _loggedInEmail.value = ""
        _currentScreen.value = "AUTH_LOGIN"
    }

    // SYNC ACTIONS
    fun triggerSync() {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncStatusMessage.value = "Scanning mailbox and running financial check..."
            
            val emailText = customEmailBodyInput.value.ifEmpty { null }
            val emailSubj = customEmailSubjectInput.value.ifEmpty { null }

            val result = StockSyncCoordinator.runSync(
                context = getApplication(),
                db = db,
                customEmailBody = emailText,
                customEmailSubject = emailSubj
            )

            _isSyncing.value = false
            _lastSyncCompletedTime.value = System.currentTimeMillis()

            if (result.isSuccess) {
                val news = result.getOrNull()
                _lastSyncStatusMessage.value = "Success! Saved ticker: ${news?.ticker}. Integrated to Google Sheet Row ${news?.sheetRowIndex}."
                _activeAlertMessage.value = null
                _activeAlertDetails.value = null
            } else {
                val error = result.exceptionOrNull()
                _lastSyncStatusMessage.value = "Error: ${error?.message}"
                _activeAlertMessage.value = "Sync Failure Occurred"
                _activeAlertDetails.value = error?.message ?: "Check settings error records for exact exception details."
            }

            // Reset custom input text back so it scans normal stock list on next click
            customEmailBodyInput.value = ""
            customEmailSubjectInput.value = ""
        }
    }

    // CONFIG CONTROLLERS
    fun updateConfig(
        userEmail: String,
        googleSheetName: String,
        financialApiKey: String,
        isSimulationErrorSheets: Boolean,
        isSimulationErrorEmail: Boolean
    ) {
        viewModelScope.launch {
            val oldConfig = configDao.getConfig() ?: SyncConfig()
            val newConfig = oldConfig.copy(
                userEmail = userEmail,
                googleSheetName = googleSheetName,
                financialApiKey = financialApiKey,
                isSimulationErrorSheets = isSimulationErrorSheets,
                isSimulationErrorEmail = isSimulationErrorEmail
            )
            configDao.insertConfig(newConfig)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            newsDao.clearNews()
            errorDao.clearErrors()
            _lastSyncStatusMessage.value = "Cleared historical logs successfully."
        }
    }

    fun dismissAlert() {
        _activeAlertMessage.value = null
        _activeAlertDetails.value = null
    }
}
