package com.example

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAspectRatio: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var playerTitleText: TextView

    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // FIT default prevents stretching

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // Hide navigation and status bars for immersive landscape view
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        btnAspectRatio = findViewById(R.id.btnAspectRatio)
        btnBack = findViewById(R.id.btnBack)
        playerTitleText = findViewById(R.id.playerTitleText)

        // Get Stream URL
        val streamUrl = intent.getStringExtra("STREAM_URL")
        if (streamUrl.isNullOrBlank()) {
            Toast.makeText(this, "Valid streaming URL not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Player
        initializePlayer(streamUrl)

        // Setup Back Button Click Listener
        btnBack.setOnClickListener { finish() }

        // Setup Aspect Ratio toggle button (Anti-Stretching toggling)
        btnAspectRatio.setOnClickListener {
            currentResizeMode = if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                Toast.makeText(this, "Zoom Mode: Aspect Ratio Maintained", Toast.LENGTH_SHORT).show()
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                Toast.makeText(this, "Fit Mode: Aspect Ratio Maintained", Toast.LENGTH_SHORT).show()
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            playerView.resizeMode = currentResizeMode
        }
    }

    private fun initializePlayer(url: String) {
        player = ExoPlayer.Builder(this).build().apply {
            playerView.player = this
            playerView.resizeMode = currentResizeMode

            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            play()

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        progressBar.visibility = View.VISIBLE
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PlayerActivity,
                        "Streaming Server offline: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
