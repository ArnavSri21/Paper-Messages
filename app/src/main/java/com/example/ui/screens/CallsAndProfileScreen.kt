package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CallRecordItem
import com.example.data.model.UserProfile
import com.example.security.CryptoEngine
import com.example.ui.components.AmbientLiquidBackground
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PaperAvatar
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperBgMain
import com.example.ui.theme.PaperError
import com.example.ui.theme.PaperGlassBorderL2
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSuccess
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.ActiveCallSession
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Calls History Screen (Part 5.8)
 */
@Composable
fun CallsTabScreen(
    calls: List<CallRecordItem>,
    onStartCall: (conversationId: String, contactName: String, avatarColor: String, isVideo: Boolean) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INCOMING, OUTGOING, MISSED

    val filteredCalls = remember(calls, selectedFilter) {
        if (selectedFilter == "ALL") {
            calls
        } else {
            calls.filter { it.direction.equals(selectedFilter, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_history_list"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PaperTextPrimary
                    )
                    Text(
                        text = "End-to-End Encrypted Voice & Video Signaling",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperAccentSecondary
                    )
                }

                LiquidGlassButton(
                    text = "New Call",
                    icon = Icons.Default.Call,
                    onClick = {
                        onStartCall("conv_private_elena", "Elena Rostova", "#54D6E8", false)
                    },
                    testTag = "start_quick_call_button"
                )
            }
        }

        // Filter Chips (All, Incoming, Outgoing, Missed)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("ALL", "INCOMING", "OUTGOING", "MISSED")) { filter ->
                    val selected = filter == selectedFilter
                    LiquidGlassSurface(
                        intensity = if (selected) GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                        shape = RoundedCornerShape(14.dp),
                        accentTint = if (selected) PaperAccentPrimary else null,
                        onClick = { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else PaperTextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        items(filteredCalls, key = { it.id }) { call ->
            CallHistoryCard(
                call = call,
                onVoiceRedial = {
                    onStartCall(call.conversationId, call.contactName, call.avatarColor, false)
                },
                onVideoRedial = {
                    onStartCall(call.conversationId, call.contactName, call.avatarColor, true)
                }
            )
        }
    }
}

@Composable
private fun CallHistoryCard(
    call: CallRecordItem,
    onVoiceRedial: () -> Unit,
    onVideoRedial: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }
    val dateStr = remember(call.createdAt) {
        call.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Today"
    }
    val durationStr = remember(call.durationSeconds) {
        if (call.durationSeconds <= 0L) {
            "Missed"
        } else {
            val mins = call.durationSeconds / 60
            val secs = call.durationSeconds % 60
            "${mins}m ${secs}s"
        }
    }

    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(20.dp),
        onClick = if (call.callType == "VIDEO") onVideoRedial else onVoiceRedial,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperAvatar(
                name = call.contactName,
                avatarHex = call.avatarColor,
                size = 48.dp,
                isOnline = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (call.direction == "MISSED") PaperError else PaperTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dirIcon = when (call.direction) {
                        "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived
                        "MISSED" -> Icons.AutoMirrored.Filled.CallMissed
                        else -> Icons.AutoMirrored.Filled.CallMade
                    }
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = call.direction,
                        tint = if (call.direction == "MISSED") PaperError else PaperSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${call.callType} · $durationStr · $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary
                    )
                }
            }

            IconButton(onClick = onVoiceRedial) {
                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = PaperTextPrimary)
            }
            IconButton(onClick = onVideoRedial) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = PaperAccentSecondary)
            }
        }
    }
}

/**
 * Live Voice & Video Call Interface with CameraX Self-Preview & Audio Waveform (Part 5.9)
 */
@Composable
fun ActiveCallOverlayScreen(
    session: ActiveCallSession,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(session.isVideo) {
        if (session.isVideo && !hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (session.isMuted) 1.02f else 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_pulse_ring"
    )

    val mins = (session.elapsedSeconds / 60).toString().padStart(2, '0')
    val secs = (session.elapsedSeconds % 60).toString().padStart(2, '0')

    AmbientLiquidBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("active_call_overlay")
        ) {
            // Center Area: Voice Audio Visualizer OR Video Call Viewport
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PaperAccentPrimary.copy(alpha = 0.32f),
                                        PaperAccentSecondary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    PaperAvatar(
                        name = session.contactName,
                        avatarHex = session.avatarColor,
                        size = 116.dp,
                        isOnline = true
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = session.contactName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = PaperTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${if (session.isVideo) "E2EE Video Call" else "E2EE Voice Call"} · $mins:$secs",
                    style = MaterialTheme.typography.titleSmall,
                    color = PaperAccentSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Animated Audio Waveform Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp)
                ) {
                    listOf(0.4f, 0.85f, 1.0f, 0.65f, 0.9f, 0.5f, 0.75f).forEach { factor ->
                        val barHeight = (12 + (20 * factor * (pulseScale - 0.85f))).coerceIn(8f, 34f)
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(barHeight.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(PaperAccentSecondary, PaperAccentPrimary))
                                )
                        )
                    }
                }
            }

            // Rounded Floating CameraX Self-Preview for Video Calls (Part 5.9)
            if (session.isVideo && session.isCameraEnabled) {
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_3_ELEVATED,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 20.dp)
                        .width(126.dp)
                        .height(178.dp)
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                            },
                            update = { previewView ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    try {
                                        val provider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val selector = if (session.isFrontCamera) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }
                                        provider.unbindAll()
                                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                                    } catch (_: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Self Preview\n(Camera Ready)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperTextSecondary
                            )
                        }
                    }
                }
            }

            // Bottom Floating Glass Control Panel (Part 5.9)
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_3_ELEVATED,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (session.isMuted) "Unmute" else "Mute",
                        active = !session.isMuted,
                        onClick = onToggleMute
                    )
                    CallControlButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Speaker",
                        active = session.isSpeakerOn,
                        onClick = onToggleSpeaker
                    )
                    CallControlButton(
                        icon = if (session.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = "Camera",
                        active = session.isCameraEnabled,
                        onClick = onToggleCamera
                    )
                    if (session.isVideo) {
                        CallControlButton(
                            icon = Icons.Default.Cameraswitch,
                            label = "Flip",
                            active = true,
                            onClick = onFlipCamera
                        )
                    }
                    // End Call Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PaperError)
                            .clickable(onClick = onEndCall)
                            .testTag("end_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LiquidGlassSurface(
            intensity = if (active) GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
            shape = CircleShape,
            accentTint = if (active) PaperAccentPrimary else null,
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) Color.White else PaperTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PaperTextSecondary)
    }
}

/**
 * Profile & Categorized Settings Screen (Part 5.10 & 5.11)
 */
@Composable
fun ProfileAndSettingsScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    darkTheme: Boolean,
    accentHex: String,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectAccentColor: (String) -> Unit,
    onSaveProfileAndPrivacy: (
        displayName: String,
        username: String,
        bio: String,
        privacyOnline: String,
        privacyLastSeen: String,
        privacyReadReceipts: Boolean,
        privacyPhoneLookup: String,
        reducedMotion: Boolean
    ) -> Unit,
    onRotateKeys: () -> Unit,
    onToggleBlockUser: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    val displayName = userProfile?.displayName?.ifBlank { "Alex Mercer" } ?: "Alex Mercer"
    val username = userProfile?.username?.ifBlank { "alex_mercer" } ?: "alex_mercer"
    val bio = userProfile?.bio?.ifBlank { "Conversations deserve structure, space and clarity." }
        ?: "Conversations deserve structure, space and clarity."
    val phone = "${userProfile?.countryCode ?: "+1"} ${userProfile?.phoneNumber?.ifBlank { "5550192834" } ?: "5550192834"}"
    val keyFingerprint = userProfile?.keyFingerprint?.ifBlank {
        CryptoEngine.computeSafetyFingerprint(currentUserId)
    } ?: CryptoEngine.computeSafetyFingerprint(currentUserId)

    val privacyOnline = userProfile?.privacyOnline ?: "EVERYONE"
    val privacyLastSeen = userProfile?.privacyLastSeen ?: "EVERYONE"
    val privacyReadReceipts = userProfile?.privacyReadReceipts ?: true
    val privacyPhoneLookup = userProfile?.privacyPhoneLookup ?: "CONTACTS"
    val reducedMotion = userProfile?.reducedMotion ?: false

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_and_settings_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // 1. Large Profile Hero Card (Part 5.10)
        item {
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_3_ELEVATED,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PaperAvatar(
                        name = displayName,
                        avatarHex = accentHex,
                        size = 82.dp,
                        isOnline = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PaperTextPrimary
                    )
                    Text(
                        text = "@$username · $phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperAccentSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LiquidGlassButton(
                        text = "Edit Profile & Handle",
                        icon = Icons.Default.Edit,
                        onClick = { showEditDialog = true },
                        testTag = "edit_profile_button"
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. Category: Privacy Settings (Part 5.11)
        item {
            SettingsSectionHeader(icon = Icons.Default.Lock, title = "PRIVACY CONTROLS")
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_2_STANDARD,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsCycleRow(
                        label = "Online Status Visibility",
                        value = privacyOnline,
                        onCycle = {
                            val next = when (privacyOnline) {
                                "EVERYONE" -> "CONTACTS"
                                "CONTACTS" -> "NOBODY"
                                else -> "EVERYONE"
                            }
                            onSaveProfileAndPrivacy(
                                displayName, username, bio, next,
                                privacyLastSeen, privacyReadReceipts, privacyPhoneLookup, reducedMotion
                            )
                        }
                    )
                    SettingsCycleRow(
                        label = "Last Seen Timestamp",
                        value = privacyLastSeen,
                        onCycle = {
                            val next = when (privacyLastSeen) {
                                "EVERYONE" -> "CONTACTS"
                                "CONTACTS" -> "NOBODY"
                                else -> "EVERYONE"
                            }
                            onSaveProfileAndPrivacy(
                                displayName, username, bio, privacyOnline,
                                next, privacyReadReceipts, privacyPhoneLookup, reducedMotion
                            )
                        }
                    )
                    SettingsCycleRow(
                        label = "Phone Number Discovery",
                        value = privacyPhoneLookup,
                        onCycle = {
                            val next = when (privacyPhoneLookup) {
                                "EVERYONE" -> "CONTACTS"
                                "CONTACTS" -> "NOBODY"
                                else -> "EVERYONE"
                            }
                            onSaveProfileAndPrivacy(
                                displayName, username, bio, privacyOnline,
                                privacyLastSeen, privacyReadReceipts, next, reducedMotion
                            )
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share Read Receipts", style = MaterialTheme.typography.bodyLarge, color = PaperTextPrimary)
                            Text("Allow group members to see when you read messages", style = MaterialTheme.typography.labelSmall, color = PaperTextSecondary)
                        }
                        Switch(
                            checked = privacyReadReceipts,
                            onCheckedChange = { enabled ->
                                onSaveProfileAndPrivacy(
                                    displayName, username, bio, privacyOnline,
                                    privacyLastSeen, enabled, privacyPhoneLookup, reducedMotion
                                )
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 3. Category: Security, Encryption & Device Management (Part 5.11 & Part 7)
        item {
            SettingsSectionHeader(icon = Icons.Default.Security, title = "SECURITY & END-TO-END ENCRYPTION")
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_2_STANDARD,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = PaperSuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Android Keystore EC P-256 + AES-256-GCM",
                            style = MaterialTheme.typography.titleSmall,
                            color = PaperTextPrimary
                        )
                    }
                    Text(
                        text = "Safety Number Fingerprint:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary
                    )
                    Text(
                        text = keyFingerprint,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PaperHighlight
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = PaperAccentSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Device: ${userProfile?.deviceName ?: "Android Primary Device"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaperTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LiquidGlassButton(
                        text = "Rotate Hardware E2EE Keys",
                        icon = Icons.Default.Refresh,
                        isPrimary = false,
                        onClick = onRotateKeys,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "rotate_keys_button"
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 4. Category: Appearance & Accessibility (Part 5.11)
        item {
            SettingsSectionHeader(icon = Icons.Default.Palette, title = "APPEARANCE & MOTION")
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_2_STANDARD,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = PaperHighlight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Deep Dark Liquid Glass Mode", style = MaterialTheme.typography.bodyLarge, color = PaperTextPrimary)
                        }
                        Switch(checked = darkTheme, onCheckedChange = onToggleDarkTheme)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduced Motion (Accessibility)", style = MaterialTheme.typography.bodyLarge, color = PaperTextPrimary)
                            Text("Disable ambient background drift and spring scaling", style = MaterialTheme.typography.labelSmall, color = PaperTextSecondary)
                        }
                        Switch(
                            checked = reducedMotion,
                            onCheckedChange = { rm ->
                                onSaveProfileAndPrivacy(
                                    displayName, username, bio, privacyOnline,
                                    privacyLastSeen, privacyReadReceipts, privacyPhoneLookup, rm
                                )
                            }
                        )
                    }

                    Text("Accent Color Token", style = MaterialTheme.typography.labelMedium, color = PaperTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("#8B7CFF", "#54D6E8", "#43D9A3", "#FFBE69", "#FF667D").forEach { hex ->
                            val color = parseHexColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (accentHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                        color = if (accentHex.equals(hex, ignoreCase = true)) Color.White else PaperGlassBorderL2,
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectAccentColor(hex) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 5. Category: Blocked Users & About PaperMessages
        item {
            SettingsSectionHeader(icon = Icons.Default.Block, title = "MODERATION & BLOCKED ACCOUNTS")
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_2_STANDARD,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val blocked = userProfile?.blockedList() ?: emptyList()
                    Text(
                        text = if (blocked.isEmpty()) "No blocked accounts." else "Blocked: ${blocked.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperTextSecondary
                    )
                    LiquidGlassButton(
                        text = "Manage Blocked Users",
                        icon = Icons.Default.Block,
                        isPrimary = false,
                        onClick = { showBlockDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        item {
            SettingsSectionHeader(icon = Icons.Default.Info, title = "ABOUT PAPERMESSAGES")
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_1_SUBTLE,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PaperMessages v2.4 · Startup Modular Monolith Edition",
                        style = MaterialTheme.typography.titleSmall,
                        color = PaperTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"Conversations deserve more than a message feed. They deserve structure, space and clarity.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperHighlight
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LiquidGlassButton(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        isPrimary = false,
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sign_out_button"
                    )
                }
            }
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(displayName) }
        var editHandle by remember { mutableStateOf(username) }
        var editBio by remember { mutableStateOf(bio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Edit PaperMessages Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name (Mandatory)") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editHandle,
                        onValueChange = { editHandle = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(30) },
                        label = { Text("Unique @username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it.take(220) },
                        label = { Text("Bio") },
                        maxLines = 3,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Save Profile",
                    enabled = editName.isNotBlank() && editHandle.length >= 3,
                    onClick = {
                        showEditDialog = false
                        onSaveProfileAndPrivacy(
                            editName, editHandle, editBio, privacyOnline,
                            privacyLastSeen, privacyReadReceipts, privacyPhoneLookup, reducedMotion
                        )
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }

    if (showBlockDialog) {
        var handleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Block or Unblock @username") },
            text = {
                OutlinedTextField(
                    value = handleInput,
                    onValueChange = { handleInput = it },
                    label = { Text("Enter @username or display name") },
                    singleLine = true,
                    colors = paperTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Toggle Block",
                    enabled = handleInput.isNotBlank(),
                    onClick = {
                        onToggleBlockUser(handleInput.trim())
                        showBlockDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Close", color = PaperTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PaperAccentSecondary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = PaperTextSecondary
        )
    }
}

@Composable
private fun SettingsCycleRow(
    label: String,
    value: String,
    onCycle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = PaperTextPrimary)
        LiquidGlassSurface(
            intensity = GlassIntensity.LEVEL_1_SUBTLE,
            shape = RoundedCornerShape(10.dp),
            onClick = onCycle
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PaperHighlight,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
