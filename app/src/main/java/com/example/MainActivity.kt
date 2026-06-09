package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.databinding.ActivityMainBinding
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KhelaMainCheck"
        
        // Enter your dynamic domain hosting api.php URL here.
        const val API_URL = "https://yourdomain.com/api.php"
        
        // Default smartlink popunder URL if database settings are empty or offline
        private const val DEFAULT_POPUNDER_URL = "https://www.profitablecpmrate.com/xjnd0129?key=e12be8f3edce82a85e9dfbb16042db61"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MatchAdapter
    private val matchModelList = ArrayList<MatchModel>()
    private val allMatchesList = ArrayList<MatchModel>()
    private lateinit var requestQueue: RequestQueue

    // Managed from dynamic control api
    private var networkPopunderUrl = DEFAULT_POPUNDER_URL
    private var adsMasterSwitch = "ON"

    private var selectedCategory = "All"
    private var selectedTab = "Live" // "Live", "Categories", "Upcoming"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize lists & RequestQueue
        requestQueue = Volley.newRequestQueue(this)
        binding.recyclerViewMatches.layoutManager = LinearLayoutManager(this)
        
        adapter = MatchAdapter(this, matchModelList)
        binding.recyclerViewMatches.adapter = adapter

        // Set up Category Slider Click Listeners
        binding.btnCatAll.setOnClickListener { updateCategoryFilter("All") }
        binding.btnCatCricket.setOnClickListener { updateCategoryFilter("Cricket") }
        binding.btnCatFootball.setOnClickListener { updateCategoryFilter("Football") }
        binding.btnCatOthers.setOnClickListener { updateCategoryFilter("Others") }

        // Set up Bottom Navigation Item Listeners
        binding.navLive.setOnClickListener { updateTabUI("Live") }
        binding.navCategories.setOnClickListener { updateTabUI("Categories") }
        binding.navUpcoming.setOnClickListener { updateTabUI("Upcoming") }

        // Start with Live Tab styling active
        updateTabUI("Live")

        // Fetch dynamic configuration entries from hosting PHP script
        fetchMatchesAndSettings()
    }

    private fun updateCategoryFilter(categoryName: String) {
        selectedCategory = categoryName
        
        // Reset category circles backgrounds
        binding.circleCatAll.setBackgroundResource(if (selectedCategory == "All") R.drawable.circle_active else R.drawable.circle_inactive)
        binding.circleCatCricket.setBackgroundResource(if (selectedCategory == "Cricket") R.drawable.circle_active else R.drawable.circle_inactive)
        binding.circleCatFootball.setBackgroundResource(if (selectedCategory == "Football") R.drawable.circle_active else R.drawable.circle_inactive)
        binding.circleCatOthers.setBackgroundResource(if (selectedCategory == "Others") R.drawable.circle_active else R.drawable.circle_inactive)

        // Reset category text colors
        binding.txtCatAll.setTextColor(if (selectedCategory == "All") 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.txtCatCricket.setTextColor(if (selectedCategory == "Cricket") 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.txtCatFootball.setTextColor(if (selectedCategory == "Football") 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.txtCatOthers.setTextColor(if (selectedCategory == "Others") 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())

        // Icon tints
        binding.imgCatAll.setColorFilter(if (selectedCategory == "All") 0xFF10D070.toInt() else 0xFFFFFFFF.toInt())
        binding.imgCatCricket.setColorFilter(if (selectedCategory == "Cricket") 0xFF10D070.toInt() else 0xFFFFFFFF.toInt())
        binding.imgCatFootball.setColorFilter(if (selectedCategory == "Football") 0xFF10D070.toInt() else 0xFFFFFFFF.toInt())
        binding.imgCatOthers.setColorFilter(if (selectedCategory == "Others") 0xFF10D070.toInt() else 0xFFFFFFFF.toInt())

        applyFilters()
    }

    private fun updateTabUI(tab: String) {
        selectedTab = tab

        // Live Tab styling
        val isLive = tab == "Live"
        binding.pillLive.setBackgroundResource(if (isLive) R.drawable.pill_active_nav else android.R.color.transparent)
        binding.textLive.setTextColor(if (isLive) 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.textLive.setTypeface(binding.textLive.typeface, if (isLive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        val imgLive = binding.pillLive.getChildAt(1) as? ImageView
        imgLive?.setColorFilter(if (isLive) 0xFF05140D.toInt() else 0xFFA0A5A2.toInt())

        // Categories Tab styling
        val isCat = tab == "Categories"
        binding.pillCategories.setBackgroundResource(if (isCat) R.drawable.pill_active_nav else android.R.color.transparent)
        binding.textCategories.setTextColor(if (isCat) 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.textCategories.setTypeface(binding.textCategories.typeface, if (isCat) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        val imgCat = binding.pillCategories.getChildAt(0) as? ImageView
        imgCat?.setColorFilter(if (isCat) 0xFF05140D.toInt() else 0xFFA0A5A2.toInt())

        // Upcoming Tab styling
        val isUpcoming = tab == "Upcoming"
        binding.pillUpcoming.setBackgroundResource(if (isUpcoming) R.drawable.pill_active_nav else android.R.color.transparent)
        binding.textUpcoming.setTextColor(if (isUpcoming) 0xFF10D070.toInt() else 0xFFA0A5A2.toInt())
        binding.textUpcoming.setTypeface(binding.textUpcoming.typeface, if (isUpcoming) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        val imgUpcoming = binding.pillUpcoming.getChildAt(0) as? ImageView
        imgUpcoming?.setColorFilter(if (isUpcoming) 0xFF05140D.toInt() else 0xFFA0A5A2.toInt())

        applyFilters()
    }

    private fun applyFilters() {
        matchModelList.clear()
        for (match in allMatchesList) {
            // Apply category filter
            val matchesCategory = if (selectedCategory == "All") {
                true
            } else {
                match.category.equals(selectedCategory, ignoreCase = true)
            }

            // Apply bottom tab filter
            val matchesTab = when (selectedTab) {
                "Live" -> true // Standard view showing all matches
                "Upcoming" -> match.status.equals("UPCOMING", ignoreCase = true)
                "Categories" -> true // Show general categories
                else -> true
            }

            if (matchesCategory && matchesTab) {
                matchModelList.add(match)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchMatchesAndSettings() {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, API_URL, null,
            { response ->
                try {
                    // Update settings & Ads controls dynamically
                    if (response.has("settings")) {
                        val settings = response.getJSONObject("settings")
                        adsMasterSwitch = settings.optString("ads_switch", "ON")
                        networkPopunderUrl = settings.optString("popunder_url", DEFAULT_POPUNDER_URL)

                        val bannerAdScript = settings.optString("banner_ad_script", "")
                        if ("ON".equals(adsMasterSwitch, ignoreCase = true) && bannerAdScript.isNotBlank()) {
                            renderAdsterraWebBanner(bannerAdScript)
                        } else {
                            binding.fallbackBannerView.visibility = View.VISIBLE
                            binding.bannerWebView.visibility = View.GONE
                        }
                    }

                    // Dynamically fill current matches array
                    allMatchesList.clear()
                    val matchesArray = response.getJSONArray("matches")
                    for (i in 0 until matchesArray.length()) {
                        val obj = matchesArray.getJSONObject(i)
                        val match = MatchModel(
                            id = obj.optInt("id", 0),
                            title = obj.optString("title", ""),
                            team1Name = obj.optString("team1Name", ""),
                            team1Logo = obj.optString("team1Logo", ""),
                            team2Name = obj.optString("team2Name", ""),
                            team2Logo = obj.optString("team2Logo", ""),
                            category = obj.optString("category", ""),
                            status = obj.optString("status", ""),
                            timeText = obj.optString("timeText", ""),
                            server1Url = obj.optString("server1Url", "")
                        )
                        allMatchesList.add(match)
                    }
                    applyFilters()

                } catch (e: JSONException) {
                    Log.e(TAG, "Parsing error: ${e.message}")
                    loadFallbackLocalSeededData()
                }
            },
            { error ->
                Log.w(TAG, "Dynamic hosting offline or not yet configured. Loading elegant fallback live matches.")
                loadFallbackLocalSeededData()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun renderAdsterraWebBanner(htmlScript: String) {
        binding.bannerWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.bannerWebView.webChromeClient = WebChromeClient()

        // Wrap raw code securely inside high-performance responsive container
        val customHtml = """
            <html>
            <body style='margin:0;padding:0;display:flex;justify-content:center;align-items:center;background:#05140D;'>
                $htmlScript
            </body>
            </html>
        """.trimIndent()

        binding.bannerWebView.loadDataWithBaseURL(null, customHtml, "text/html", "UTF-8", null)
        binding.bannerWebView.visibility = View.VISIBLE
        binding.fallbackBannerView.visibility = View.GONE
    }

    private fun loadFallbackLocalSeededData() {
        allMatchesList.clear()
        allMatchesList.add(
            MatchModel(
                id = 1,
                title = "ICC World Cup Live • Super Eight Match 5",
                team1Name = "Bangladesh",
                team1Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/94LthnB6T79YAdQ6pX2CJA_48x48.png",
                team2Name = "India",
                team2Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/v9YOF6Zco_g0fXQofY77vQ_48x48.png",
                category = "Cricket",
                status = "LIVE",
                timeText = "Match In Progress",
                server1Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8" // Beautiful verification HLS stream link
            )
        )
        allMatchesList.add(
            MatchModel(
                id = 2,
                title = "EPL Football Derby • Premier Matches",
                team1Name = "Man United",
                team1Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/udY6snE7v8EG0kiN8l6gGA_48x48.png",
                team2Name = "Chelsea",
                team2Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/fhg62Y7YHA0ki6g_48x48.png",
                category = "Football",
                status = "UPCOMING",
                timeText = "Today at 10 PM",
                server1Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            )
        )
        allMatchesList.add(
            MatchModel(
                id = 3,
                title = "T20 Series International Cup",
                team1Name = "Australia",
                team1Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/mS9g6g77_g10oP_48x48.png",
                team2Name = "Pakistan",
                team2Logo = "https://ssl.gstatic.com/onebox/media/sports/logos/z4V5gG8_Z10XQ_48x48.png",
                category = "Cricket",
                status = "HIGHLIGHT",
                timeText = "Completed Today",
                server1Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            )
        )
        applyFilters()
    }

    fun onMatchClicked(match: MatchModel) {
        if ("ON".equals(adsMasterSwitch, ignoreCase = true)) {
            // 1. POP-UNDER OUTBOUND TRICK: Open smartlink instantly in Chrome background
            openPopUnderOutboundUrl()

            // 2. INTERSTITIAL COUNTDOWN: Give app local professional transition
            showInterstitialProgressDialog(match)
        } else {
            // Safe straight launch if master ad switch is OFF
            launchExoPlayer(match.server1Url)
        }
    }

    private fun openPopUnderOutboundUrl() {
        try {
            val parsedUri = Uri.parse(networkPopunderUrl)
            val chromeIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.chrome")
            }
            startActivity(chromeIntent)
        } catch (e: ActivityNotFoundException) {
            // Default browser fallback if google chrome is uninstalled
            val defaultIntent = Intent(Intent.ACTION_VIEW, Uri.parse(networkPopunderUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(defaultIntent)
        }
    }

    private fun showInterstitialProgressDialog(match: MatchModel) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.match_item, null)
        
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()

        // Transform standard list components inside overlay card to display sponsor message
        dialogView.findViewById<TextView>(R.id.matchStatus)?.apply {
            text = "SPONSOR AD"
            setBackgroundResource(R.drawable.capsule_tag_green)
            setTextColor(0xFF10D070.toInt())
        }

        dialogView.findViewById<TextView>(R.id.matchCategory)?.apply {
            text = "Sponsor"
        }

        dialogView.findViewById<TextView>(R.id.matchTitleText)?.apply {
            text = "Loading premium streaming links. Please support sponsors!"
        }

        val btnWatchLive = dialogView.findViewById<View>(R.id.btnWatchLive)
        val btnText = dialogView.findViewById<TextView>(R.id.btnText)
        btnWatchLive?.isEnabled = false
        btnText?.text = "SKIP AD (3s)..."

        // Count down timer
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                dialog.dismiss()
            } catch (ignored: Exception) {}
            launchExoPlayer(match.server1Url)
        }, 3000)
    }

    private fun launchExoPlayer(url: String) {
        if (url.trim().isEmpty()) {
            Toast.makeText(this, "Streaming Server Down!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("STREAM_URL", url)
        }
        startActivity(intent)
    }

    // High performance model POCO class
    data class MatchModel(
        val id: Int,
        val title: String,
        val team1Name: String,
        val team1Logo: String,
        val team2Name: String,
        val team2Logo: String,
        val category: String,
        val status: String,
        val timeText: String,
        val server1Url: String
    )

    // Adapter Implementation
    private class MatchAdapter(
        private val activity: MainActivity,
        private val list: List<MatchModel>
    ) : RecyclerView.Adapter<MatchAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val matchTitle: TextView = itemView.findViewById(R.id.matchTitleText)
            val team1: TextView = itemView.findViewById(R.id.team1Name)
            val team2: TextView = itemView.findViewById(R.id.team2Name)
            val status: TextView = itemView.findViewById(R.id.matchStatus)
            val category: TextView = itemView.findViewById(R.id.matchCategory)
            val logo1: ImageView = itemView.findViewById(R.id.team1Logo)
            val logo2: ImageView = itemView.findViewById(R.id.team2Logo)
            val btnWatch: View = itemView.findViewById(R.id.btnWatchLive)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.match_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.matchTitle.text = item.title
            holder.team1.text = item.team1Name
            holder.team2.text = item.team2Name
            holder.status.text = item.status.uppercase()
            holder.category.text = item.category

            // Customize badge backgrounds corresponding to states
            when (item.status.uppercase()) {
                "LIVE" -> {
                    holder.status.text = "• LIVE"
                    holder.status.setBackgroundResource(R.drawable.capsule_tag_red)
                    holder.status.setTextColor(0xFFFF3B3B.toInt())
                }
                "UPCOMING" -> {
                    holder.status.text = "UPCOMING"
                    holder.status.setBackgroundResource(R.drawable.capsule_tag_green)
                    holder.status.setTextColor(0xFF10D070.toInt())
                }
                else -> {
                    holder.status.text = item.status.uppercase()
                    holder.status.setBackgroundResource(R.drawable.circle_inactive)
                    holder.status.setTextColor(0xFFFFFFFF.toInt())
                }
            }

            // High performance Glide loading with fallback placeholders
            if (item.team1Logo.trim().isNotEmpty()) {
                Glide.with(activity)
                     .load(item.team1Logo)
                     .centerInside()
                     .placeholder(android.R.drawable.ic_menu_gallery)
                     .into(holder.logo1)
            } else {
                holder.logo1.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            if (item.team2Logo.trim().isNotEmpty()) {
                Glide.with(activity)
                     .load(item.team2Logo)
                     .centerInside()
                     .placeholder(android.R.drawable.ic_menu_gallery)
                     .into(holder.logo2)
            } else {
                holder.logo2.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            holder.btnWatch.setOnClickListener {
                activity.onMatchClicked(item)
            }
        }

        override fun getItemCount() = list.size
    }
}
