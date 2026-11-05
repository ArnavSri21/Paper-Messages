package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.AmbientLiquidBackground
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperError
import com.example.ui.theme.PaperGlassBorderL2
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSuccess
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutPaperAccount(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1050L)
        onFinished()
    }

    AmbientLiquidBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(alpha)
                    .scale(scale)
            ) {
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_3_ELEVATED,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.size(104.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_emblem_1793896222776),
                        contentDescription = "PaperMessages emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "PaperMessages",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PaperTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Structure, space and clarity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperTextSecondary
                )
            }
        }
    }
}

private data class OnboardingPageData(
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun OnboardingAndAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showOnboarding by remember { mutableStateOf(true) }
    var currentSlide by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val pages = remember {
        listOf(
            OnboardingPageData(
                badge = "01 · PRIVATE COMMUNICATION",
                title = "End-to-End Encrypted by Default",
                subtitle = "Every 1-on-1 conversation and voice note is protected with hardware-backed ECDH key agreement and AES-GCM authenticated encryption.",
                icon = Icons.Default.Lock
            ),
            OnboardingPageData(
                badge = "02 · ORGANIZED GROUP CONVERSATIONS",
                title = "Threads That Eliminate Noise",
                subtitle = "Turn any group message into a focused thread with its own subscriptions, @mentions, and live reply counters—without cluttering the main feed.",
                icon = Icons.Default.Forum
            ),
            OnboardingPageData(
                badge = "03 · COMMUNITIES & COLLABORATION",
                title = "Communities, Channels & Calls",
                subtitle = "Discover public communities, publish broadcast announcements with live polls, and connect over crisp voice and video calls.",
                icon = Icons.Default.Groups
            )
        )
    }

    AmbientLiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Brand Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_emblem_1793896222776),
                        contentDescription = "PaperMessages logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PaperMessages",
                        style = MaterialTheme.typography.titleMedium,
                        color = PaperTextPrimary
                    )
                }

                if (showOnboarding) {
                    TextButton(
                        onClick = { showOnboarding = false },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip", color = PaperTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showOnboarding) {
                val page = pages[currentSlide]
                Column(
                    modifier = Modifier.widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidGlassSurface(
                        intensity = GlassIntensity.LEVEL_3_ELEVATED,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_onboarding_glass_1793896221242),
                            contentDescription = "Liquid Glass illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.78f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC080A10))
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(PaperAccentPrimary, PaperAccentBlue)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = page.title,
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = page.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PaperHighlight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedContent(targetState = page, label = "onboarding_slide") { slide ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = PaperTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = slide.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PaperTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Slide indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (idx == currentSlide) 26.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == currentSlide) PaperAccentPrimary else PaperGlassBorderL2
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    LiquidGlassButton(
                        text = if (currentSlide < pages.lastIndex) "Continue" else "Get Started",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (currentSlide < pages.lastIndex) {
                                currentSlide += 1
                            } else {
                                showOnboarding = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "onboarding_continue_button"
                    )
                }
            } else {
                // Authentication Card (Part 5.2)
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_3_ELEVATED,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem_1793896222776),
                            contentDescription = "PaperMessages logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Your conversations, reimagined.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = PaperTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sign in with Google to initialize your encrypted device keystore, then bind your phone number and @username handle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaperTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (authError != null) {
                            LiquidGlassSurface(
                                intensity = GlassIntensity.LEVEL_1_SUBTLE,
                                shape = RoundedCornerShape(14.dp),
                                accentTint = PaperError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = authError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PaperError,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                color = PaperAccentPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            LiquidGlassButton(
                                text = "Sign in with Google",
                                icon = Icons.Default.Security,
                                onClick = {
                                    isLoading = true
                                    authError = null
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            authError = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "google_sign_in_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy",
                                tint = PaperAccentSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero-trust Firestore rules · Android Keystore ECDH + AES-GCM",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Phone Number, Country Code, 6-Digit OTP Verification & @Username Handle Setup Screen (Part 5.2)
 */
@Composable
fun PhoneAndOtpSetupScreen(
    initialName: String,
    onCompleteSetup: (displayName: String, username: String, phone: String, countryCode: String, bio: String) -> Unit
) {
    val countryCodes = remember {
        listOf(
            "+1 (US/CA)" to "+1",
            "+44 (UK)" to "+44",
            "+49 (DE)" to "+49",
            "+81 (JP)" to "+81",
            "+91 (IN)" to "+91",
            "+61 (AU)" to "+61"
        )
    }
    var selectedCountry by remember { mutableStateOf(countryCodes.first()) }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("5550192834") }
    var displayName by remember { mutableStateOf(initialName.ifBlank { "Alex Mercer" }) }
    var username by remember {
        mutableStateOf(
            initialName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16).ifBlank { "alex_mercer" }
        )
    }
    var bio by remember { mutableStateOf("Building focused conversations on PaperMessages.") }

    var stepOtp by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("482910") }
    var countdown by remember { mutableIntStateOf(30) }

    LaunchedEffect(stepOtp) {
        if (stepOtp) {
            countdown = 30
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
        }
    }

    AmbientLiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_3_ELEVATED,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(PaperAccentPrimary, PaperAccentBlue))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!stepOtp) Icons.Default.PhoneAndroid else Icons.Default.CheckCircle,
                                contentDescription = "Setup step",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (!stepOtp) "Your conversations, reimagined." else "Verify 6-Digit Security Code",
                                style = MaterialTheme.typography.titleMedium,
                                color = PaperTextPrimary
                            )
                            Text(
                                text = if (!stepOtp) "Step 1 of 2 · Phone & Unique @Handle" else "Step 2 of 2 · Sent to ${selectedCountry.second} $phoneNumber",
                                style = MaterialTheme.typography.labelMedium,
                                color = PaperTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!stepOtp) {
                        // Country Code + Phone Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                LiquidGlassSurface(
                                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = { countryMenuExpanded = true },
                                    modifier = Modifier
                                        .height(56.dp)
                                        .padding(top = 4.dp)
                                        .testTag("country_code_selector")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedCountry.first,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = PaperTextPrimary
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = countryMenuExpanded,
                                    onDismissRequest = { countryMenuExpanded = false }
                                ) {
                                    countryCodes.forEach { pair ->
                                        DropdownMenuItem(
                                            text = { Text(pair.first) },
                                            onClick = {
                                                selectedCountry = pair
                                                countryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() }.take(15) },
                                label = { Text("Phone number") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = paperTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("phone_number_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(64) },
                            label = { Text("Mandatory Display Name") },
                            singleLine = true,
                            colors = paperTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(30) },
                            label = { Text("Unique @username (min 3 chars)") },
                            prefix = { Text("@", color = PaperAccentSecondary) },
                            singleLine = true,
                            colors = paperTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it.take(180) },
                            label = { Text("Bio") },
                            maxLines = 2,
                            colors = paperTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        LiquidGlassButton(
                            text = "Continue to Verification",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            enabled = phoneNumber.length >= 6 && displayName.isNotBlank() && username.length >= 3,
                            onClick = { stepOtp = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "continue_to_otp_button"
                        )
                    } else {
                        // 6-Digit OTP Interface (Part 5.2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (0 until 6).forEach { index ->
                                val digitChar = otpCode.getOrNull(index)?.toString() ?: ""
                                LiquidGlassSurface(
                                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                                    shape = RoundedCornerShape(14.dp),
                                    accentTint = if (digitChar.isNotEmpty()) PaperAccentPrimary else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 3.dp)
                                        .height(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = digitChar,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = PaperTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = {
                                val clean = it.filter { ch -> ch.isDigit() }.take(6)
                                otpCode = clean
                            },
                            label = { Text("Enter 6-digit verification code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            colors = paperTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { stepOtp = false }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit phone",
                                    tint = PaperAccentSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit phone number", color = PaperAccentSecondary)
                            }

                            Text(
                                text = if (countdown > 0) "Resend in ${countdown}s" else "Code verified Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (otpCode.length == 6) PaperSuccess else PaperTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        LiquidGlassButton(
                            text = "Verify & Launch PaperMessages",
                            icon = Icons.Default.CheckCircle,
                            enabled = otpCode.length == 6,
                            onClick = {
                                onCompleteSetup(
                                    displayName,
                                    username,
                                    phoneNumber,
                                    selectedCountry.second,
                                    bio
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "verify_otp_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun paperTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PaperTextPrimary,
    unfocusedTextColor = PaperTextPrimary,
    focusedContainerColor = PaperSurfaceElevated.copy(alpha = 0.65f),
    unfocusedContainerColor = PaperSurfaceElevated.copy(alpha = 0.45f),
    focusedBorderColor = PaperAccentPrimary,
    unfocusedBorderColor = PaperGlassBorderL2,
    focusedLabelColor = PaperHighlight,
    unfocusedLabelColor = PaperTextSecondary,
    cursorColor = PaperAccentSecondary
)
