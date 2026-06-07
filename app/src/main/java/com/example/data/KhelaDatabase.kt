package com.example.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

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
// 1. DATA ENTITIES
// ==========================================

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val team1Name: String,
    val team1Logo: String,
    val team2Name: String,
    val team2Logo: String,
    val category: String, // Cricket, Football, etc.
    val status: String,   // live, upcoming, highlight
    val timeText: String, // e.g. "Starts in 2 hours", "Today, 8 PM"
    val server1Url: String,
    val server2Url: String,
    val serverHindiUrl: String
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val categoryName: String // Bangladesh TV, Sports TV, Movies TV
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val popupMessage: String,
    val popupLink: String,
    val showPopup: Boolean,
    val maintenanceMode: Boolean,
    val adminPasswordHash: String = "fccd36c9233ff8f6bc06a38ecef4ac3dbe04085e7a9e34a06cd1ab7289eeac66", // SHA-256 hash representation of "Kh365@#mIn$StReAm!2026"
    val adminUsername: String = "Khela365_Admin"
)

// ==========================================
// 2. DATA ACCESS OBJECT (DAO)
// ==========================================

@Dao
interface KhelaDao {
    // Matches
    @Query("SELECT * FROM matches ORDER BY status ASC, id DESC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'live'")
    fun getLiveMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'upcoming'")
    fun getUpcomingMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'highlight'")
    fun getHighlightMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Int)

    // Channels (IPTV)
    @Query("SELECT * FROM channels")
    fun getAllChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE categoryName = :categoryName")
    fun getChannelsByCategoryFlow(categoryName: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT categoryName FROM channels")
    fun getChannelCategoriesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: Int)

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)
}

// ==========================================
// 3. ROOM DATABASE CLASS
// ==========================================

@Database(entities = [MatchEntity::class, ChannelEntity::class, AppSettingsEntity::class], version = 3, exportSchema = false)
abstract class KhelaDatabase : RoomDatabase() {
    abstract fun khelaDao(): KhelaDao

    companion object {
        @Volatile
        private var INSTANCE: KhelaDatabase? = null

        fun getDatabase(context: Context): KhelaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KhelaDatabase::class.java,
                    "khela365_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. REPOSITORY WITH AUTO-SEEDING
// ==========================================

class KhelaRepository(private val khelaDao: KhelaDao) {
    val allMatches: Flow<List<MatchEntity>> = khelaDao.getAllMatchesFlow()
    val liveMatches: Flow<List<MatchEntity>> = khelaDao.getLiveMatchesFlow()
    val upcomingMatches: Flow<List<MatchEntity>> = khelaDao.getUpcomingMatchesFlow()
    val highlightMatches: Flow<List<MatchEntity>> = khelaDao.getHighlightMatchesFlow()
    val allChannels: Flow<List<ChannelEntity>> = khelaDao.getAllChannelsFlow()
    val channelCategories: Flow<List<String>> = khelaDao.getChannelCategoriesFlow()
    val appSettings: Flow<AppSettingsEntity?> = khelaDao.getSettingsFlow()

    fun getChannelsByCategory(categoryName: String): Flow<List<ChannelEntity>> =
        khelaDao.getChannelsByCategoryFlow(categoryName)

    suspend fun getMatchById(matchId: Int): MatchEntity? = khelaDao.getMatchById(matchId)

    suspend fun insertMatch(match: MatchEntity) = khelaDao.insertMatch(match)
    suspend fun deleteMatch(matchId: Int) = khelaDao.deleteMatchById(matchId)

    suspend fun insertChannel(channel: ChannelEntity) = khelaDao.insertChannel(channel)
    suspend fun deleteChannel(channelId: Int) = khelaDao.deleteChannelById(channelId)

    suspend fun updateSettings(settings: AppSettingsEntity) = khelaDao.insertSettings(settings)

    // Seeding sample live sports streams, channels, and universal app configurations
    suspend fun seedDatabaseIfEmpty() {
        // 1. Seed settings
        val settings = khelaDao.getSettingsDirect()
        if (settings == null) {
            khelaDao.insertSettings(
                AppSettingsEntity(
                    id = 1,
                    popupMessage = "আমাদের অফিশিয়াল টেলিগ্রাম চ্যানেলে যুক্ত হোন সব আপডেট সবার আগে পেতে!",
                    popupLink = "https://t.me/khela365_official",
                    showPopup = true,
                    maintenanceMode = false,
                    adminPasswordHash = SecurityUtils.sha256("Kh365@#mIn\$StReAm!2026"), // Hash of new master password
                    adminUsername = "Khela365_Admin"
                )
            )
        }

        // 2. Seed matches if empty (We will use verified, real demo M3U8 links for testing)
        // Public sample HLS links from streamtest or big buck bunny to guarantee 100% stable playback inside emulator
        val matchesSample = listOf(
            MatchEntity(
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

        // Seed matches
        val m1 = khelaDao.getMatchById(1)
        if (m1 == null) {
            for (match in matchesSample) {
                khelaDao.insertMatch(match)
            }
        }

        // 3. Seed Channels
        // Check if channel count is empty and then seed standard channels:
        val channelsSample = listOf(
            ChannelEntity(
                name = "GTV Sports Live",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/G_tv_logo.png",
                streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                categoryName = "Bangladesh TV"
            ),
            ChannelEntity(
                name = "T Sports HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/T_Sports_Logo.png",
                streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                categoryName = "Bangladesh TV"
            ),
            ChannelEntity(
                name = "BTV National",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/cd/Logo_of_BTV_National.jpg",
                streamUrl = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                categoryName = "Bangladesh TV"
            ),
            ChannelEntity(
                name = "Sony Sports Ten 1",
                logoUrl = "https://logodownload.org/wp-content/uploads/2021/11/sony-ten-1-logo.png",
                streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                categoryName = "Sports Channels"
            ),
            ChannelEntity(
                name = "Star Sports HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/b/b3/Star-sports.png",
                streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                categoryName = "Sports Channels"
            ),
            ChannelEntity(
                name = "Willow Cricket",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f6/Willow_Cricket_logo.svg/1200px-Willow_Cricket_logo.svg.png",
                streamUrl = SecurityUtils.obfuscate("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
                categoryName = "Sports Channels"
            ),
            ChannelEntity(
                name = "HBO Movies HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/de/HBO_logo.svg/1200px-HBO_logo.svg.png",
                streamUrl = SecurityUtils.obfuscate("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"),
                categoryName = "Movies TV"
            ),
            ChannelEntity(
                name = "Sony Pix HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/25/Sony_PIX_logo.png",
                streamUrl = SecurityUtils.obfuscate("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                categoryName = "Movies TV"
            )
        )

        // Seed channels (inserting if none exist)
        val allChans = khelaDao.getAllChannelsFlow()
        // We'll write a simple check or run insertions with REPLACE.
        // It's safer to always do a check of the first item to avoid writing multiple times.
        // Let's retrieve channel categories or a specific channels count if we had a count query, 
        // since we don't, we can just insert them on startup or if database is new, 
        // using settings as a proxy is clean or just seed them.
        if (settings == null) {
            for (chan in channelsSample) {
                khelaDao.insertChannel(chan)
            }
        }

        // Always force-insert/verify the requested review test channel is present:
        khelaDao.insertChannel(
            ChannelEntity(
                id = 99,
                name = "T-Sports",
                logoUrl = "https://ibb.co.com/zVJWD1YL",
                streamUrl = SecurityUtils.obfuscate("https://tvsen7.aynaott.com/tsports-hd/tracks-v1a1/mono.ts.m3u8"),
                categoryName = "Bangladesh"
            )
        )
    }
}
