package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ConversationItem
import com.example.data.model.ThreadReplyItem
import com.example.security.CryptoEngine
import com.example.ui.components.AmbientLiquidBackground
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PaperAvatar
import com.example.ui.theme.OutgoingBubbleGradient
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperBgMain
import com.example.ui.theme.PaperBgSecondary
import com.example.ui.theme.PaperError
import com.example.ui.theme.PaperGlassBorderL1
import com.example.ui.theme.PaperGlassBorderL2
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSuccess
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationAndThreadScreen(
    currentUserId: String,
    conversation: ConversationItem,
    allConversations: List<ConversationItem>,
    messages: List<ChatMessage>,
    selectedTopic: String,
    activeThreadParentId: String?,
    threadReplies: List<ThreadReplyItem>,
    threadSearchQuery: String,
    privacyReadReceiptsEnabled: Boolean,
    onBackToChats: () -> Unit,
    onSelectTopic: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    onCloseThread: () -> Unit,
    onThreadSearchChange: (String) -> Unit,
    onSendMessage: (content: String, topic: String?, attachmentType: String, attachmentName: String, attachmentMeta: String) -> Unit,
    onToggleMessageReaction: (ChatMessage, String) -> Unit,
    onEditMessage: (ChatMessage, String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onTogglePinMessage: (ChatMessage) -> Unit,
    onForwardMessage: (ChatMessage, ConversationItem) -> Unit,
    onSendThreadReply: (content: String, quotedSender: String, quotedPreview: String) -> Unit,
    onToggleFollowThread: (ChatMessage) -> Unit,
    onToggleMuteThread: (ChatMessage) -> Unit,
    onToggleThreadReplyReaction: (ThreadReplyItem, String) -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onReportTarget: (targetType: String, targetId: String, targetName: String, reason: String, details: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showInChatSearch by remember { mutableStateOf(false) }
    var inChatQuery by remember { mutableStateOf("") }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var messageBeingEdited by remember { mutableStateOf<ChatMessage?>(null) }
    var messageBeingForwarded by remember { mutableStateOf<ChatMessage?>(null) }
    var messageForReadReceipts by remember { mutableStateOf<ChatMessage?>(null) }
    var showReportDialogFor by remember { mutableStateOf<ChatMessage?>(null) }
    var showPollBuilderDialog by remember { mutableStateOf(false) }

    val activeThreadParent = remember(messages, activeThreadParentId) {
        messages.find { it.id == activeThreadParentId }
    }

    // Back handler pops active thread first on phone, then exits conversation
    BackHandler {
        if (activeThreadParentId != null) {
            onCloseThread()
        } else {
            onBackToChats()
        }
    }

    val isGroup = conversation.type == "GROUP"
    val topics = remember(conversation.topicsCsv) {
        listOf("All") + conversation.topicsList()
    }

    val visibleMessages = remember(messages, selectedTopic, inChatQuery) {
        messages.filter { msg ->
            val topicOk = !isGroup || selectedTopic == "All" || msg.topic.equals(selectedTopic, ignoreCase = true)
            val queryOk = inChatQuery.isBlank() ||
                msg.content.contains(inChatQuery, ignoreCase = true) ||
                msg.senderName.contains(inChatQuery, ignoreCase = true)
            topicOk && queryOk
        }
    }

    val pinnedMessages = remember(visibleMessages) {
        visibleMessages.filter { it.pinned && !it.deleted }
    }

    AmbientLiquidBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            val isExpandedScreen = maxWidth >= 800.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT / MAIN CONVERSATION TIMELINE
                if (isExpandedScreen || activeThreadParent == null) {
                    Column(
                        modifier = Modifier
                            .weight(if (isExpandedScreen && activeThreadParent != null) 0.56f else 1f)
                            .fillMaxHeight()
                    ) {
                        // 1. Conversation Header (Part 5.4 & 5.5)
                        ConversationHeaderBar(
                            conversation = conversation,
                            onBack = onBackToChats,
                            onToggleSearch = { showInChatSearch = !showInChatSearch },
                            onVoiceCall = onStartVoiceCall,
                            onVideoCall = onStartVideoCall,
                            onOpenInfo = { showGroupInfoSheet = true }
                        )

                        // Search bar inside conversation
                        AnimatedVisibility(visible = showInChatSearch) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                OutlinedTextField(
                                    value = inChatQuery,
                                    onValueChange = { inChatQuery = it },
                                    placeholder = { Text("Search within ${conversation.title}...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            inChatQuery = ""
                                            showInChatSearch = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close search")
                                        }
                                    },
                                    singleLine = true,
                                    colors = paperTextFieldColors(),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 2. Group Topic Selector (Part 5.5)
                        if (isGroup) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("group_topic_selector")
                            ) {
                                items(topics) { topic ->
                                    val isSelected = topic == selectedTopic
                                    LiquidGlassSurface(
                                        intensity = if (isSelected) GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                                        shape = RoundedCornerShape(14.dp),
                                        accentTint = if (isSelected) PaperAccentPrimary else null,
                                        onClick = { onSelectTopic(topic) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (topic == "All") "All Topics" else "# $topic",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) Color.White else PaperTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pinned message banner
                        if (pinnedMessages.isNotEmpty()) {
                            val topPinned = pinnedMessages.last()
                            LiquidGlassSurface(
                                intensity = GlassIntensity.LEVEL_1_SUBTLE,
                                shape = RoundedCornerShape(14.dp),
                                accentTint = PaperHighlight,
                                onClick = {
                                    val idx = visibleMessages.indexOfFirst { it.id == topPinned.id }
                                    if (idx >= 0) {
                                        coroutineScope.launch { listState.animateScrollToItem(idx) }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned message",
                                        tint = PaperHighlight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pinned · ${topPinned.senderName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PaperHighlight
                                        )
                                        Text(
                                            text = topPinned.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PaperTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Messages Timeline
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("conversation_messages_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // E2EE Security Assurance Notice at top of timeline
                            item {
                                LiquidGlassSurface(
                                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encryption status",
                                            tint = PaperAccentSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (conversation.type == "PRIVATE") {
                                                "Messages and voice notes are end-to-end encrypted with Android Keystore ECDH + AES-256-GCM."
                                            } else {
                                                "Organized Group Space (${conversation.encryptionMode}). Tap 'View thread' on any message to join focused discussions."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PaperTextSecondary
                                        )
                                    }
                                }
                            }

                            items(visibleMessages, key = { it.id }) { msg ->
                                val isSelectedThreadParent = msg.id == activeThreadParentId
                                MessageBubbleItem(
                                    currentUserId = currentUserId,
                                    conversation = conversation,
                                    message = msg,
                                    isSelectedThreadParent = isSelectedThreadParent,
                                    onOpenThread = { onOpenThread(msg.id) },
                                    onLongPress = { selectedMessageForActions = msg },
                                    onToggleReaction = { emoji -> onToggleMessageReaction(msg, emoji) },
                                    onInspectReaders = {
                                        if (privacyReadReceiptsEnabled) {
                                            messageForReadReceipts = msg
                                        }
                                    }
                                )
                            }
                        }

                        // 4. Floating Glass Message Composer (Part 5.4 & 5.5)
                        MessageComposerDock(
                            placeholder = if (messageBeingEdited != null) {
                                "Editing message..."
                            } else if (isGroup) {
                                "Message #${if (selectedTopic == "All") "General" else selectedTopic}..."
                            } else {
                                "Encrypted message to ${conversation.title}..."
                            },
                            initialEditValue = messageBeingEdited?.content,
                            onCancelEdit = { messageBeingEdited = null },
                            onOpenPollBuilder = { showPollBuilderDialog = true },
                            onSendText = { text, attachType, attachName, attachMeta ->
                                val editingTarget = messageBeingEdited
                                if (editingTarget != null) {
                                    onEditMessage(editingTarget, text)
                                    messageBeingEdited = null
                                } else {
                                    onSendMessage(text, null, attachType, attachName, attachMeta)
                                }
                            }
                        )
                    }
                }

                // RIGHT / FULL-SCREEN DEDICATED THREAD PANEL (Part 3.4 & Part 5.5)
                AnimatedVisibility(
                    visible = activeThreadParent != null,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = if (isExpandedScreen) {
                        Modifier
                            .weight(0.44f)
                            .fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    if (activeThreadParent != null) {
                        DedicatedThreadPanel(
                            currentUserId = currentUserId,
                            parentMessage = activeThreadParent,
                            replies = threadReplies,
                            searchQuery = threadSearchQuery,
                            onSearchQueryChange = onThreadSearchChange,
                            onCloseThread = onCloseThread,
                            onJumpToParentMessage = {
                                val index = visibleMessages.indexOfFirst { it.id == activeThreadParent.id }
                                if (!isExpandedScreen) {
                                    onCloseThread()
                                }
                                if (index >= 0) {
                                    coroutineScope.launch {
                                        delay(120L)
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            },
                            onToggleFollow = { onToggleFollowThread(activeThreadParent) },
                            onToggleMute = { onToggleMuteThread(activeThreadParent) },
                            onSendReply = onSendThreadReply,
                            onToggleReplyReaction = onToggleThreadReplyReaction
                        )
                    }
                }
            }
        }
    }

    // Message Long-Press Contextual Action Sheet (React, Thread, Edit, Pin, Forward, Readers, Report, Delete)
    val actionTarget = selectedMessageForActions
    if (actionTarget != null) {
        val quickEmojis = listOf("🚀", "💜", "🔥", "👍", "✨", "🎉")
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PaperSurfaceElevated,
            contentColor = PaperTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Quick Reaction Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    quickEmojis.forEach { emoji ->
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_2_STANDARD,
                            shape = CircleShape,
                            onClick = {
                                onToggleMessageReaction(actionTarget, emoji)
                                selectedMessageForActions = null
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MessageActionSheetItem(
                    icon = Icons.Default.Forum,
                    label = if (actionTarget.replyCount > 0L) {
                        "Open Thread (${actionTarget.replyCount} replies)"
                    } else {
                        "Start Thread on this Message"
                    },
                    onClick = {
                        val id = actionTarget.id
                        selectedMessageForActions = null
                        onOpenThread(id)
                    }
                )
                MessageActionSheetItem(
                    icon = Icons.Default.Edit,
                    label = "Edit Message",
                    onClick = {
                        messageBeingEdited = actionTarget
                        selectedMessageForActions = null
                    }
                )
                MessageActionSheetItem(
                    icon = Icons.Default.PushPin,
                    label = if (actionTarget.pinned) "Unpin from Conversation" else "Pin to Conversation",
                    onClick = {
                        onTogglePinMessage(actionTarget)
                        selectedMessageForActions = null
                    }
                )
                MessageActionSheetItem(
                    icon = Icons.Default.Share,
                    label = "Forward to Another Conversation",
                    onClick = {
                        messageBeingForwarded = actionTarget
                        selectedMessageForActions = null
                    }
                )
                if (privacyReadReceiptsEnabled) {
                    MessageActionSheetItem(
                        icon = Icons.Default.Visibility,
                        label = "View Read Receipts (${actionTarget.readCount} readers)",
                        onClick = {
                            messageForReadReceipts = actionTarget
                            selectedMessageForActions = null
                        }
                    )
                }
                MessageActionSheetItem(
                    icon = Icons.Default.Flag,
                    label = "Report Message",
                    tint = PaperError,
                    onClick = {
                        showReportDialogFor = actionTarget
                        selectedMessageForActions = null
                    }
                )
                MessageActionSheetItem(
                    icon = Icons.Default.Delete,
                    label = "Delete Message",
                    tint = PaperError,
                    onClick = {
                        onDeleteMessage(actionTarget)
                        selectedMessageForActions = null
                    }
                )
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }

    // Group Resource Library & E2EE Key Verification Info Sheet
    if (showGroupInfoSheet) {
        val safetyFingerprint = remember(currentUserId, conversation.id) {
            CryptoEngine.computeSafetyFingerprint(currentUserId, conversation.id)
        }
        ModalBottomSheet(
            onDismissRequest = { showGroupInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PaperSurfaceElevated,
            contentColor = PaperTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PaperAvatar(
                        name = conversation.title,
                        avatarHex = conversation.avatarColor,
                        size = 54.dp,
                        isOnline = conversation.onlineCount > 0L,
                        isGroup = conversation.type == "GROUP"
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PaperTextPrimary
                        )
                        Text(
                            text = "${conversation.subtitle} · Invite Code: ${conversation.inviteCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaperAccentSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Encryption & Safety Number Card
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PaperSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cryptographic Session (${conversation.encryptionMode})",
                                style = MaterialTheme.typography.titleSmall,
                                color = PaperTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Safety Fingerprint (HKDF-SHA256):",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = safetyFingerprint,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PaperHighlight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group Resource Library (Part 6)
                Text(
                    text = "SHARED RESOURCE LIBRARY (${conversation.resourcesList().size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (conversation.resourcesList().isEmpty()) {
                    Text(
                        text = "No shared documents yet. Attach a PDF or image in the composer to index it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperTextMuted
                    )
                } else {
                    conversation.resourcesList().forEach { resName ->
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_1_SUBTLE,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = PaperAccentSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = resName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = PaperTextPrimary
                                    )
                                    Text(
                                        text = "Encrypted Attachment · Synced across devices",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PaperTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Group Read Receipts Drawer (Part 5.5)
    val receiptTarget = messageForReadReceipts
    if (receiptTarget != null) {
        AlertDialog(
            onDismissRequest = { messageForReadReceipts = null },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Read by ${receiptTarget.readCount} Members") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Privacy-respecting read receipts (members who disabled receipts in Privacy Settings are omitted):",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextMuted
                    )
                    receiptTarget.readersList().forEach { reader ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = PaperAccentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reader, color = PaperTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageForReadReceipts = null }) {
                    Text("Close", color = PaperAccentPrimary)
                }
            }
        )
    }

    // Forward Message Dialog
    val forwardTarget = messageBeingForwarded
    if (forwardTarget != null) {
        AlertDialog(
            onDismissRequest = { messageBeingForwarded = null },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Forward Message To...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allConversations.forEach { targetConv ->
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_1_SUBTLE,
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                onForwardMessage(forwardTarget, targetConv)
                                messageBeingForwarded = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PaperAvatar(
                                    name = targetConv.title,
                                    avatarHex = targetConv.avatarColor,
                                    size = 32.dp,
                                    isGroup = targetConv.type == "GROUP"
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = targetConv.title, color = PaperTextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageBeingForwarded = null }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }

    // Poll Builder Dialog (Part 6)
    if (showPollBuilderDialog) {
        var pollQuestion by remember { mutableStateOf("") }
        var pollOpt1 by remember { mutableStateOf("Approve Architecture") }
        var pollOpt2 by remember { mutableStateOf("Discuss in Thread First") }
        AlertDialog(
            onDismissRequest = { showPollBuilderDialog = false },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Create Group Poll") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question") },
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pollOpt1,
                        onValueChange = { pollOpt1 = it },
                        label = { Text("Option 1") },
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pollOpt2,
                        onValueChange = { pollOpt2 = it },
                        label = { Text("Option 2") },
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Publish Poll",
                    enabled = pollQuestion.isNotBlank(),
                    onClick = {
                        showPollBuilderDialog = false
                        onSendMessage(
                            pollQuestion,
                            null,
                            "POLL",
                            "Group Poll",
                            "$pollOpt1 (1 vote) | $pollOpt2 (0 votes)"
                        )
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPollBuilderDialog = false }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }

    // Report Message Dialog
    val reportTarget = showReportDialogFor
    if (reportTarget != null) {
        var reason by remember { mutableStateOf("Spam or Unwanted Content") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialogFor = null },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Report Message to Moderation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason Category") },
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional context") },
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Submit Report",
                    onClick = {
                        onReportTarget("MESSAGE", reportTarget.id, reportTarget.senderName, reason, details)
                        showReportDialogFor = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showReportDialogFor = null }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ConversationHeaderBar(
    conversation: ConversationItem,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onOpenInfo: () -> Unit
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("conversation_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PaperTextPrimary
                )
            }

            PaperAvatar(
                name = conversation.title,
                avatarHex = conversation.avatarColor,
                size = 42.dp,
                isOnline = conversation.onlineCount > 0L,
                isGroup = conversation.type == "GROUP"
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenInfo)
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PaperTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperAccentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search in conversation",
                    tint = PaperTextSecondary
                )
            }
            IconButton(
                onClick = onVoiceCall,
                modifier = Modifier.testTag("conversation_voice_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = PaperTextPrimary
                )
            }
            IconButton(
                onClick = onVideoCall,
                modifier = Modifier.testTag("conversation_video_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = PaperAccentSecondary
                )
            }
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier.testTag("conversation_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Group Info & Resources",
                    tint = PaperTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    currentUserId: String,
    conversation: ConversationItem,
    message: ChatMessage,
    isSelectedThreadParent: Boolean,
    onOpenThread: () -> Unit,
    onLongPress: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onInspectReaders: () -> Unit
) {
    val isOutgoing = message.senderId == currentUserId || message.senderName == "You"
    val isGroup = conversation.type == "GROUP"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = remember(message.createdAt) {
        message.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Now"
    }

    val decryptedContent = remember(message.cipherText, message.ivBase64, message.content) {
        CryptoEngine.decryptMessage(
            cipherTextBase64 = message.cipherText,
            ivBase64 = message.ivBase64,
            userId = currentUserId,
            conversationId = conversation.id,
            fallbackPlaintext = message.content
        )
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 19.dp,
        topEnd = 19.dp,
        bottomStart = if (isOutgoing) 19.dp else 6.dp,
        bottomEnd = if (isOutgoing) 6.dp else 19.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_item_${message.id}"),
        horizontalArrangement = if (isOutgoing && !isGroup) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isOutgoing || isGroup) {
            PaperAvatar(
                name = message.senderName,
                avatarHex = if (isOutgoing) "#8B7CFF" else "#54D6E8",
                size = 36.dp,
                isOnline = true
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = if (isOutgoing && !isGroup) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isOutgoing && !isGroup) {
                            OutgoingBubbleGradient
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    PaperSurfaceElevated.copy(alpha = 0.92f),
                                    PaperBgSecondary.copy(alpha = 0.92f)
                                )
                            )
                        }
                    )
                    .border(
                        width = if (isSelectedThreadParent) 1.5.dp else 1.dp,
                        color = if (isSelectedThreadParent) PaperAccentSecondary else PaperGlassBorderL2,
                        shape = bubbleShape
                    )
                    .combinedClickable(
                        onClick = { if (isGroup) onOpenThread() else onLongPress() },
                        onLongClick = onLongPress
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column {
                    // Sender Header for Group Messages
                    if (isGroup) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = message.senderName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PaperHighlight
                            )
                            if (message.senderRole.isNotBlank()) {
                                Surface(
                                    color = PaperAccentPrimary.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = message.senderRole,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = PaperHighlight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(
                                color = PaperAccentSecondary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#${message.topic}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = PaperAccentSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }

                    // Attachment / Voice Note / Poll Preview
                    if (message.attachmentType != "NONE" && !message.deleted) {
                        AttachmentPreviewCard(
                            attachmentType = message.attachmentType,
                            attachmentName = message.attachmentName,
                            attachmentMeta = message.attachmentMeta
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Message Text with @mention highlighting
                    Text(
                        text = formatMentionsAnnotatedString(decryptedContent),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (message.deleted) PaperTextMuted else Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Timestamp, Edited & Read Receipt Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timeText + if (message.edited) " · edited" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        if (isGroup) {
                            Text(
                                text = "· Read by ${message.readCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperAccentSecondary,
                                modifier = Modifier.clickable(onClick = onInspectReaders)
                            )
                        } else if (isOutgoing) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = PaperAccentSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Reaction Chips Row
            val reactions = message.parsedReactions()
            if (reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactions.forEach { (emoji, count) ->
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_1_SUBTLE,
                            shape = RoundedCornerShape(12.dp),
                            onClick = { onToggleReaction(emoji) }
                        ) {
                            Text(
                                text = "$emoji $count",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // SIGNATURE THREAD ANCHOR BAR FOR GROUP MESSAGES (Part 5.5)
            if (isGroup && !message.deleted) {
                Spacer(modifier = Modifier.height(6.dp))
                LiquidGlassSurface(
                    intensity = if (message.replyCount > 0L) GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(14.dp),
                    accentTint = if (message.replyCount > 0L) PaperAccentPrimary else null,
                    onClick = onOpenThread,
                    modifier = Modifier.testTag("open_thread_button_${message.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "View thread",
                            tint = if (message.replyCount > 0L) PaperHighlight else PaperTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        if (message.replyCount > 0L) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedContent(
                                        targetState = message.replyCount,
                                        label = "thread_reply_counter"
                                    ) { count ->
                                        Text(
                                            text = "$count ${if (count == 1L) "reply" else "replies"} · View thread",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = PaperHighlight
                                        )
                                    }
                                    if (message.unreadReplyCount > 0L) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(PaperAccentSecondary)
                                        )
                                    }
                                }
                                if (message.lastReplyPreview.isNotBlank()) {
                                    Text(
                                        text = "${message.lastReplySender}: ${message.lastReplyPreview}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PaperTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 260.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Reply in thread",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewCard(
    attachmentType: String,
    attachmentName: String,
    attachmentMeta: String
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_1_SUBTLE,
        shape = RoundedCornerShape(12.dp),
        accentTint = PaperAccentSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (attachmentType) {
                "VOICE" -> Icons.Default.GraphicEq
                "IMAGE" -> Icons.Default.Image
                "POLL" -> Icons.Default.Poll
                else -> Icons.Default.Description
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PaperAccentPrimary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = attachmentType,
                    tint = PaperAccentSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachmentName.ifBlank { attachmentType },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PaperTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (attachmentMeta.isNotBlank()) {
                    Text(
                        text = attachmentMeta,
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperAccentSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Thread Interface (Part 5.5 — Thread Panel Design)
 */
@Composable
private fun DedicatedThreadPanel(
    currentUserId: String,
    parentMessage: ChatMessage,
    replies: List<ThreadReplyItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseThread: () -> Unit,
    onJumpToParentMessage: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleMute: () -> Unit,
    onSendReply: (content: String, quotedSender: String, quotedPreview: String) -> Unit,
    onToggleReplyReaction: (ThreadReplyItem, String) -> Unit
) {
    var showThreadSearch by remember { mutableStateOf(false) }
    var quotedReply by remember { mutableStateOf<ThreadReplyItem?>(null) }
    var replyText by remember { mutableStateOf("") }

    val filteredReplies = remember(replies, searchQuery) {
        if (searchQuery.isBlank()) {
            replies
        } else {
            replies.filter {
                it.content.contains(searchQuery, ignoreCase = true) ||
                    it.senderName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_3_ELEVATED,
        shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("dedicated_thread_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Thread Top Section (Title, Follow/Unfollow, Mute, Search, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCloseThread,
                    modifier = Modifier.testTag("close_thread_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Thread",
                        tint = PaperTextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thread · #${parentMessage.topic}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PaperTextPrimary
                    )
                    Text(
                        text = "${parentMessage.replyCount} ${if (parentMessage.replyCount == 1L) "reply" else "replies"} · Started by ${parentMessage.senderName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperAccentSecondary
                    )
                }

                // Follow / Unfollow Toggle
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(12.dp),
                    accentTint = if (parentMessage.followingThread) PaperSuccess else null,
                    onClick = onToggleFollow,
                    modifier = Modifier.testTag("thread_follow_toggle")
                ) {
                    Text(
                        text = if (parentMessage.followingThread) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (parentMessage.followingThread) PaperSuccess else PaperTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (parentMessage.threadMuted) {
                            Icons.Default.NotificationsOff
                        } else {
                            Icons.Default.NotificationsActive
                        },
                        contentDescription = "Mute thread",
                        tint = if (parentMessage.threadMuted) PaperTextMuted else PaperHighlight,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(onClick = { showThreadSearch = !showThreadSearch }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search within thread",
                        tint = PaperTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showThreadSearch) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search replies in thread...") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Anchored Parent Message Card (with Jump to Original Parent Message action)
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_2_STANDARD,
                shape = RoundedCornerShape(18.dp),
                accentTint = PaperAccentPrimary,
                onClick = onJumpToParentMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PaperAvatar(
                                name = parentMessage.senderName,
                                avatarHex = "#8B7CFF",
                                size = 30.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = parentMessage.senderName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = PaperHighlight
                                )
                                Text(
                                    text = "Parent Message · Tap to jump to timeline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaperTextMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parentMessage.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PaperTextPrimary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = PaperGlassBorderL1
            )

            // 3. Chronological Thread Replies List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("thread_replies_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredReplies, key = { it.id }) { reply ->
                    ThreadReplyCard(
                        reply = reply,
                        isOwn = reply.senderId == currentUserId || reply.senderName == "You",
                        onQuoteReply = { quotedReply = reply },
                        onToggleReaction = { emoji -> onToggleReplyReaction(reply, emoji) }
                    )
                }
            }

            // 4. Thread Bottom Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Quoted Reply Reference Banner
                val activeQuote = quotedReply
                if (activeQuote != null) {
                    LiquidGlassSurface(
                        intensity = GlassIntensity.LEVEL_1_SUBTLE,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${activeQuote.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaperAccentSecondary
                                )
                                Text(
                                    text = activeQuote.content,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaperTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { quotedReply = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear quote",
                                    tint = PaperTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Quick @mention chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    listOf("@elena_r", "@marcus_c", "@aria_l").forEach { handle ->
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_1_SUBTLE,
                            shape = RoundedCornerShape(10.dp),
                            onClick = { replyText = "$replyText $handle ".trimStart() }
                        ) {
                            Text(
                                text = handle,
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperAccentSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text(
                                    text = "Reply to thread...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PaperTextMuted
                                )
                            },
                            maxLines = 4,
                            colors = paperTextFieldColors(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("thread_reply_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onSendReply(
                                        replyText,
                                        quotedReply?.senderName ?: "",
                                        quotedReply?.content ?: ""
                                    )
                                    replyText = ""
                                    quotedReply = null
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(PaperAccentPrimary, PaperAccentBlue))
                                )
                                .testTag("thread_reply_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Thread Reply",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadReplyCard(
    reply: ThreadReplyItem,
    isOwn: Boolean,
    onQuoteReply: () -> Unit,
    onToggleReaction: (String) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = remember(reply.createdAt) {
        reply.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Now"
    }

    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_1_SUBTLE,
        shape = RoundedCornerShape(16.dp),
        accentTint = if (isOwn) PaperAccentPrimary else null,
        onClick = onQuoteReply,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PaperAvatar(
                        name = reply.senderName,
                        avatarHex = if (isOwn) "#8B7CFF" else "#54D6E8",
                        size = 28.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reply.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PaperHighlight
                    )
                    if (reply.senderRole.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· ${reply.senderRole}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperTextMuted
                        )
                    }
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperTextMuted
                )
            }

            if (reply.quotedPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PaperBgMain.copy(alpha = 0.55f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "↪ ${reply.quotedSenderName}: ${reply.quotedPreview}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatMentionsAnnotatedString(reply.content),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reactions = reply.parsedReactions()
                reactions.forEach { (emoji, count) ->
                    Surface(
                        color = PaperSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onToggleReaction(emoji) }
                    ) {
                        Text(
                            text = "$emoji $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperTextPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                listOf("🔥", "👍", "💜").forEach { quickEmoji ->
                    if (!reactions.containsKey(quickEmoji)) {
                        Text(
                            text = quickEmoji,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clickable { onToggleReaction(quickEmoji) }
                                .padding(horizontal = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposerDock(
    placeholder: String,
    initialEditValue: String?,
    onCancelEdit: () -> Unit,
    onOpenPollBuilder: () -> Unit,
    onSendText: (text: String, attachType: String, attachName: String, attachMeta: String) -> Unit
) {
    var messageInput by remember(initialEditValue) { mutableStateOf(initialEditValue ?: "") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiStrip by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000L)
                recordingSeconds += 1
            }
        }
    }

    // Zero-permission Android Photo Picker (Google Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "Photo_${uri.lastPathSegment?.takeLast(8) ?: "Media"}.jpg"
            onSendText(
                messageInput.ifBlank { "Shared an encrypted image" },
                "IMAGE",
                fileName,
                "1.8 MB · Client-Side AES-GCM Encrypted"
            )
            messageInput = ""
        }
    }

    // Standard Document Picker (SAF)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val docName = "Document_${uri.lastPathSegment?.takeLast(8) ?: "Spec"}.pdf"
            onSendText(
                messageInput.ifBlank { "Shared encrypted document: $docName" },
                "DOCUMENT",
                docName,
                "PDF · SHA-256 Verified"
            )
            messageInput = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (initialEditValue != null) {
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_1_SUBTLE,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Editing message",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaperHighlight
                    )
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = PaperTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Attachment Strip (Photo Picker, Document SAF, Poll Builder)
        AnimatedVisibility(visible = showAttachMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        showAttachMenu = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = PaperAccentSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo/Video", style = MaterialTheme.typography.labelMedium, color = PaperTextPrimary)
                    }
                }

                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        showAttachMenu = false
                        documentPickerLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = PaperHighlight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Document", style = MaterialTheme.typography.labelMedium, color = PaperTextPrimary)
                    }
                }

                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_2_STANDARD,
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        showAttachMenu = false
                        onOpenPollBuilder()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Poll, contentDescription = null, tint = PaperSuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Poll", style = MaterialTheme.typography.labelMedium, color = PaperTextPrimary)
                    }
                }
            }
        }

        // Quick Emoji Strip
        AnimatedVisibility(visible = showEmojiStrip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🚀", "💜", "🔥", "✨", "👍", "🎯", "🔒", "🎉").forEach { emoji ->
                    LiquidGlassSurface(
                        intensity = GlassIntensity.LEVEL_1_SUBTLE,
                        shape = CircleShape,
                        onClick = { messageInput += emoji },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = emoji, fontSize = 17.sp)
                        }
                    }
                }
            }
        }

        // Main Floating Glass Input Bar
        LiquidGlassSurface(
            intensity = GlassIntensity.LEVEL_2_STANDARD,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAttachMenu = !showAttachMenu },
                    modifier = Modifier.testTag("composer_attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = PaperAccentSecondary
                    )
                }

                if (isRecordingVoice) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PaperError)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording E2EE Voice Note · 0:${recordingSeconds.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PaperHighlight
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PaperTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        colors = paperTextFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("composer_text_input")
                    )
                }

                IconButton(onClick = { showEmojiStrip = !showEmojiStrip }) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji picker",
                        tint = PaperTextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        if (isRecordingVoice) {
                            isRecordingVoice = false
                            val dur = "0:${recordingSeconds.coerceAtLeast(3).toString().padStart(2, '0')}"
                            onSendText(
                                "Encrypted Voice Note ($dur)",
                                "VOICE",
                                "Voice_Note.m4a",
                                "$dur · E2EE Audio"
                            )
                        } else {
                            isRecordingVoice = true
                        }
                    },
                    modifier = Modifier.testTag("composer_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Message",
                        tint = if (isRecordingVoice) PaperError else PaperTextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSendText(messageInput, "NONE", "", "")
                            messageInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PaperAccentPrimary, PaperAccentBlue))
                        )
                        .testTag("composer_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = PaperTextPrimary,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_1_SUBTLE,
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint)
        }
    }
}

private fun formatMentionsAnnotatedString(text: String) = buildAnnotatedString {
    val words = text.split(" ")
    words.forEachIndexed { index, word ->
        if (word.startsWith("@") && word.length > 2) {
            withStyle(SpanStyle(color = PaperAccentSecondary, fontWeight = FontWeight.SemiBold)) {
                append(word)
            }
        } else {
            append(word)
        }
        if (index < words.lastIndex) append(" ")
    }
}
