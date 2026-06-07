package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// ==========================================
// 0. SECURITY & OBFUSCATION UTILITIES
// ==========================================
object SecurityUtils {
    private const val XOR_KEY = 'K'.code

    // Encrypt stream URLs from plain M3U8 string to Base64 XOR obfuscated form
    fun obfuscate(input: String): String {
        if (input.isBlank()) return ""
        val bytes = input.toByteArray(Charsets.UTF_8)
        val obfuscatedBytes = ByteArray(bytes.size) { i ->
            (bytes[i].toInt() xor XOR_KEY).toByte()
        }
        return Base64.encodeToString(obfuscatedBytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    // Decrypt obfuscated Base64 XOR streaming URLs back to playable plain strings
    fun deobfuscate(obfuscated: String): String {
        if (obfuscated.isBlank()) return ""
        return try {
            val bytes = Base64.decode(obfuscated, Base64.NO_WRAP or Base64.URL_SAFE)
            val originalBytes = ByteArray(bytes.size) { i ->
                (bytes[i].toInt() xor XOR_KEY).toByte()
            }
            String(originalBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            obfuscated
        }
    }

    // SHA-256 secure password hashing function
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

// ==========================================
// 1. DATA ENTITIES (Zero-argument Constructors for Firebase)
// ==========================================
data class MatchEntity(
    val id: Int = 0,
    val title: String = "",
    val team1Name: String = "",
    val team1Logo: String = "",
    val team2Name: String = "",
    val team2Logo: String = "",
    val category: String = "", // Cricket, Football, etc.
    val status: String = "",   // live, upcoming, highlight
    val timeText: String = "", // e.g. "Starts in 2 hours", "Today, 8 PM"
    val server1Url: String = "",
    val server2Url: String = "",
    val serverHindiUrl: String = ""
)

data class ChannelEntity(
    val id: Int = 0,
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val categoryName: String = "" // Bangladesh TV, Sports TV, Movies TV
)

data class AppSettingsEntity(
    val id: Int = 1,
    val popupMessage: String = "",
    val popupLink: String = "",
    val showPopup: Boolean = false,
    val maintenanceMode: Boolean = false,
    val adminPasswordHash: String = "fccd36c9233ff8f6bc06a38ecef4ac3dbe04085e7a9e34a06cd1ab7289eeac66", // SHA-256 hash representation of "Kh365@#mIn$StReAm!2026"
    val adminUsername: String = "Khela365_Admin"
)

// ==========================================
// 2. FIREBASE CLASSICAL RECONCILIATOR
// ==========================================
class KhelaRepository(context: Context) {
    private var database: FirebaseDatabase
    private var matchesRef: DatabaseReference
    private var channelsRef: DatabaseReference
    private var settingsRef: DatabaseReference

    private val _allMatches = MutableStateFlow<List<MatchEntity>>(emptyList())
    val allMatches: Flow<List<MatchEntity>> = _allMatches

    val liveMatches: Flow<List<MatchEntity>> = _allMatches.map { list ->
        list.filter { it.status == "live" }
    }

    val upcomingMatches: Flow<List<MatchEntity>> = _allMatches.map { list ->
        list.filter { it.status == "upcoming" }
    }

    val highlightMatches: Flow<List<MatchEntity>> = _allMatches.map { list ->
        list.filter { it.status == "highlight" }
    }

    private val _allChannels = MutableStateFlow<List<ChannelEntity>>(emptyList())
    val allChannels: Flow<List<ChannelEntity>> = _allChannels

    val channelCategories: Flow<List<String>> = _allChannels.map { list ->
        list.map { it.categoryName }.distinct().sorted()
    }

    private val _appSettings = MutableStateFlow<AppSettingsEntity?>(null)
    val appSettings: Flow<AppSettingsEntity?> = _appSettings

    init {
        val dbUrl = try {
            BuildConfig.FIREBASE_DATABASE_URL
        } catch (e: Exception) {
            ""
        }.ifBlank { "https://khela365-live-default-rtdb.firebaseio.com/" }

        // Programmatic Firebase Setup to dynamically bind the credentials at Runtime
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:60f0cfde5b484d44b909329a874a74c4:android:debug")
                    .setProjectId("khela-365-live")
                    .setDatabaseUrl(dbUrl)
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }
        } catch (e: Exception) {
            Log.e("FirebaseSetup", "Error during dynamic Firebase bootstrapping", e)
        }

        database = try {
            FirebaseDatabase.getInstance(dbUrl)
        } catch (e: Exception) {
            Log.e("FirebaseSetup", "Failed to initialize with secure dbUrl, falling back to default", e)
            FirebaseDatabase.getInstance()
        }
        matchesRef = database.getReference("matches")
        channelsRef = database.getReference("channels")
        settingsRef = database.getReference("app_settings")

        // 1. Firebase value listener for Live Matches
        matchesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MatchEntity>()
                for (child in snapshot.children) {
                    try {
                        val match = child.getValue(MatchEntity::class.java)
                        if (match != null) {
                            list.add(match)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseParsing", "Failed to parse MatchEntity", e)
                    }
                }
                _allMatches.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDatabase", "Matches subscription cancelled: ${error.message}")
            }
        })

        // 2. Firebase value listener for Live IPTV Channels
        channelsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChannelEntity>()
                for (child in snapshot.children) {
                    try {
                        val channel = child.getValue(ChannelEntity::class.java)
                        if (channel != null) {
                            list.add(channel)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseParsing", "Failed to parse ChannelEntity", e)
                    }
                }
                _allChannels.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDatabase", "Channels subscription cancelled: ${error.message}")
            }
        })

        // 3. Firebase value listener for Master App Settings
        settingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val settings = snapshot.getValue(AppSettingsEntity::class.java)
                    if (settings != null) {
                        _appSettings.value = settings
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseParsing", "Failed to parse AppSettingsEntity", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDatabase", "Settings subscription cancelled: ${error.message}")
            }
        })
    }

    suspend fun getMatchById(matchId: Int): MatchEntity? {
        return _allMatches.value.find { it.id == matchId }
    }

    fun insertMatch(match: MatchEntity) {
        val calculatedId = if (match.id == 0) {
            val maxId = _allMatches.value.maxOfOrNull { it.id } ?: 0
            maxId + 1
        } else {
            match.id
        }
        val targetMatch = match.copy(id = calculatedId)
        matchesRef.child(calculatedId.toString()).setValue(targetMatch)
    }

    fun deleteMatch(matchId: Int) {
        matchesRef.child(matchId.toString()).removeValue()
    }

    fun insertChannel(channel: ChannelEntity) {
        val calculatedId = if (channel.id == 0) {
            val maxId = _allChannels.value.maxOfOrNull { it.id } ?: 0
            maxId + 1
        } else {
            channel.id
        }
        val targetChannel = channel.copy(id = calculatedId)
        channelsRef.child(calculatedId.toString()).setValue(targetChannel)
    }

    fun deleteChannel(channelId: Int) {
        channelsRef.child(channelId.toString()).removeValue()
    }

    fun updateSettings(settings: AppSettingsEntity) {
        settingsRef.setValue(settings)
    }

    fun getChannelsByCategory(categoryName: String): Flow<List<ChannelEntity>> =
        _allChannels.map { list ->
            list.filter { it.categoryName == categoryName }
        }

    // Seeding sample live sports streams, channels and universal app configurations
    fun seedDatabaseIfEmpty() {
        settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dbHash = snapshot.child("adminPasswordHash").getValue(String::class.java)
                val dbUser = snapshot.child("adminUsername").getValue(String::class.java)
                val targetHash = SecurityUtils.sha256("Kh365@#mIn\$StReAm!2026")
                
                if (!snapshot.exists() || dbUser != "Khela365_Admin" || dbHash != targetHash) {
                    val defaultSettings = AppSettingsEntity(
                        id = 1,
                        popupMessage = snapshot.child("popupMessage").getValue(String::class.java) ?: "আমাদের অফিশিয়াল টেলিগ্রাম চ্যানেলে যুক্ত হোন সব আপডেট সবার আগে পেতে!",
                        popupLink = snapshot.child("popupLink").getValue(String::class.java) ?: "https://t.me/khela365_official",
                        showPopup = snapshot.child("showPopup").getValue(Boolean::class.java) ?: true,
                        maintenanceMode = snapshot.child("maintenanceMode").getValue(Boolean::class.java) ?: false,
                        adminPasswordHash = targetHash,
                        adminUsername = "Khela365_Admin"
                    )
                    settingsRef.setValue(defaultSettings)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        matchesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val matchesSample = listOf(
                        MatchEntity(
                            id = 1,
                            title = "Bangladesh vs AFGHANISTAN",
                            team1Name = "Bangladesh",
                            team1Logo = "https://flagcdn.com/w160/bd.png",
                            team2Name = "Afghanistan",
                            team2Logo = "https://flagcdn.com/w160/af.png",
                            category = "Cricket",
                            status = "live",
                            timeText = "Live Now",
                            server1Url = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            server2Url = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            serverHindiUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
                        ),
                        MatchEntity(
                            id = 2,
                            title = "India vs Pakistan T20 World Cup",
                            team1Name = "India",
                            team1Logo = "https://flagcdn.com/w160/in.png",
                            team2Name = "Pakistan",
                            team2Logo = "https://flagcdn.com/w160/pk.png",
                            category = "Cricket",
                            status = "live",
                            timeText = "Live Now",
                            server1Url = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                            server2Url = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            serverHindiUrl = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8")
                        ),
                        MatchEntity(
                            id = 3,
                            title = "Real Madrid vs Barcelona",
                            team1Name = "Real Madrid",
                            team1Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Thg0vA7ZKVvWsh9m98varg_96x96.png",
                            team2Name = "FC Barcelona",
                            team2Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/6sLa9zZ_S0Pco6S6Cr6pGg_96x96.png",
                            category = "Football",
                            status = "upcoming",
                            timeText = "Starts: Tonight 01:00 AM",
                            server1Url = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            server2Url = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            serverHindiUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
                        ),
                        MatchEntity(
                            id = 4,
                            title = "Argentina vs Brazil friendly",
                            team1Name = "Argentina",
                            team1Logo = "https://flagcdn.com/w160/ar.png",
                            team2Name = "Brazil",
                            team2Logo = "https://flagcdn.com/w160/br.png",
                            category = "Football",
                            status = "upcoming",
                            timeText = "Starts: Tomorrow 06:30 AM",
                            server1Url = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            server2Url = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            serverHindiUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
                        ),
                        MatchEntity(
                            id = 5,
                            title = "Sri Lanka vs England Classics",
                            team1Name = "Sri Lanka",
                            team1Logo = "https://flagcdn.com/w160/lk.png",
                            team2Name = "England",
                            team2Logo = "https://flagcdn.com/w160/gb.png",
                            category = "Cricket",
                            status = "highlight",
                            timeText = "Match Highlights: Asia Cup",
                            server1Url = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            server2Url = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            serverHindiUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
                        )
                    )
                    for (m in matchesSample) {
                        matchesRef.child(m.id.toString()).setValue(m)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        channelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val channelsSample = listOf(
                        ChannelEntity(
                            id = 1,
                            name = "GTV Sports Live",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/G_tv_logo.png",
                            streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            categoryName = "Bangladesh TV"
                        ),
                        ChannelEntity(
                            id = 2,
                            name = "T Sports HD",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/T_Sports_Logo.png",
                            streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                            categoryName = "Bangladesh TV"
                        ),
                        ChannelEntity(
                            id = 3,
                            name = "BTV National",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/cd/Logo_of_BTV_National.jpg",
                            streamUrl = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            categoryName = "Bangladesh TV"
                        ),
                        ChannelEntity(
                            id = 4,
                            name = "Sony Sports Ten 1",
                            logoUrl = "https://logodownload.org/wp-content/uploads/2021/11/sony-ten-1-logo.png",
                            streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            categoryName = "Sports Channels"
                        ),
                        ChannelEntity(
                            id = 5,
                            name = "Star Sports HD",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/b/b3/Star-sports.png",
                            streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                            categoryName = "Sports Channels"
                        ),
                        ChannelEntity(
                            id = 6,
                            name = "Willow Cricket",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f6/Willow_Cricket_logo.svg/1200px-Willow_Cricket_logo.svg.png",
                            streamUrl = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                            categoryName = "Sports Channels"
                        ),
                        ChannelEntity(
                            id = 7,
                            name = "HBO Movies HD",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/de/HBO_logo.svg/1200px-HBO_logo.svg.png",
                            streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                            categoryName = "Movies TV"
                        ),
                        ChannelEntity(
                            id = 8,
                            name = "Sony Pix HD",
                            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/25/Sony_PIX_logo.png",
                            streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                            categoryName = "Movies TV"
                        )
                    )
                    for (c in channelsSample) {
                        channelsRef.child(c.id.toString()).setValue(c)
                    }
                }

                // Always force-insert/verify the requested review test channel is present:
                channelsRef.child("99").setValue(
                    ChannelEntity(
                        id = 99,
                        name = "T-Sports",
                        logoUrl = "https://ibb.co.com/zVJWD1YL",
                        streamUrl = SecurityUtils.obfuscate("https://tvsen7.aynaott.com/tsports-hd/tracks-v1a1/mono.ts.m3u8"),
                        categoryName = "Bangladesh"
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
