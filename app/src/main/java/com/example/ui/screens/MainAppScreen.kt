package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppSettingsEntity
import com.example.data.ChannelEntity
import com.example.data.MatchEntity
import com.example.ui.components.KhelaPlayer
import com.example.ui.theme.DeepCharcoalGreen
import com.example.ui.theme.MutedGray
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ShadowMintGreen
import com.example.ui.theme.White
import com.example.ui.viewmodel.KhelaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    viewModel: KhelaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    // 1. Splash Screen States
    var isSplashComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2200) // Show stunning splash loader for 2.2 seconds
        isSplashComplete = true
    }

    // 2. Alert Telegram welcome dialog state
    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(appSettings, isSplashComplete) {
        if (isSplashComplete && appSettings?.showPopup == true) {
            showDialog = true
        }
    }

    if (!isSplashComplete) {
        SplashScreenLayout()
    } else if (appSettings?.maintenanceMode == true && currentScreen != "admin") {
        MaintenanceScreenLayout(
            onBypassAdmin = { viewModel.navigateTo("admin") }
        )
    } else {
        // Core View Holder
        Scaffold(
            bottomBar = {
                if (currentScreen == "home" || currentScreen == "categories" || currentScreen == "upcoming") {
                    KhelaBottomNavigation(
                        currentTab = currentScreen,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            },
            containerColor = DeepCharcoalGreen,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Screen transition routes
                when (currentScreen) {
                    "home" -> HomeTabScreen(viewModel)
                    "categories" -> CategoriesTabScreen(viewModel)
                    "upcoming" -> UpcomingTabScreen(viewModel)
                    "player" -> PlayerDetailScreen(viewModel)
                    "admin" -> AdminPanelScreen(viewModel)
                }

                // Telegram welcome popup modal (housed relative to app state)
                if (showDialog && appSettings != null) {
                    TelegramWelcomeDialog(
                        settings = appSettings!!,
                        onDismiss = { showDialog = false },
                        onJoin = {
                            showDialog = false
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appSettings!!.popupLink))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// A. INTRO SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreenLayout() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoalGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant glowing launcher ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = NeonGreen,
                        startAngle = rotation,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Custom Sports Vector graphic mockup inside ring
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = "Cricket Logo",
                        tint = NeonGreen,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "KHELA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Branding Label
            Text(
                text = "Khela365",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
            )

            Text(
                text = "LIVE SPORTS & IPTV PORTAL",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = NeonGreen,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // High Precision Loader
            LinearProgressIndicator(
                color = NeonGreen,
                trackColor = ShadowMintGreen,
                modifier = Modifier
                    .width(160.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

// ==========================================
// B. MAINTENANCE SCREEN OVERLAY
// ==========================================
@Composable
fun MaintenanceScreenLayout(
    onBypassAdmin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoalGreen)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = "Maintenance Icon",
                tint = NeonGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "সার্ভার রক্ষণাবেক্ষণ চলছে!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "আমাদের সিস্টেম আপডেট করা হচ্ছে। কিছুক্ষণের মধ্যে আমরা আবারও লাইভে আসছি! ধন্যবাদ আমাদের সাথে থাকার জন্য।",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBypassAdmin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShadowMintGreen,
                    contentColor = NeonGreen
                ),
                modifier = Modifier.border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Developer Control Panel")
            }
        }
    }
}

// ==========================================
// C. CUSTOM TELEGRAM ALERT WELCOME POPUP
// ==========================================
@Composable
fun TelegramWelcomeDialog(
    settings: AppSettingsEntity,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, NeonGreen, RoundedCornerShape(16.dp))
                .testTag("telegram_popup")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Telegram Icon circular badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(DeepCharcoalGreen, CircleShape)
                        .border(1.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram Send Logo",
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "গুরুত্বপূর্ণ নোটিফিকেশন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = settings.popupMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(MutedGray, MutedGray))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("popup_close_button")
                    ) {
                        Text("বন্ধ করুন")
                    }

                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DeepCharcoalGreen
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("popup_action_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Join", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("যুক্ত হোন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// D. HIGH-ACCENT BOTTOM NAVIGATION
// ==========================================
@Composable
fun KhelaBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = ShadowMintGreen,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "home",
            onClick = { onTabSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepCharcoalGreen,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = MutedGray,
                unselectedTextColor = MutedGray
            ),
            icon = { Icon(Icons.Default.SportsSoccer, contentDescription = "Live Matches") },
            label = { Text("Live Match") }
        )

        NavigationBarItem(
            selected = currentTab == "categories",
            onClick = { onTabSelected("categories") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepCharcoalGreen,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = MutedGray,
                unselectedTextColor = MutedGray
            ),
            icon = { Icon(Icons.Default.Tv, contentDescription = "IPTV Grid Categories") },
            label = { Text("Categories") }
        )

        NavigationBarItem(
            selected = currentTab == "upcoming",
            onClick = { onTabSelected("upcoming") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepCharcoalGreen,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = MutedGray,
                unselectedTextColor = MutedGray
            ),
            icon = { Icon(Icons.Default.Schedule, contentDescription = "Upcoming Games") },
            label = { Text("Upcoming") }
        )
    }
}

// ==========================================
// E. TAB SCREEN: HOME MATCHES LIST
// ==========================================
@Composable
fun HomeTabScreen(viewModel: KhelaViewModel) {
    val liveMatches by viewModel.liveMatches.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // Horizontal sport categories names mapped in raw structures
    val categories = listOf("All", "Cricket", "Football", "Others")

    Column(modifier = Modifier.fillMaxSize()) {
        // App Top Bar inside frame center-aligned symmetrically
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Far Left: Admin button
            IconButton(
                onClick = { viewModel.navigateTo("admin") },
                modifier = Modifier
                    .size(40.dp)
                    .background(ShadowMintGreen, CircleShape)
                    .align(Alignment.CenterStart)
                    .testTag("admin_panel_trigger")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Area", tint = NeonGreen)
            }

            // EXACT CENTER: Symmetrical Styled Logo and Name representation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(NeonGreen, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "365",
                            color = DeepCharcoalGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khela 365",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "আজকের লাইভ ম্যাচ লিস্ট",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen
                )
            }

            // Far Right: Notification button (or Telegram channel)
            IconButton(
                onClick = { /* Notifications placeholder */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(ShadowMintGreen, CircleShape)
                    .align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
            }
        }

        // H-Categories Selector Slider with circle icons
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Column(
                    modifier = Modifier.clickable { viewModel.selectCategory(cat) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Avatar background item with styled glow borders
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ShadowMintGreen)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                shape = CircleShape,
                                ambientColor = NeonGreen,
                                spotColor = NeonGreen
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (cat) {
                            "All" -> Icons.Default.Sports
                            "Cricket" -> Icons.Default.SportsCricket
                            "Football" -> Icons.Default.SportsSoccer
                            else -> Icons.Default.SportsKabaddi
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = cat,
                            tint = if (isSelected) NeonGreen else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

        // Main Live Match Grid Card Layout
        val filteredList = if (selectedCategory == "All") {
            liveMatches
        } else {
            liveMatches.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SportsVolleyball, contentDescription = "Blank Live Matches", tint = MutedGray, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "এই মুহূর্তে কোনো লাইভ ম্যাচ নেই!", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(text = "আসন্ন ম্যাচের জন্য Upcoming ট্যাবে চোখ রাখুন।", color = MutedGray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { match ->
                    LiveMatchGridCard(
                        match = match,
                        onCardClicked = { viewModel.selectMatchToPlay(match) }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveMatchGridCard(
    match: MatchEntity,
    onCardClicked: () -> Unit
) {
    // Pulse animation profile for standard glowing/breathing Red dot and green border
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClicked() }
            .border(
                1.5.dp,
                NeonGreen.copy(alpha = alphaAnim * 0.7f),
                RoundedCornerShape(16.dp)
            )
            .testTag("match_card_${match.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Live badge top header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(DeepCharcoalGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = match.category,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonGreen
                    )
                }

                // Breathing Red Live badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.Red.copy(alpha = alphaAnim), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Versus Panel [Team 1 Logo + Name] VS [Team 2 Logo + Name]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1 Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = match.team1Logo,
                        contentDescription = match.team1Name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DeepCharcoalGreen)
                            .padding(4.dp),
                        error = rememberVectorPainter(image = Icons.Default.Sports)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = match.team1Name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                // VS Accent Circle Indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DeepCharcoalGreen, CircleShape)
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = NeonGreen
                    )
                }

                // Team 2 Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.team2Name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AsyncImage(
                        model = match.team2Logo,
                        contentDescription = match.team2Name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DeepCharcoalGreen)
                            .padding(4.dp),
                        error = rememberVectorPainter(image = Icons.Default.Sports)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer of Match Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(0.7f)
                )

                // Stream play action button launcher
                Button(
                    onClick = { onCardClicked() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DeepCharcoalGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Match Stream", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("খেলুন", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ==========================================
// F. TAB SCREEN: CATEGORIES GRID (IPTV SECTOR)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesTabScreen(viewModel: KhelaViewModel) {
    val allChannels by viewModel.allChannels.collectAsState()
    val channelCategories by viewModel.channelCategories.collectAsState()

    var activeCategorySelection by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Main Grid Top Bar Header Center-aligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeCategorySelection != null) {
                IconButton(
                    onClick = { activeCategorySelection = null },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = activeCategorySelection ?: "চ্যানেল ক্যাটাগরি",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (activeCategorySelection == null) "ক্লিক করে আইপিটিভি চ্যানেল লিস্ট দেখুন" else "লাইভ টিভি চ্যানেলগুলো সিলেক্ট করুন",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

        if (activeCategorySelection == null) {
            // Display Grid of Categories Rounded box cards (Categories view: GridView)
            val availableCategories = listOf("Bangladesh TV", "Sports Channels", "Movies TV")
            val categoriesFromDb = channelCategories.ifEmpty { availableCategories }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoriesFromDb) { category ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = NeonGreen)
                            .clickable {
                                activeCategorySelection = category
                            }
                            .testTag("category_grid_$category")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val catIcon = when {
                                    category.contains("Bangla", true) -> Icons.Default.Tv
                                    category.contains("Sport", true) -> Icons.Default.SportsCricket
                                    category.contains("Movie", true) -> Icons.Default.Movie
                                    else -> Icons.Default.LiveTv
                                }
                                Icon(catIcon, contentDescription = category, tint = NeonGreen, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Display Subcategory / Channels List under active selection
            val filteredChannels = allChannels.filter { it.categoryName == activeCategorySelection }

            if (filteredChannels.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("এই ক্যাটাগরিতে কোনো চ্যানেল যুক্ত করা হয়নি!", color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredChannels) { channel ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectChannelToPlay(channel) }
                                .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .testTag("channel_card_${channel.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DeepCharcoalGreen)
                                        .padding(4.dp),
                                    error = rememberVectorPainter(image = Icons.Default.LiveTv)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// G. TAB SCREEN: UPCOMING MATCHES & HIGHLIGHTS
// ==========================================
@Composable
fun UpcomingTabScreen(viewModel: KhelaViewModel) {
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val highlightMatches by viewModel.highlightMatches.collectAsState()

    var activeSubTab by remember { mutableStateOf("upcoming") } // upcoming, highlights

    Column(modifier = Modifier.fillMaxSize()) {
        // Screen header Center-aligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "সময়সূচী ও হাইলাইটস",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "আসন্ন খেলা এবং হাইলাইটস রিফ্রেশ করুন",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Subtab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(ShadowMintGreen, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            listOf("upcoming" to "আসন্ন ম্যাচসমূহ", "highlights" to "হাইলাইটস রিডি").forEach { (tabId, labelName) ->
                val isActive = activeSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) NeonGreen else Color.Transparent)
                        .clickable { activeSubTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) DeepCharcoalGreen else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeSubTab == "upcoming") {
            if (upcomingMatches.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("এই মুহূর্তে কোনো আসন্ন ম্যাচ সিডিউল করা নেই!", color = Color.White)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(upcomingMatches) { match ->
                        UpcomingMatchCard(match)
                    }
                }
            }
        } else {
            if (highlightMatches.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("কোনো হাইলাইটস ভিডিও লিঙ্ক যুক্ত করা হয়নি!", color = Color.White)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(highlightMatches) { match ->
                        HighlightCard(
                            match = match,
                            onPlay = { viewModel.selectMatchToPlay(match) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingMatchCard(match: MatchEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(DeepCharcoalGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = match.category, color = NeonGreen, style = MaterialTheme.typography.labelSmall)
                }

                // Notification bell alarm toggle simulation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = "Reminder Set", tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিমাইন্ডার সেট করা আছে", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams Versus Representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.0f)) {
                    AsyncImage(
                        model = match.team1Logo,
                        contentDescription = match.team1Name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepCharcoalGreen)
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = match.team1Name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Text(text = "VS", color = NeonGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))

                // Team 2
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1.0f)) {
                    Text(text = match.team2Name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = match.team2Logo,
                        contentDescription = match.team2Name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepCharcoalGreen)
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = match.title, style = MaterialTheme.typography.bodySmall, color = MutedGray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.7f))
                Text(text = match.timeText, style = MaterialTheme.typography.labelMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HighlightCard(
    match: MatchEntity,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ShadowMintGreen),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .border(1.dp, NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(DeepCharcoalGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "FULL REPLAY", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.PlayCircle, contentDescription = "Ready to Play", tint = NeonGreen)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = match.title + " - Highlights Video",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = match.timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray
            )
        }
    }
}

// ==========================================
// H. PLAYBACK DETAIL & STREAM GUIDE SCREEN LAYOUT
// ==========================================
@Composable
fun PlayerDetailScreen(viewModel: KhelaViewModel) {
    // Current match and details details
    val activeMatch = viewModel.activePlaybackMatch
    val activeChannel = viewModel.activePlaybackChannel
    val activeTitle = activeMatch?.title ?: activeChannel?.name ?: "Streaming Channel"

    var selectedServer by remember { mutableStateOf(viewModel.selectedServerName) }
    val playUrl = viewModel.getActivePlaybackUrl()
    val servers = viewModel.getActiveServers()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Player Container top element
        KhelaPlayer(
            videoUrl = playUrl,
            title = activeTitle,
            servers = servers,
            currentServer = selectedServer,
            onServerSelected = { server ->
                selectedServer = server
                viewModel.selectedServerName = server
            },
            onBack = { viewModel.navigateTo("home") },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ==========================================
// I. FULL ADMIN SECURITY PANEL SCREEN (CRUD)
// ==========================================
@Composable
fun AdminPanelScreen(viewModel: KhelaViewModel) {
    val context = LocalContext.current
    val appSettings by viewModel.appSettings.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()

    // Authentication gates States
    val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsState()
    val failedAttempts by viewModel.failedAttempts.collectAsState()
    val cooldownActiveUntil by viewModel.cooldownActiveUntil.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var usernameInputField by remember { mutableStateOf("") }
    var passwordInputField by remember { mutableStateOf("") }
    var loginErrorText by remember { mutableStateOf<String?>(null) }

    // Enforce check session automatic logout when screen opens
    LaunchedEffect(Unit) {
        viewModel.checkSessionTimeout()
    }

    if (!isAdminAuthenticated) {
        // Secure Admin authentications UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoalGreen)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val curTime = System.currentTimeMillis()
            val isLockedOut = cooldownActiveUntil > curTime
            val remainingCooldownSeconds = if (isLockedOut) ((cooldownActiveUntil - curTime) / 1000).toInt() + 1 else 0

            // Auto refresh lockout timers in Compose loop
            if (isLockedOut) {
                LaunchedEffect(key1 = curTime) {
                    kotlinx.coroutines.delay(1000)
                    // trigger recomposition
                    passwordInputField = passwordInputField
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(ShadowMintGreen, RoundedCornerShape(20.dp))
                    .border(1.5.dp, if (isLockedOut) Color.Red else NeonGreen, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(DeepCharcoalGreen, CircleShape)
                        .border(1.5.dp, if (isLockedOut) Color.Red else NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLockedOut) Icons.Default.Warning else Icons.Default.Security,
                        contentDescription = "Lock Secure",
                        tint = if (isLockedOut) Color.Red else NeonGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "অ্যাডমিন সিকিউরিটি গেটওয়ে",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Khela 365 Control Shield",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isLockedOut) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ক্রমাগত ৫ বার ভুল পাসওয়ার্ড দেওয়ার কারণে আপনার আইপি সাময়িক ব্লক করা হয়েছে!",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "পুনরায় চেষ্টা করুন: $remainingCooldownSeconds সেকেন্ড পর",
                                color = Color.Red,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Completely clean administrative Username text field
                OutlinedTextField(
                    value = usernameInputField,
                    onValueChange = {
                        usernameInputField = it
                        loginErrorText = null
                    },
                    label = { Text("এডমিন ইউজারনেম") },
                    singleLine = true,
                    enabled = !isLockedOut,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DeepCharcoalGreen,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("admin_login_username_field")
                )

                // Safe and clean password input field (fully blank without leakages)
                OutlinedTextField(
                    value = passwordInputField,
                    onValueChange = { 
                        passwordInputField = it
                        loginErrorText = null
                    },
                    label = { Text("মাস্টার সিক্রেট পাসওয়ার্ড") },
                    singleLine = true,
                    enabled = !isLockedOut,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(image, contentDescription = "Toggle password visibility", tint = MutedGray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DeepCharcoalGreen,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("admin_login_password_field")
                )

                if (loginErrorText != null) {
                    Text(
                        text = loginErrorText ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepCharcoalGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Verified", tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHA-256 এনক্রিপ্টেড পাসওয়ার্ড প্রটেকশন", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassEmpty, "Timeout", tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("১০০% নিরাপদ সেশন (২ ঘণ্টা পর অটোমেটিক লগআউট)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, "Lockout", tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ব্রুট ফোর্স প্রোটেকশন চালু (৫ বার ট্রাই ব্লক)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, "Encrypt", tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্ট্রিমিং URL Obfuscation ও ডাটাবেজ প্রটেকশন", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo("home") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাহির হোন", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (usernameInputField.isBlank()) {
                                loginErrorText = "ইউজারনেম ইনপুট দিন!"
                            } else if (passwordInputField.isBlank()) {
                                loginErrorText = "পাসওয়ার্ড ইনপুট দিন!"
                            } else {
                                val success = viewModel.loginAdmin(usernameInputField, passwordInputField)
                                if (success) {
                                    usernameInputField = ""
                                    passwordInputField = ""
                                    loginErrorText = null
                                } else {
                                    val now = System.currentTimeMillis()
                                    if (viewModel.cooldownActiveUntil.value > now) {
                                        loginErrorText = "লগইন লক ডাউনলোড! ১ মিনিট অপেক্ষা করুন।"
                                    } else {
                                        val remaining = 5 - failedAttempts
                                        loginErrorText = "ভুল ইউজারনেম অথবা পাসওয়ার্ড! আর $remaining বার সুযোগ আছে।"
                                    }
                                }
                            }
                        },
                        enabled = !isLockedOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DeepCharcoalGreen
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("admin_login_submit_btn")
                    ) {
                        Text("লগইন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Render Administrative panel contents if validated successfully
        AdminPanelAndForms(viewModel, context, appSettings, allMatches, allChannels)
    }
}

@Composable
fun AdminPanelAndForms(
    viewModel: KhelaViewModel,
    context: android.content.Context,
    appSettings: AppSettingsEntity?,
    allMatches: List<MatchEntity>,
    allChannels: List<ChannelEntity>
) {
    // Tabs for Admin
    var activeAdminTab by remember { mutableStateOf("settings") } // settings, matches, channels

    // Forms states
    var popupMsgInput by remember { mutableStateOf("") }
    var popupUrlInput by remember { mutableStateOf("") }
    var popupShowToggle by remember { mutableStateOf(true) }
    var maintenanceToggle by remember { mutableStateOf(false) }

    // Password change state
    var newAdminPasswordInput by remember { mutableStateOf("") }

    // Init form with settings values
    LaunchedEffect(appSettings) {
        appSettings?.let {
            popupMsgInput = it.popupMessage
            popupUrlInput = it.popupLink
            popupShowToggle = it.showPopup
            maintenanceToggle = it.maintenanceMode
        }
    }

    // Match form states
    var matchTitle by remember { mutableStateOf("") }
    var t1Name by remember { mutableStateOf("") }
    var t1Logo by remember { mutableStateOf("") }
    var t2Name by remember { mutableStateOf("") }
    var t2Logo by remember { mutableStateOf("") }
    var matchCat by remember { mutableStateOf("Cricket") }
    var matchStatus by remember { mutableStateOf("live") } // live, upcoming, highlight
    var matchTimeText by remember { mutableStateOf("Live Now") }
    var server1 by remember { mutableStateOf("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8") }
    var server2 by remember { mutableStateOf("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8") }
    var serverHindi by remember { mutableStateOf("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8") }

    // Channel Form states
    var chanName by remember { mutableStateOf("") }
    var chanLogo by remember { mutableStateOf("") }
    var chanUrl by remember { mutableStateOf("") }
    var chanCatName by remember { mutableStateOf("Bangladesh TV") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoalGreen)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("home") }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "প্যানেল নিয়ন্ত্রণ (Security Dashboard)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .background(Color.Red, CircleShape)
                    .clickable { viewModel.navigateTo("home") }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("প্যানেল থেকে বের হোন", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal navigation tabs for Admin controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            listOf("settings" to "অ্যাপ সেটিংস", "matches" to "লাইভ ম্যাচ যোগ", "channels" to "সার্ভার আইপিটিভি").forEach { (tabId, tabName) ->
                val isActive = activeAdminTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeAdminTab = tabId }
                        .border(
                            1.dp,
                            if (isActive) NeonGreen else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .background(if (isActive) ShadowMintGreen else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) NeonGreen else Color.White
                    )
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(16.dp))

        // Main Config panels
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeAdminTab == "settings") {
                item {
                    Text("অ্যাপ কনফিগারেশন সেটিংস", style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                item {
                    // Custom settings fields
                    OutlinedTextField(
                        value = popupMsgInput,
                        onValueChange = { popupMsgInput = it },
                        label = { Text("পপ-আপ নোটিশ বার্তা (Notice Popup msg)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = ShadowMintGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_popup_msg_field")
                    )
                }
                item {
                    OutlinedTextField(
                        value = popupUrlInput,
                        onValueChange = { popupUrlInput = it },
                        label = { Text("টেলিগ্রাম বা পপ-আপ লিংক (Notice hyperlink URL)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = ShadowMintGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_popup_link_field")
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ওয়েলকাম পপ-আপ দেখান (Show Dialog Welcome)", color = Color.White)
                        Switch(
                            checked = popupShowToggle,
                            onCheckedChange = { popupShowToggle = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মেইনটেনেন্স মোড (Maintenance block Mode)", color = Color.White)
                            Text("চলতি অ্যাপ লক করতে এটি চালু করুন", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                        Switch(
                            checked = maintenanceToggle,
                            onCheckedChange = { maintenanceToggle = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = newAdminPasswordInput,
                        onValueChange = { newAdminPasswordInput = it },
                        label = { Text("নতুন অ্যাডমিন পাসওয়ার্ড পরিবর্তন করুন (New Sec-Pass plain)") },
                        placeholder = { Text("পাসওয়ার্ড অপরিবর্তিত রাখতে এটি খালি রাখুন") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = ShadowMintGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_new_password_field")
                    )
                }
                item {
                    Button(
                        onClick = {
                            viewModel.saveAppSettings(
                                popupMessage = popupMsgInput,
                                popupLink = popupUrlInput,
                                showPopup = popupShowToggle,
                                maintenanceMode = maintenanceToggle,
                                newPasswordPlain = newAdminPasswordInput
                            )
                            newAdminPasswordInput = ""
                            // Show success visual log
                            viewModel.navigateTo("home")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepCharcoalGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_save_settings_btn")
                    ) {
                        Text("সেটিংস ও নতুন পাসওয়ার্ড সেভ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (activeAdminTab == "matches") {
                item {
                    Text("অনলাইন লাইভ ম্যাচ ক্রিয়েটর", style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                item {
                    OutlinedTextField(
                        value = matchTitle,
                        onValueChange = { matchTitle = it },
                        label = { Text("ম্যাচের মূল বিবরণ (যেমন BAN vs PAK T20)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = t1Name,
                            onValueChange = { t1Name = it },
                            label = { Text("টিম ১ নাম") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = t2Name,
                            onValueChange = { t2Name = it },
                            label = { Text("টিম ২ নাম") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = t1Logo,
                            onValueChange = { t1Logo = it },
                            label = { Text("টিম ১ লোগো চিত্র URL") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = t2Logo,
                            onValueChange = { t2Logo = it },
                            label = { Text("টিম ২ লোগো চিত্র URL") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = matchCat,
                            onValueChange = { matchCat = it },
                            label = { Text("ক্যাটাগরি (Cricket, Football)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = matchStatus,
                            onValueChange = { matchStatus = it },
                            label = { Text("স্ট্যাটাস (live, upcoming, highlight)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = matchTimeText,
                        onValueChange = { matchTimeText = it },
                        label = { Text("ম্যাচের সময় বা কাউন্টডাউন বাক্য (Countdown/Live stats)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = server1,
                        onValueChange = { server1 = it },
                        label = { Text("সার্ভার ১ M3U8 Url (Server 1 link)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = server2,
                        onValueChange = { server2 = it },
                        label = { Text("সার্ভার ২ M3U8 Url (Server 2 link)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = serverHindi,
                        onValueChange = { serverHindi = it },
                        label = { Text("সার্ভার হিন্দি M3U8 Url (Hindi audio link)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (matchTitle.isNotBlank() && t1Name.isNotBlank() && t2Name.isNotBlank()) {
                                viewModel.addLiveMatch(
                                    title = matchTitle,
                                    team1Name = t1Name,
                                    team1Logo = t1Logo.ifBlank { "https://flagcdn.com/w160/bd.png" },
                                    team2Name = t2Name,
                                    team2Logo = t2Logo.ifBlank { "https://flagcdn.com/w160/gb.png" },
                                    category = matchCat,
                                    status = matchStatus,
                                    timeText = matchTimeText,
                                    srv1 = server1,
                                    srv2 = server2,
                                    srvHindi = serverHindi
                                )
                                // Clear
                                matchTitle = ""
                                t1Name = ""
                                t2Name = ""
                                viewModel.navigateTo("home")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepCharcoalGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("নতুন লাইভ ম্যাচ পাবলিশ করুন", fontWeight = FontWeight.Bold)
                    }
                }

                // Delete Match section
                item {
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    Text("বিদ্যমান ম্যাচ ডিলিট করুন (Active matches CRUD)", style = MaterialTheme.typography.titleMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                }

                items(allMatches) { match ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ShadowMintGreen, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = match.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (match.status == "live") Color.Red else MutedGray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${match.team1Name} vs ${match.team2Name} [${match.status.uppercase()}]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.deleteMatch(match.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("delete_match_btn_${match.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighlightOff,
                                contentDescription = "Finish Match",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Finish", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                item {
                    Text("অনলাইন সার্ভার আইপিটিভি টিভি চ্যানেল এডিটর", style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                item {
                    OutlinedTextField(
                        value = chanName,
                        onValueChange = { chanName = it },
                        label = { Text("টিভি চ্যানেলের নাম (যেমন: GTV Live, Willow TV)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = chanLogo,
                        onValueChange = { chanLogo = it },
                        label = { Text("চ্যানেল লোগো চিত্র URL") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = chanUrl,
                        onValueChange = { chanUrl = it },
                        label = { Text("চ্যানেল স্ট্রিমিং লিঙ্ক (HLS .m3u8 link)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = chanCatName,
                        onValueChange = { chanCatName = it },
                        label = { Text("টিভি সাব-ক্যাটাগরি (e.g. Bangladesh TV, Sports Channels)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (chanName.isNotBlank() && chanUrl.isNotBlank()) {
                                viewModel.addIptvChannel(
                                    name = chanName,
                                    logo = chanLogo,
                                    streamUrl = chanUrl,
                                    categoryName = chanCatName
                                )
                                chanName = ""
                                chanUrl = ""
                                activeAdminTab = "channels"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepCharcoalGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("আইপিটিভি চ্যানেলটি সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }

                // Delete channel lists
                item {
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    Text("বিদ্যমান টিভি চ্যানেল ডিলিট করুন", style = MaterialTheme.typography.titleMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                }

                items(allChannels) { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ShadowMintGreen, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${channel.name} [${channel.categoryName}]", color = Color.White)
                        IconButton(
                            onClick = { viewModel.deleteChannel(channel.id) },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Channel", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
