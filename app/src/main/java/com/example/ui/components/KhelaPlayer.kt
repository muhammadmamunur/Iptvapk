package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.theme.DeepCharcoalGreen
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ShadowMintGreen
import com.example.ui.theme.MutedGray
import kotlinx.coroutines.delay
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.ContextWrapper

// Extension function to find Activity from Context securely
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun KhelaPlayer(
    videoUrl: String,
    title: String,
    servers: Map<String, String>,
    currentServer: String,
    onServerSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set screen orientation to horizontal (landscape) on entering player, and restore on disposal
    DisposableEffect(context) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    // Register Player Listener for real-time error and buffering status tracking
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    playerError = null
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val cause = error.cause
                playerError = if (cause is java.io.IOException || error.errorCodeName.contains("NETWORK", ignoreCase = true)) {
                    "নেটওয়ার্ক সংযোগ দুর্বল অথবা অফলাইন!"
                } else {
                    "স্ট্রীম অফলাইন অথবা লিঙ্কটি সাময়িকভাবে উপলব্ধ নেই!"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Set source whenever stream URL changes
    LaunchedEffect(videoUrl) {
        playerError = null
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Manage lifecycle of Player
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Check if player has errors before automatically playing
                    if (playerError == null) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Player States
    var showControls by remember { mutableStateOf(true) }
    var touchLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var videoResizeMode by remember { mutableStateOf(1) } // Default to 1 (RESIZE_MODE_FILL)
    var activeQuality by remember { mutableStateOf("Auto") }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Auto-hide controls timer
    LaunchedEffect(showControls, touchLocked) {
        if (showControls && !touchLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Capture play states
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            delay(500)
        }
    }

    // Outer surface
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. TOP VIEW: VIDEO VIEW CONTAINER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!touchLocked) {
                        showControls = !showControls
                    } else {
                        // Show small lock button momentarily when clicked while locked
                        showControls = true
                    }
                }
        ) {
            // Native ExoPlayer View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Force custom controls
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    // Handle resize/stretch mode
                    playerView.resizeMode = when (videoResizeMode) {
                        1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL // BoxFit.fill (Stretch)
                        2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM // BoxFit.cover (Zoom/Crop)
                        else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT // Standard Fit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Lock indicator or control states
            if (showControls) {
                // Dark Gradients representing video player bar shadows
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrush)
                )
            }

            // CONTROL GROUP LAYOUT
            if (showControls) {
                // 1. TOP HEADER BAR OF PLAYER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back from Player",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    // Top Actions (Quality & Screen Size)
                    Row {
                        // Quality indicator button
                        Box {
                            Button(
                                onClick = { if (!touchLocked) showQualityMenu = !showQualityMenu },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShadowMintGreen.copy(alpha = 0.8f),
                                    contentColor = NeonGreen
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .testTag("quality_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Quality", modifier = Modifier.size(14.dp), tint = NeonGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = activeQuality, style = MaterialTheme.typography.labelMedium)
                            }

                            // Quality Dropdown menu
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false },
                                modifier = Modifier.background(DeepCharcoalGreen)
                            ) {
                                listOf("Auto", "1080p", "720p", "480p").forEach { quality ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (activeQuality == quality) {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(24.dp))
                                                }
                                                Text(text = quality, color = Color.White)
                                            }
                                        },
                                        onClick = {
                                            activeQuality = quality
                                            showQualityMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. MIDDLE SCREEN OVERLAYS: CONTROLS & TOUCH LOCK
                // Floating Touch-Lock Button on Left Side
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            touchLocked = !touchLocked
                            if (touchLocked) showControls = false
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(ShadowMintGreen.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, NeonGreen.copy(alpha = 0.6f), CircleShape)
                            .testTag("lock_button")
                    ) {
                        Icon(
                            imageVector = if (touchLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Touch Control Lock",
                            tint = if (touchLocked) Color.Red else NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Central Play/Pause Buttons (only functional if not locked)
                if (!touchLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Rewind 10s simulation
                        IconButton(
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        // Large Play/Pause
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(NeonGreen, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DeepCharcoalGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 10s simulation
                        IconButton(
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Inline Mute Button at Bottom Left of Video overlay
                if (!touchLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier.testTag("mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute Toggle",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // If completely locked, display a tiny padlock watermark so the user knows they locked the interaction
            if (touchLocked && !showControls) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Screen Interactions Locked",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 4. Buffering status spinner overlay
            if (isBuffering && playerError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // 5. Network loss / offline custom error presentation overlay
            if (playerError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .clickable(enabled = true, onClick = {}), // intercept click
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error Icon",
                            tint = Color.Red,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = playerError!!,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    playerError = null
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = DeepCharcoalGreen
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পুনরায় চেষ্টা করুন", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = onBack,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ফিরে যান", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // 2. BOTTOM CONTROL CONSOLE: SERVER LIST & RESIZE CONTROLBAR (Permanently separate under video window)
        if (!touchLocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepCharcoalGreen)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সার্ভার সিলেকশন (Multi-Server link selector):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MutedGray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        servers.forEach { (serverName, url) ->
                            val isSelected = currentServer == serverName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonGreen else ShadowMintGreen)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGreen else NeonGreen.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onServerSelected(serverName)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = serverName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) DeepCharcoalGreen else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "স্ক্রিন রেশিও:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MutedGray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    IconButton(
                        onClick = {
                            videoResizeMode = (videoResizeMode + 1) % 3
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(ShadowMintGreen, RoundedCornerShape(8.dp))
                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("fullscreen_toggle_button")
                    ) {
                        Icon(
                            imageVector = when (videoResizeMode) {
                                1 -> Icons.Default.FullscreenExit // BoxFit.fill (Stretch to fit)
                                2 -> Icons.Default.AspectRatio // BoxFit.cover (Zoom & Crop)
                                else -> Icons.Default.Fullscreen // Fit
                            },
                            contentDescription = "Toggle Aspect-Ratio / Fill Mode",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
