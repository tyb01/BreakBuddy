package com.pawsup.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pawsup.R
import com.pawsup.cats.CatRegistry
import com.pawsup.cats.CatAssetResolver
import com.pawsup.break_experience.components.VideoPlayerCompositor
import com.pawsup.data.UserPreferences
import com.pawsup.monitoring.MonitoringService
import com.pawsup.settings.SettingsActivity
import com.pawsup.ui.CatPosterImage
import com.pawsup.ui.theme.CrimsonTextFamily
import com.pawsup.ui.theme.PawsUpTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var catRegistry: CatRegistry

    private var currentStep by mutableIntStateOf(0)

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* state refreshed via ON_RESUME observer */ }

    // Launched after MeetMiso plays — user picks apps, then we go to Settings.
    private val appPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val misoCat = catRegistry.free()
        val act = this

        setContent {
            PawsUpTheme {
                val scope = rememberCoroutineScope()
                OnboardingFlow(
                    step = currentStep,
                    misoCat = misoCat,
                    onNext = { currentStep++ },
                    onPermissionsComplete = { currentStep++ },
                    onGrantUsageActual = {
                        act.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onGrantOverlay = {
                        // Deep-links directly to PawsUp's overlay toggle — no scrolling needed
                        act.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${act.packageName}")
                            )
                        )
                    },
                    onGrantNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onFinish = {
                        scope.launch { prefs.setOnboardingCompleted(true) }
                        ContextCompat.startForegroundService(
                            act, Intent(act, MonitoringService::class.java)
                        )
                        // Open app picker; when user taps Done, appPickerLauncher
                        // navigates to Settings and finishes onboarding.
                        appPickerLauncher.launch(
                            Intent(act, com.pawsup.apppicker.AppPickerActivity::class.java)
                        )
                    },
                    isUsageGranted  = { act.hasUsagePermission() },
                    isOverlayGranted = { Settings.canDrawOverlays(act) },
                    isNotifGranted  = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                act, android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else true
                    }
                )
            }
        }
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

// ─── FLOW ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun OnboardingFlow(
    step: Int,
    misoCat: com.pawsup.cats.Cat,
    onNext: () -> Unit,
    onPermissionsComplete: () -> Unit,
    onGrantUsageActual: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotifications: () -> Unit,
    onFinish: () -> Unit,
    isUsageGranted: () -> Boolean,
    isOverlayGranted: () -> Boolean,
    isNotifGranted: () -> Boolean
) {
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
        }
    ) { currentStep ->
        when (currentStep) {
            0    -> WelcomeStep(misoCat = misoCat, onNext = onNext)
            1    -> PermissionsStep(
                        onGrantUsageActual   = onGrantUsageActual,
                        onGrantOverlay       = onGrantOverlay,
                        onGrantNotifications = onGrantNotifications,
                        onPermissionsComplete = onPermissionsComplete,
                        isUsageGranted       = isUsageGranted,
                        isOverlayGranted     = isOverlayGranted,
                        isNotifGranted       = isNotifGranted
                    )
            else -> MeetMisoStep(onFinish)
        }
    }
}

// ─── WELCOME ─────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(misoCat: com.pawsup.cats.Cat, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A08))
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        // Large headline — competitor-inspired layout, brand language
        Text(
            text = "When you've been\nscrolling too long,\na cat comes to sit\nwith you.",
            fontFamily = CrimsonTextFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            color = Color(0xFFF5ECD7),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // "2 min setup" badge — matches competitor's "Backed by science" pill
        Surface(
            color = Color(0xFF2A1F1A),
            shape = RoundedCornerShape(50.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_cat),
                    contentDescription = null,
                    tint = Color(0xFFE8A87C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "2 minutes to set up",
                    color = Color(0xFFE8A87C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Miso illustration fills the middle space
        CatPosterImage(
            cat = misoCat,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        // Large CTA button — matches competitor's bottom button style
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8A87C),
                contentColor   = Color(0xFF1A0C00)
            )
        ) {
            Text(
                text = "Let's go",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ─── PERMISSIONS ─────────────────────────────────────────────────────────────

@Composable
private fun PermissionsStep(
    onGrantUsageActual: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotifications: () -> Unit,
    onPermissionsComplete: () -> Unit,
    isUsageGranted: () -> Boolean,
    isOverlayGranted: () -> Boolean,
    isNotifGranted: () -> Boolean
) {
    var usageOk   by remember { mutableStateOf(isUsageGranted()) }
    var overlayOk by remember { mutableStateOf(isOverlayGranted()) }
    var notifOk   by remember { mutableStateOf(isNotifGranted()) }
    val allGranted = usageOk && overlayOk && notifOk

    var showUsageGuide   by remember { mutableStateOf(false) }
    var showOverlayGuide by remember { mutableStateOf(false) }

    // Re-check all permissions instantly on every resume (user returns from system settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                usageOk   = isUsageGranted()
                overlayOk = isOverlayGranted()
                notifOk   = isNotifGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showUsageGuide) {
        UsageAccessGuideScreen(
            onOpenSettings      = { showUsageGuide = false; onGrantUsageActual() },
            onBack              = { showUsageGuide = false },
            isPermissionGranted = isUsageGranted
        )
        return
    }

    if (showOverlayGuide) {
        OverlayPermissionGuideScreen(
            onOpenSettings      = { showOverlayGuide = false; onGrantOverlay() },
            onBack              = { showOverlayGuide = false },
            isPermissionGranted = isOverlayGranted
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A08))
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            "Almost there",
            fontFamily = CrimsonTextFamily,
            fontSize = 32.sp,
            color = Color(0xFFF5ECD7)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Three quick permissions and you're set.",
            color = Color(0xFFF5ECD7).copy(0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Order: Notifications → Overlay → Usage Access
        PermissionRow(
            title   = stringResource(R.string.onboarding_permission_notification_title),
            body    = stringResource(R.string.onboarding_permission_notification_body),
            granted = notifOk,
            onGrant = { onGrantNotifications() }
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            title   = stringResource(R.string.onboarding_permission_overlay_title),
            body    = stringResource(R.string.onboarding_permission_overlay_body),
            granted = overlayOk,
            onGrant = { showOverlayGuide = true }
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            title   = stringResource(R.string.onboarding_permission_usage_title),
            body    = stringResource(R.string.onboarding_permission_usage_body),
            granted = usageOk,
            onGrant = { showUsageGuide = true }  // opens guide first, then settings
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick  = onPermissionsComplete,
            enabled  = allGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFFE8A87C),
                contentColor           = Color(0xFF1A0C00),
                disabledContainerColor = Color(0xFF2A1F1A),
                disabledContentColor   = Color(0xFFF5ECD7).copy(0.3f)
            )
        ) {
            Text(
                stringResource(R.string.onboarding_all_done_cta),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ─── USAGE ACCESS GUIDE ───────────────────────────────────────────────────────

@Composable
private fun UsageAccessGuideScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    isPermissionGranted: () -> Boolean
) {
    // Auto-dismiss when the user returns from settings with the permission already granted
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isPermissionGranted()) {
                onBack()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_800)
            activeStep = (activeStep + 1) % 3
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A08))
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // Back
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color(0xFFF5ECD7).copy(0.5f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Granting Usage Access",
            fontFamily = CrimsonTextFamily,
            fontSize = 30.sp,
            color = Color(0xFFF5ECD7),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Follow these three steps after tapping the button below.",
            color = Color(0xFFF5ECD7).copy(0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // Looping step cards — each highlights in sequence
        GuideStepCard(
            number   = "1",
            title    = "Open Usage Access settings",
            subtitle = "Tap the button at the bottom of this screen.",
            active   = activeStep == 0
        )
        Spacer(Modifier.height(12.dp))
        GuideStepCard(
            number   = "2",
            title    = "Find PawsUp in the list",
            subtitle = "Scroll until you see PawsUp and tap it.",
            active   = activeStep == 1
        )
        Spacer(Modifier.height(12.dp))
        GuideStepCard(
            number   = "3",
            title    = "Turn on Usage Access",
            subtitle = "Tap the toggle to enable it, then come back here.",
            active   = activeStep == 2
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8A87C),
                contentColor   = Color(0xFF1A0C00)
            )
        ) {
            Text("Open Usage Access Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ─── OVERLAY PERMISSION GUIDE ────────────────────────────────────────────────

@Composable
private fun OverlayPermissionGuideScreen(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    isPermissionGranted: () -> Boolean
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isPermissionGranted()) {
                onBack()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_800)
            activeStep = (activeStep + 1) % 3
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A08))
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color(0xFFF5ECD7).copy(0.5f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Enabling Display Over Apps",
            fontFamily = CrimsonTextFamily,
            fontSize = 30.sp,
            color = Color(0xFFF5ECD7),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Follow these steps after tapping the button below.",
            color = Color(0xFFF5ECD7).copy(0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        GuideStepCard(
            number   = "1",
            title    = "Open Display over other apps",
            subtitle = "Tap the button at the bottom of this screen.",
            active   = activeStep == 0
        )
        Spacer(Modifier.height(12.dp))
        GuideStepCard(
            number   = "2",
            title    = "Find PawsUp in the list",
            subtitle = "Scroll down until you see PawsUp and tap it.",
            active   = activeStep == 1
        )
        Spacer(Modifier.height(12.dp))
        GuideStepCard(
            number   = "3",
            title    = "Allow display over other apps",
            subtitle = "Tap the toggle to enable it, then come back here.",
            active   = activeStep == 2
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8A87C),
                contentColor   = Color(0xFF1A0C00)
            )
        ) {
            Text("Open Display Over Apps Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun GuideStepCard(number: String, title: String, subtitle: String, active: Boolean) {
    val borderColor by animateColorAsState(
        targetValue = if (active) Color(0xFFE8A87C) else Color.Transparent,
        animationSpec = tween(400),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (active) Color(0xFF3D2E26) else Color(0xFF2A1F1A),
        animationSpec = tween(400),
        label = "bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color(0xFFE8A87C) else Color(0xFF3D2E26)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = number,
                    color = if (active) Color(0xFF1A0C00) else Color(0xFFF5ECD7).copy(0.5f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    color = if (active) Color(0xFFF5ECD7) else Color(0xFFF5ECD7).copy(0.7f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = subtitle,
                    color = Color(0xFFF5ECD7).copy(0.5f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ─── PERMISSION ROW ───────────────────────────────────────────────────────────

@Composable
private fun PermissionRow(
    title: String,
    body: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF2A1F1A)),
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color(0xFFF5ECD7), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(body, color = Color(0xFFF5ECD7).copy(0.6f), fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Text("✓", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else {
                OutlinedButton(
                    onClick = onGrant,
                    border  = BorderStroke(1.dp, Color(0xFFE8A87C).copy(0.5f)),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE8A87C))
                ) {
                    Text(stringResource(R.string.onboarding_permission_grant), fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── MEET MISO ────────────────────────────────────────────────────────────────

@Composable
private fun MeetMisoStep(onFinish: () -> Unit) {
    var showFarewell by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayerCompositor(
            assetPath = CatAssetResolver.entranceVideo("miso"),
            loop      = false,
            onVideoEnd = {
                showFarewell = true
                scope.launch {
                    delay(2_000)
                    onFinish()
                }
            }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .padding(horizontal = 32.dp)
        ) {
            AnimatedVisibility(visible = !showFarewell, enter = fadeIn()) {
                Text(
                    text = stringResource(R.string.onboarding_miso_intro),
                    fontFamily = CrimsonTextFamily,
                    fontSize = 26.sp,
                    color = Color(0xFFF5ECD7),
                    textAlign = TextAlign.Center
                )
            }
            AnimatedVisibility(visible = showFarewell, enter = fadeIn()) {
                Text(
                    text = stringResource(R.string.onboarding_miso_farewell),
                    fontFamily = CrimsonTextFamily,
                    fontSize = 26.sp,
                    color = Color(0xFFF5ECD7),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
