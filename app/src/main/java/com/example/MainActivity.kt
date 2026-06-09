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
    private lateinit var requestQueue: RequestQueue

    // Managed from dynamic control api
    private var networkPopunderUrl = DEFAULT_POPUNDER_URL
    private var adsMasterSwitch = "ON"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize lists & RequestQueue
        requestQueue = Volley.newRequestQueue(this)
        binding.recyclerViewMatches.layoutManager = LinearLayoutManager(this)
        
        adapter = MatchAdapter(this, matchModelList)
        binding.recyclerViewMatches.adapter = adapter

        // Fetch dynamic configuration entries from hosting PHP script
        fetchMatchesAndSettings()
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
                    matchModelList.clear()
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
                        matchModelList.add(match)
                    }
                    adapter.notifyDataSetChanged()

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
            <body style='margin:0;padding:0;display:flex;justify-content:center;align-items:center;background:#1E293B;'>
                $htmlScript
            </body>
            </html>
        """.trimIndent()

        binding.bannerWebView.loadDataWithBaseURL(null, customHtml, "text/html", "UTF-8", null)
        binding.bannerWebView.visibility = View.VISIBLE
        binding.fallbackBannerView.visibility = View.GONE
    }

    private fun loadFallbackLocalSeededData() {
        matchModelList.clear()
        matchModelList.add(
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
        matchModelList.add(
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
        matchModelList.add(
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
        adapter.notifyDataSetChanged()
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
            setBackgroundColor(0xFF00FF66.toInt())
            setTextColor(0xFF0F172A.toInt())
        }

        dialogView.findViewById<TextView>(R.id.matchCategory)?.apply {
            text = "Loading premium streaming links. Please support sponsors!"
        }

        val btnWatchLive = dialogView.findViewById<Button>(R.id.btnWatchLive)
        btnWatchLive?.apply {
            text = "SKIP SPONSOR AD (3s)..."
            isEnabled = false
        }

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
            val btnWatch: Button = itemView.findViewById(R.id.btnWatchLive)
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
            holder.category.text = "${item.category} • ${item.timeText}"

            // Customize badge backgrounds corresponding to states
            when (item.status.uppercase()) {
                "LIVE" -> holder.status.setBackgroundColor(0xFFEF4444.toInt())
                "UPCOMING" -> holder.status.setBackgroundColor(0xFFEAB308.toInt())
                else -> holder.status.setBackgroundColor(0xFF3B82F6.toInt())
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
