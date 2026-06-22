package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.HydrationLog
import com.example.ui.LanguageManager
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val activeLang = viewModel.currentLanguage

    // Active screen navigation index
    var selectedTabIndex by remember { mutableStateOf(0) }

    // State bindings
    val dailyStepLog by viewModel.dailyStepLog.collectAsState()
    val hydrationLogs by viewModel.hydrationLogs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Permissions Request handler
    var hasPhysicalActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPhysicalActivityPermission = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: hasPhysicalActivityPermission
        }
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
    }

    LaunchedEffect(Unit) {
        val permissionsNeeded = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            launcher.launch(permissionsNeeded.toTypedArray())
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateDark,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Filled.DirectionsRun, contentDescription = "Summary") },
                    label = { Text(LanguageManager.getString("steps", activeLang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateDark,
                        selectedTextColor = LightTealAccent,
                        indicatorColor = LightTealAccent,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Filled.WaterDrop, contentDescription = "Hydration") },
                    label = { Text(LanguageManager.getString("hydration_history", activeLang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateDark,
                        selectedTextColor = LightBlueAccent,
                        indicatorColor = LightBlueAccent,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Profile") },
                    label = { Text(LanguageManager.getString("switch_account", activeLang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateDark,
                        selectedTextColor = EnergyPink,
                        indicatorColor = EnergyPink,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateDark)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LanguageManager.getString("app_title", activeLang),
                        color = SmoothWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeLang == LanguageManager.Language.TR) "Yerel Profil Aktif" else "Local Profile Active",
                        color = LightTealAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Language Selector Badge
                IconButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SlateCard)
                ) {
                    Text(
                        text = if (activeLang == LanguageManager.Language.TR) "EN" else "TR",
                        color = LightTealAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> DashboardTab(viewModel, dailyStepLog, userProfile)
                    1 -> HydrationTab(viewModel, hydrationLogs, userProfile)
                    2 -> ProfileTab(viewModel, userProfile)
                }
            }
        }
    }
}

/**
 * 1. DASHBOARD TAB ("Özet")
 */
@Composable
fun DashboardTab(
    viewModel: MainViewModel,
    stepLog: com.example.data.DailyStepsLog?,
    profile: com.example.data.UserProfile?
) {
    val activeLang = viewModel.currentLanguage
    val steps = stepLog?.steps ?: 0
    val target = stepLog?.stepTarget ?: 8000
    val progress = if (target > 0) (steps.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val distanceKm = String.format(Locale.US, "%.2f", (stepLog?.distanceMeters ?: 0) / 1000f)
    val calories = stepLog?.caloriesBurnt ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image configuration showing athlete banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
            ) {
                // Use the generated 16:9 hero image safely
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1782141761969),
                    contentDescription = "Healthy Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Linear gradient fade effect over the banner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                Text(
                    text = LanguageManager.getString("anti_cheat_active", activeLang),
                    color = LightTealAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Circular Step Progress
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.handleManualStepSimulation()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Track Arc
                        Canvas(modifier = Modifier.size(170.dp)) {
                            drawCircle(
                                color = SlateDark,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 16.dp.toPx())
                            )
                        }
                        // Progress Arc
                        Canvas(modifier = Modifier.size(170.dp)) {
                            drawArc(
                                color = TealAccent,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx())
                            )
                        }
                        
                        // Centered step stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = LightTealAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = steps.toString(),
                                color = SmoothWhite,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "/ $target",
                                color = SoftGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row showing Kilometers & Calories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$distanceKm km",
                                color = SmoothWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = LanguageManager.getString("distance", activeLang),
                                color = SoftGray,
                                fontSize = 12.sp
                            )
                        }
                        Divider(modifier = Modifier.height(30.dp).width(1.dp), color = SoftGray.copy(alpha = 0.3f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$calories kcal",
                                color = SmoothWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = LanguageManager.getString("calories", activeLang),
                                color = SoftGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // AI Coach Advice
        item {
            AICoachCard(steps = steps, target = target, lang = activeLang)
        }

        // Long Distance GPS Mode Tracker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "GPS",
                                tint = LightBlueAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LanguageManager.getString("gps_mode", activeLang),
                                color = SmoothWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = viewModel.gpsModeEnabled,
                            onCheckedChange = { viewModel.updateGpsMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark,
                                checkedTrackColor = LightBlueAccent
                            )
                        )
                    }

                    if (viewModel.gpsModeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val statusKey = if (viewModel.gpsSignalActive) "gps_enabled" else "gps_signal_loss"
                                Text(
                                    text = LanguageManager.getString(statusKey, activeLang),
                                    color = if (viewModel.gpsSignalActive) LightTealAccent else EnergyPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${LanguageManager.getString("active_speed", activeLang)}: ${if (viewModel.gpsSignalActive) String.format("%.1f", viewModel.currentSpeedKmh) else "0.0"} km/h",
                                    color = SmoothWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Mock signal toggle
                            Button(
                                onClick = { viewModel.toggleGpsSignal() },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (viewModel.gpsSignalActive) "Simulate Signal Lock" else "Reconnect",
                                    fontSize = 10.sp,
                                    color = LightBlueAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Space padding
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 2. HYDRATION MANAGER TAB ("Sıvı")
 */
@Composable
fun HydrationTab(
    viewModel: MainViewModel,
    logs: List<HydrationLog>,
    profile: com.example.data.UserProfile?
) {
    val activeLang = viewModel.currentLanguage
    val target = profile?.waterTargetMl ?: 2000
    val totalNet = logs.sumOf { it.netHydrationMl }
    val progress = if (target > 0) (totalNet.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f

    var showLogDialog by remember { mutableStateOf(false) }
    var selectedBeverage by remember { mutableStateOf("Water") }
    var customVolumeMl by remember { mutableStateOf("250") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visual hydration indicator with Dynamic Sine-wave filling water Glass effect!
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageManager.getString("hydration", activeLang),
                        color = SmoothWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sine-wave cup Canvas
                    Box(
                        modifier = Modifier.size(140.dp, 180.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Drawing the cup limits
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Cup boundary stroke
                            val path = Path().apply {
                                moveTo(20f, 10f)
                                lineTo(10f, size.height - 10f)
                                lineTo(size.width - 10f, size.height - 10f)
                                lineTo(size.width - 20f, 10f)
                            }
                            drawPath(path, color = SoftGray, style = Stroke(width = 3.dp.toPx()))

                            // Draw liquid content safely
                            val fillHeight = (size.height - 20f) * progress
                            if (fillHeight > 0f) {
                                val wavePath = Path().apply {
                                    val startY = size.height - 10f - fillHeight
                                    moveTo(15f, size.height - 10f)
                                    lineTo(10f + (15f / size.height) * fillHeight, startY)
                                    
                                    // Sine curve calculations for moving liquid
                                    for (x in 20 until size.width.toInt() - 20) {
                                        val wave = sin((x / 14f) + (System.currentTimeMillis() / 250f)) * 6f
                                        lineTo(x.toFloat(), startY + wave.toFloat())
                                    }
                                    
                                    lineTo(size.width - 12f, size.height - 10f)
                                    close()
                                }
                                drawPath(wavePath, color = HydrationBlue.copy(alpha = 0.8f))
                            }
                        }

                        // Text overlay displaying percent representation
                        Column(
                            modifier = Modifier.padding(bottom = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${totalNet} ml",
                                color = SmoothWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/ ${target} ml",
                                color = SmoothWhite.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Fast Log action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.logBeverage("Water", 200) },
                            colors = ButtonDefaults.buttonColors(containerColor = HydrationBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+200ml ${LanguageManager.getString("water", activeLang)}")
                        }
                        Button(
                            onClick = { showLogDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LightTealAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = SlateDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(LanguageManager.getString("add_drink", activeLang), color = SlateDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Hydration logs detailed dynamic histories
        item {
            Text(
                text = LanguageManager.getString("hydration_history", activeLang),
                color = SmoothWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Henüz sıvı kaydı eklenmedi.",
                        color = SoftGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (log.liquidType) {
                                "Water" -> Icons.Filled.WaterDrop
                                "Coffee" -> Icons.Filled.Coffee
                                "Tea" -> Icons.Filled.EmojiFoodBeverage
                                else -> Icons.Filled.SportsMartialArts
                            }
                            val color = if (log.liquidType == "Coffee") EnergyPink else LightBlueAccent

                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val translatedType = when (log.liquidType) {
                                    "Water" -> LanguageManager.getString("water", activeLang)
                                    "Coffee" -> "Kahve"
                                    "Tea" -> "Çay"
                                    "Soda" -> "Gazlı İçecek"
                                    "Sports" -> "Sporcu İçeceği"
                                    else -> log.liquidType
                                }
                                Text(
                                    text = "$translatedType - ${log.amountMl} ml",
                                    color = SmoothWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Net: ${log.netHydrationMl} ml (Katsayı: ${log.hydrationCoefficient})",
                                    color = if (log.netHydrationMl >= 0) LightTealAccent else EnergyPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteHydration(log) }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = EnergyPink)
                        }
                    }
                }
            }
        }

        // Space padding
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal dialog trigger for dynamic cup selection and coefficients config
    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text(LanguageManager.getString("add_drink", activeLang), color = SmoothWhite) },
            containerColor = SlateCard,
            confirmButton = {
                Button(
                    onClick = {
                        val vol = customVolumeMl.toIntOrNull() ?: 250
                        viewModel.logBeverage(selectedBeverage, vol)
                        showLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightTealAccent)
                ) {
                    Text(LanguageManager.getString("save", activeLang), color = SlateDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text(LanguageManager.getString("cancel", activeLang), color = SoftGray)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Drink dropdown list selectors
                    Text(LanguageManager.getString("drink_type", activeLang) + ":", color = SmoothWhite, fontSize = 14.sp)
                    val beverageTypes = listOf("Water", "Coffee", "Tea", "Soda", "Sports")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        beverageTypes.forEach { type ->
                            val isSel = selectedBeverage == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) LightBlueAccent else Color.Transparent)
                                    .clickable { selectedBeverage = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (type) {
                                        "Water" -> "Su"
                                        "Coffee" -> "Kahve"
                                        "Tea" -> "Çay"
                                        "Soda" -> "Soda"
                                        "Sports" -> "Spor"
                                        else -> type
                                    },
                                    color = if (isSel) SlateDark else SmoothWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Cup preset selectors
                    Text(LanguageManager.getString("cup_size", activeLang) + ":", color = SmoothWhite, fontSize = 14.sp)
                    val presets = listOf("200", "330", "500")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { cap ->
                            Button(
                                onClick = { customVolumeMl = cap },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (customVolumeMl == cap) LightTealAccent else SlateDark
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "$cap ml", color = if (customVolumeMl == cap) SlateDark else SmoothWhite)
                            }
                        }
                    }

                    // Manual Input field
                    OutlinedTextField(
                        value = customVolumeMl,
                        onValueChange = { customVolumeMl = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Miktar (ml)", color = SoftGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTealAccent,
                            unfocusedBorderColor = SoftGray,
                            focusedTextColor = SmoothWhite,
                            unfocusedTextColor = SmoothWhite
                        )
                    )
                }
            }
        )
    }
}

/**
 * 3. PROFILE / LOGIN / HEALTH METRIC CONFIG TAB
 */
@Composable
fun ProfileTab(
    viewModel: MainViewModel,
    profile: com.example.data.UserProfile?
) {
    val activeLang = viewModel.currentLanguage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clean profile header card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(LightTealAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.name?.take(1) ?: "G").uppercase(),
                            color = SlateDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = profile?.name ?: (if (activeLang == LanguageManager.Language.TR) "Misafir Kullanıcı" else "Guest User"),
                            color = SmoothWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeLang == LanguageManager.Language.TR) "Yerel Profil (Çevrimdışı/Güvenli)" else "Local Profile (Offline/Secure)",
                            color = SoftGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Metric Settings with Secure AES encryption and validation saves
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = LightTealAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sağlık Bilgilerini Düzenle (Secure E2E)",
                            color = SmoothWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Height Meter input
                    OutlinedTextField(
                        value = viewModel.heightInput,
                        onValueChange = { viewModel.heightInput = it },
                        label = { Text(LanguageManager.getString("height", activeLang), color = SoftGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SmoothWhite,
                            unfocusedTextColor = SmoothWhite,
                            focusedBorderColor = LightTealAccent,
                            unfocusedBorderColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Weight Input
                    OutlinedTextField(
                        value = viewModel.weightInput,
                        onValueChange = { viewModel.weightInput = it },
                        label = { Text(LanguageManager.getString("weight", activeLang), color = SoftGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SmoothWhite,
                            unfocusedTextColor = SmoothWhite,
                            focusedBorderColor = LightTealAccent,
                            unfocusedBorderColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Daily Step target limit
                    OutlinedTextField(
                        value = viewModel.stepGoalInput,
                        onValueChange = { viewModel.stepGoalInput = it },
                        label = { Text(LanguageManager.getString("step_goal", activeLang), color = SoftGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SmoothWhite,
                            unfocusedTextColor = SmoothWhite,
                            focusedBorderColor = LightTealAccent,
                            unfocusedBorderColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Daily water volume target
                    OutlinedTextField(
                        value = viewModel.waterGoalInput,
                        onValueChange = { viewModel.waterGoalInput = it },
                        label = { Text(LanguageManager.getString("hydration_goal", activeLang), color = SoftGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SmoothWhite,
                            unfocusedTextColor = SmoothWhite,
                            focusedBorderColor = LightTealAccent,
                            unfocusedBorderColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.updateProfileSettings() },
                        colors = ButtonDefaults.buttonColors(containerColor = LightTealAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = LanguageManager.getString("update_profile", activeLang),
                            color = SlateDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Security declaration badge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = "Secure", tint = LightTealAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = LanguageManager.getString("health_data_secure", activeLang),
                        color = SoftGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Night DND reminder alert
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.NotificationsOff, contentDescription = "DND", tint = EnergyPink)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = LanguageManager.getString("notification_dnd_active", activeLang),
                        color = SoftGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Space padding
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

data class AICoachTip(
    val containerColor: Color,
    val borderColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val emoji: String,
    val title: String,
    val text: String
)

@Composable
fun AICoachCard(steps: Int, target: Int, lang: LanguageManager.Language) {
    val progress = if (target > 0) steps.toFloat() / target else 0f
    
    val tip = when {
        steps < 1500 -> {
            AICoachTip(
                containerColor = EnergyPink.copy(alpha = 0.08f),
                borderColor = EnergyPink.copy(alpha = 0.4f),
                badgeBg = EnergyPink,
                badgeText = SmoothWhite,
                emoji = "⚠️",
                title = if (lang == LanguageManager.Language.TR) "Hareketsiz Kaldınız!" else "Sedentary Alert!",
                text = if (lang == LanguageManager.Language.TR) 
                    "Bugün sadece $steps adım attınız. Vücudunuzu uyandırmak ve metabolizmanızı canlandırmak için kalkıp 5 dakika yürümeye ne dersiniz?" 
                    else "You've only taken $steps steps so far. Why not stand up and walk for 5 minutes to wake up your body?"
            )
        }
        progress < 0.6f -> {
            AICoachTip(
                containerColor = LightBlueAccent.copy(alpha = 0.08f),
                borderColor = LightBlueAccent.copy(alpha = 0.4f),
                badgeBg = LightBlueAccent,
                badgeText = SlateDark,
                emoji = "⚡",
                title = if (lang == LanguageManager.Language.TR) "Devam Edin!" else "Keep Moving!",
                text = if (lang == LanguageManager.Language.TR) 
                    "Şu an $steps adımdasınız. Güne güzel bir aktiflik kattınız, hedefe doğru kararlı adımlarla ilerliyorsunuz!" 
                    else "Currently at $steps steps. You have added a nice touch of activity to your day, keep marching toward your target!"
            )
        }
        else -> {
            AICoachTip(
                containerColor = LightTealAccent.copy(alpha = 0.08f),
                borderColor = LightTealAccent.copy(alpha = 0.5f),
                badgeBg = LightTealAccent,
                badgeText = SlateDark,
                emoji = "🔥",
                title = if (lang == LanguageManager.Language.TR) "Süper Aktif Performans!" else "Super Active Performance!",
                text = if (lang == LanguageManager.Language.TR) 
                    "Harika! $steps adıma ulaştınız. Bugün bedeninize muhteşem bir iyilik yaptınız!" 
                    else "Spectacular! You reached $steps steps. You've done an incredible favor for your body and health today!"
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = tip.containerColor),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tip.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(tip.badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "HEALTH AI",
                            color = tip.badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip.title,
                        color = SmoothWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = tip.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tip.text,
                color = SmoothWhite.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


