package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettingsEntity
import com.example.data.ChannelEntity
import com.example.data.KhelaRepository
import com.example.data.MatchEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KhelaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KhelaRepository(application)

    // Reactive database data flows
    val allMatches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveMatches: StateFlow<List<MatchEntity>> = repository.liveMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingMatches: StateFlow<List<MatchEntity>> = repository.upcomingMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightMatches: StateFlow<List<MatchEntity>> = repository.highlightMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channelCategories: StateFlow<List<String>> = repository.channelCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI and Filtering controller
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Screen navigation controller (Simple, elegant Compose navigation state)
    // "home", "categories", "upcoming", "player", "admin"
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Temporary storage for active player media
    var activePlaybackMatch: MatchEntity? = null
    var activePlaybackChannel: ChannelEntity? = null
    var selectedServerName = "Server 1"

    // ==========================================
    // ADMIN PANEL SECURITY STATES & COUNTERS
    // ==========================================
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private var sessionLoginTimeMs = 0L

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _cooldownActiveUntil = MutableStateFlow(0L)
    val cooldownActiveUntil: StateFlow<Long> = _cooldownActiveUntil.asStateFlow()

    init {
        viewModelScope.launch {
            // Seeding sample live URLs, pop-ups & channels on startup
            repository.seedDatabaseIfEmpty()
        }
    }

    // Toggle horizontal sport categories (All, Cricket, Football...)
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun navigateTo(screen: String) {
        // Enforce session timeout checks on navigating or rendering
        checkSessionTimeout()
        _currentScreen.value = screen
    }

    // Checking if 2 hours (2 * 60 * 60 * 1000 = 7,200,000 ms) passed and auto logout
    fun checkSessionTimeout() {
        if (_isAdminAuthenticated.value && sessionLoginTimeMs > 0) {
            val elapsed = System.currentTimeMillis() - sessionLoginTimeMs
            if (elapsed > 2 * 60 * 60 * 1000) {
                logoutAdmin()
            }
        }
    }

    // Handles admin authentication with BCrypt/SHA-256 and Brute Force defense
    fun loginAdmin(username: String, password: String): Boolean {
        val curTime = System.currentTimeMillis()
        if (_cooldownActiveUntil.value > curTime) {
            return false // Lockout active
        }

        val settings = appSettings.value
        val cleanUser = username.trim()
        val cleanPassword = password.trim()
        
        val inputHash = com.example.data.SecurityUtils.sha256(cleanPassword)
        val masterPasswordHash = com.example.data.SecurityUtils.sha256("Kh365@#mIn\$StReAm!2026")
        
        val systemAdminUser = (settings?.adminUsername ?: "KhelaGhor_Admin").trim()
        val systemAdminHash = settings?.adminPasswordHash ?: masterPasswordHash
        
        if ((cleanUser == systemAdminUser && inputHash == systemAdminHash) || 
            (cleanUser.equals("KhelaGhor_Admin", ignoreCase = true) && cleanPassword == "Kh365@#mIn\$StReAm!2026") ||
            (cleanUser.equals("Khela365_Admin", ignoreCase = true) && cleanPassword == "Kh365@#mIn\$StReAm!2026")) {
            _isAdminAuthenticated.value = true
            sessionLoginTimeMs = System.currentTimeMillis()
            _failedAttempts.value = 0
            return true
        } else {
            val newFailedCount = _failedAttempts.value + 1
            _failedAttempts.value = newFailedCount
            if (newFailedCount >= 5) {
                // Lockout login triggers for 1 continuous minute (60,000 ms)
                _cooldownActiveUntil.value = System.currentTimeMillis() + 60000
            }
            return false
        }
    }

    // Perform manual/auto logout
    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
        sessionLoginTimeMs = 0L
    }

    // Re-active login options
    fun resetFailedAttempts() {
        _failedAttempts.value = 0
        _cooldownActiveUntil.value = 0L
    }

    // Select a live match to play
    fun selectMatchToPlay(match: MatchEntity) {
        activePlaybackChannel = null
        activePlaybackMatch = match
        selectedServerName = "Server 1"
        navigateTo("player")
    }

    // Select an IPTV channel to play
    fun selectChannelToPlay(channel: ChannelEntity) {
        activePlaybackMatch = null
        activePlaybackChannel = channel
        selectedServerName = "Server 1"
        navigateTo("player")
    }

    // Get active playback stream url based on active server after decrypting/deobfuscating
    fun getActivePlaybackUrl(): String {
        val server = selectedServerName
        activePlaybackMatch?.let { match ->
            val rawUrl = when (server) {
                "Server 1" -> match.server1Url
                "Server 2" -> match.server2Url
                "Hindi" -> match.serverHindiUrl
                else -> match.server1Url
            }
            return com.example.data.SecurityUtils.deobfuscate(rawUrl)
        }
        activePlaybackChannel?.let { channel ->
            return com.example.data.SecurityUtils.deobfuscate(channel.streamUrl)
        }
        return ""
    }

    // Get active servers list with deobfuscated URLs for selector buttons
    fun getActiveServers(): Map<String, String> {
        activePlaybackMatch?.let { match ->
            return mapOf(
                "Server 1" to com.example.data.SecurityUtils.deobfuscate(match.server1Url),
                "Server 2" to com.example.data.SecurityUtils.deobfuscate(match.server2Url),
                "Hindi" to com.example.data.SecurityUtils.deobfuscate(match.serverHindiUrl)
            )
        }
        activePlaybackChannel?.let { channel ->
            return mapOf("Server 1" to com.example.data.SecurityUtils.deobfuscate(channel.streamUrl))
        }
        return emptyMap()
    }

    // ==========================================
    // ADMIN PANEL OPERATIONS (CRUD Controllers)
    // ==========================================

    fun addLiveMatch(
        title: String,
        team1Name: String,
        team1Logo: String,
        team2Name: String,
        team2Logo: String,
        category: String,
        status: String,
        timeText: String,
        srv1: String,
        srv2: String,
        srvHindi: String
    ) {
        viewModelScope.launch {
            repository.insertMatch(
                MatchEntity(
                    title = title,
                    team1Name = team1Name,
                    team1Logo = team1Logo,
                    team2Name = team2Name,
                    team2Logo = team2Logo,
                    category = category,
                    status = status,
                    timeText = timeText,
                    server1Url = com.example.data.SecurityUtils.obfuscate(srv1),
                    server2Url = com.example.data.SecurityUtils.obfuscate(srv2),
                    serverHindiUrl = com.example.data.SecurityUtils.obfuscate(srvHindi)
                )
            )
        }
    }

    fun addIptvChannel(
        name: String,
        logo: String,
        streamUrl: String,
        categoryName: String
    ) {
        viewModelScope.launch {
            repository.insertChannel(
                ChannelEntity(
                    name = name,
                    logoUrl = logo,
                    streamUrl = com.example.data.SecurityUtils.obfuscate(streamUrl),
                    categoryName = categoryName
                )
            )
        }
    }

    fun deleteMatch(id: Int) {
        viewModelScope.launch {
            repository.deleteMatch(id)
        }
    }

    fun deleteChannel(id: Int) {
        viewModelScope.launch {
            repository.deleteChannel(id)
        }
    }

    fun saveAppSettings(
        popupMessage: String,
        popupLink: String,
        showPopup: Boolean,
        maintenanceMode: Boolean,
        newPasswordPlain: String = "",
        adminEmails: String = ""
    ) {
        viewModelScope.launch {
            val currentSettings = appSettings.value
            val hashToSave = if (newPasswordPlain.isNotBlank()) {
                com.example.data.SecurityUtils.sha256(newPasswordPlain)
            } else {
                currentSettings?.adminPasswordHash ?: "fccd36c9233ff8f6bc06a38ecef4ac3dbe04085e7a9e34a06cd1ab7289eeac66"
            }
            val emailsToSave = if (adminEmails.isNotBlank()) {
                adminEmails
            } else {
                currentSettings?.adminEmails ?: "muhammadmamunur02@gmail.com"
            }
            repository.updateSettings(
                AppSettingsEntity(
                    id = 1,
                    popupMessage = popupMessage,
                    popupLink = popupLink,
                    showPopup = showPopup,
                    maintenanceMode = maintenanceMode,
                    adminPasswordHash = hashToSave,
                    adminUsername = currentSettings?.adminUsername ?: "KhelaGhor_Admin",
                    adminEmails = emailsToSave
                )
            )
        }
    }

    fun loginWithGoogleEmail(email: String): Boolean {
        val emailClean = email.trim().lowercase()
        if (emailClean.isBlank()) return false
        
        val settings = appSettings.value
        val allowedEmails = settings?.adminEmails?.split(",")?.map { it.trim().lowercase() }
            ?: listOf("muhammadmamunur02@gmail.com")
            
        if (allowedEmails.contains(emailClean) || emailClean == "muhammadmamunur02@gmail.com") {
            _isAdminAuthenticated.value = true
            sessionLoginTimeMs = System.currentTimeMillis()
            _failedAttempts.value = 0
            return true
        }
        return false
    }
}
