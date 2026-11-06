package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalSearchDatabase
import com.example.data.repository.PaperRepository
import com.example.ui.components.AmbientLiquidBackground
import com.example.ui.components.FloatingLiquidGlassNavBar
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MainNavSection
import com.example.ui.screens.ActiveCallOverlayScreen
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.CallsTabScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.CommunitiesAndChannelsScreen
import com.example.ui.screens.ConversationAndThreadScreen
import com.example.ui.screens.OnboardingAndAuthScreen
import com.example.ui.screens.PhoneAndOtpSetupScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.signOutPaperAccount
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperMessagesTheme
import com.example.ui.viewmodel.PaperViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaperMessagesRootApp()
        }
    }
}

@Composable
fun PaperMessagesRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Strict Auth Gate: Never instantiate Firestore repositories or ViewModels while unauthenticated
        PaperMessagesTheme {
            OnboardingAndAuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        val resolvedName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: ""
        AuthenticatedPaperMessagesApp(
            currentUserId = user.uid,
            initialDisplayName = resolvedName,
            onSignedOut = { currentUser = null }
        )
    }
}

@Composable
fun AuthenticatedPaperMessagesApp(
    currentUserId: String,
    initialDisplayName: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: PaperViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val searchDao = LocalSearchDatabase.getInstance(app).searchDao()
                PaperViewModel(
                    repository = PaperRepository(db),
                    searchDao = searchDao,
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val activeThreadParentId by viewModel.activeThreadParentId.collectAsStateWithLifecycle()
    val activeThreadReplies by viewModel.activeThreadReplies.collectAsStateWithLifecycle()
    val threadSearchQuery by viewModel.threadSearchQuery.collectAsStateWithLifecycle()
    val communities by viewModel.communities.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val localIndexedResults by viewModel.localIndexedResults.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val accentHex by viewModel.accentHex.collectAsStateWithLifecycle()

    var currentSection by rememberSaveable { mutableStateOf(MainNavSection.CHATS) }
    var phoneVerificationCompletedSession by rememberSaveable { mutableStateOf(false) }

    // Auto-dismiss status banner after 3.5 seconds
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.dismissBanner()
        }
    }

    PaperMessagesTheme(
        darkTheme = darkTheme,
        accentHex = accentHex,
        reducedMotion = userProfile?.reducedMotion ?: false
    ) {
        // Show Phone + OTP + @username Handle Setup if profile has not completed onboarding yet
        val needsPhoneSetup = userProfile != null && !userProfile!!.onboardingCompleted && !phoneVerificationCompletedSession
        if (needsPhoneSetup) {
            PhoneAndOtpSetupScreen(
                initialName = initialDisplayName,
                onCompleteSetup = { displayName, username, phone, countryCode, bio ->
                    phoneVerificationCompletedSession = true
                    viewModel.completePhoneAndHandleOnboarding(
                        displayName = displayName,
                        username = username,
                        phoneNumber = phone,
                        countryCode = countryCode,
                        bio = bio
                    )
                }
            )
            return@PaperMessagesTheme
        }

        // Active Voice / Video Call Full-Screen Overlay
        val callSession = activeCallSession
        if (callSession != null) {
            BackHandler { viewModel.endActiveCall() }
            ActiveCallOverlayScreen(
                session = callSession,
                onToggleMute = { viewModel.updateActiveCallControls(isMuted = !callSession.isMuted) },
                onToggleSpeaker = { viewModel.updateActiveCallControls(isSpeakerOn = !callSession.isSpeakerOn) },
                onToggleCamera = { viewModel.updateActiveCallControls(isCameraEnabled = !callSession.isCameraEnabled) },
                onFlipCamera = { viewModel.updateActiveCallControls(isFrontCamera = !callSession.isFrontCamera) },
                onEndCall = { viewModel.endActiveCall() }
            )
            return@PaperMessagesTheme
        }

        // Active Individual or Threaded Group Conversation Screen
        val activeConv = conversations.find { it.id == activeConversationId }
        if (activeConv != null) {
            ConversationAndThreadScreen(
                currentUserId = currentUserId,
                conversation = activeConv,
                allConversations = conversations,
                messages = activeMessages,
                selectedTopic = selectedTopic,
                activeThreadParentId = activeThreadParentId,
                threadReplies = activeThreadReplies,
                threadSearchQuery = threadSearchQuery,
                privacyReadReceiptsEnabled = userProfile?.privacyReadReceipts ?: true,
                onBackToChats = { viewModel.closeConversation() },
                onSelectTopic = { viewModel.selectTopic(it) },
                onOpenThread = { viewModel.openThread(it) },
                onCloseThread = { viewModel.closeThread() },
                onThreadSearchChange = { viewModel.updateThreadSearchQuery(it) },
                onSendMessage = { text, topic, attachType, attachName, attachMeta ->
                    viewModel.sendChatMessage(text, topic, attachType, attachName, attachMeta)
                },
                onToggleMessageReaction = { msg, emoji -> viewModel.toggleReactionOnMessage(msg, emoji) },
                onEditMessage = { msg, updated -> viewModel.editMessage(msg, updated) },
                onDeleteMessage = { msg -> viewModel.deleteMessage(msg) },
                onTogglePinMessage = { msg -> viewModel.togglePinMessage(msg) },
                onForwardMessage = { msg, target -> viewModel.forwardMessage(msg, target) },
                onSendThreadReply = { content, quotedSender, quotedPreview ->
                    viewModel.sendThreadReply(content, quotedSender, quotedPreview)
                },
                onToggleFollowThread = { parent -> viewModel.toggleFollowThread(parent) },
                onToggleMuteThread = { parent -> viewModel.toggleMuteThread(parent) },
                onToggleThreadReplyReaction = { reply, emoji ->
                    viewModel.toggleThreadReplyReaction(reply, emoji)
                },
                onStartVoiceCall = {
                    viewModel.startCall(activeConv.id, activeConv.title, activeConv.avatarColor, false)
                },
                onStartVideoCall = {
                    viewModel.startCall(activeConv.id, activeConv.title, activeConv.avatarColor, true)
                },
                onReportTarget = { type, id, name, reason, details ->
                    viewModel.submitReport(type, id, name, reason, details)
                }
            )
            return@PaperMessagesTheme
        }

        // BackHandler for non-Chats bottom tabs returns to Chats tab
        if (currentSection != MainNavSection.CHATS) {
            BackHandler { currentSection = MainNavSection.CHATS }
        }

        AmbientLiquidBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                when (currentSection) {
                    MainNavSection.CHATS -> {
                        ChatsTabScreen(
                            userProfile = userProfile,
                            conversations = conversations,
                            searchQuery = searchQuery,
                            localSearchHits = localIndexedResults,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onOpenConversation = { viewModel.openConversation(it) },
                            onOpenProfileTab = { currentSection = MainNavSection.PROFILE },
                            onCreateConversation = { title, subtitle, type ->
                                viewModel.createNewConversation(title, subtitle, type)
                            },
                            onJoinByInviteCode = { code -> viewModel.joinByInviteCode(code) },
                            onTogglePin = { viewModel.togglePinConversation(it) },
                            onToggleMute = { viewModel.toggleMuteConversation(it) },
                            onToggleArchive = { viewModel.toggleArchiveConversation(it) },
                            onMarkUnread = { viewModel.markConversationUnread(it) },
                            onDeleteConversation = { viewModel.deleteConversation(it) }
                        )
                    }

                    MainNavSection.COMMUNITIES -> {
                        CommunitiesAndChannelsScreen(
                            communities = communities,
                            channels = channels,
                            onOpenLinkedConversation = { convId ->
                                currentSection = MainNavSection.CHATS
                                viewModel.openConversation(convId)
                            },
                            onToggleJoinCommunity = { viewModel.toggleJoinCommunity(it) },
                            onCreateCommunity = { name, handle, desc, cat, isPublic ->
                                viewModel.createNewCommunity(name, handle, desc, cat, isPublic)
                            },
                            onCreateChannel = { name, handle, desc, announce, pollQ, optA, optB, optC ->
                                viewModel.createNewChannel(name, handle, desc, announce, pollQ, optA, optB, optC)
                            },
                            onVoteChannelPoll = { chan, opt -> viewModel.voteInChannelPoll(chan, opt) },
                            onPostChannelAnnouncement = { chan, text ->
                                viewModel.postChannelAnnouncement(chan, text)
                            }
                        )
                    }

                    MainNavSection.CALLS -> {
                        CallsTabScreen(
                            calls = calls,
                            onStartCall = { convId, name, color, isVideo ->
                                viewModel.startCall(convId, name, color, isVideo)
                            }
                        )
                    }

                    MainNavSection.PROFILE -> {
                        ProfileAndSettingsScreen(
                            currentUserId = currentUserId,
                            initialDisplayName = initialDisplayName,
                            userProfile = userProfile,
                            darkTheme = darkTheme,
                            accentHex = accentHex,
                            onToggleDarkTheme = { viewModel.setThemeMode(it) },
                            onSelectAccentColor = { viewModel.setAccentColor(it) },
                            onSaveProfileAndPrivacy = { name, handle, bio, pOnline, pLastSeen, pReceipts, pPhone, rm ->
                                viewModel.updateProfileAndPrivacy(
                                    name, handle, bio, pOnline, pLastSeen, pReceipts, pPhone, rm
                                )
                            },
                            onRotateKeys = { viewModel.rotateIdentityKeys() },
                            onToggleBlockUser = { viewModel.toggleBlockUser(it) },
                            onSignOut = {
                                signOutPaperAccount(
                                    context = context,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            }
                        )
                    }
                }

                // Floating Status Toast Banner
                AnimatedVisibility(
                    visible = statusBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    LiquidGlassSurface(
                        intensity = GlassIntensity.LEVEL_3_ELEVATED,
                        shape = RoundedCornerShape(16.dp),
                        accentTint = PaperHighlight,
                        onClick = { viewModel.dismissBanner() }
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaperHighlight,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Floating Liquid Glass Capsule Bottom Navigation Bar (Part 4)
                val unreadChats = remember(conversations) {
                    conversations.sumOf { it.unreadCount.toInt() }
                }
                val unreadCommunities = remember(communities) {
                    communities.sumOf { it.unreadCount.toInt() }
                }

                FloatingLiquidGlassNavBar(
                    currentSection = currentSection,
                    unreadChatsCount = unreadChats,
                    unreadCommunitiesCount = unreadCommunities,
                    onSelectSection = { currentSection = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
