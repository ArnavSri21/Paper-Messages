package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LocalIndexedMessage
import com.example.data.model.ConversationItem
import com.example.data.model.UserProfile
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PaperAvatar
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperError
import com.example.ui.theme.PaperGlassBorderL1
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    userProfile: UserProfile?,
    conversations: List<ConversationItem>,
    searchQuery: String,
    localSearchHits: List<LocalIndexedMessage>,
    onSearchQueryChange: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenProfileTab: () -> Unit,
    onCreateConversation: (title: String, subtitle: String, type: String) -> Unit,
    onJoinByInviteCode: (String) -> Unit,
    onTogglePin: (ConversationItem) -> Unit,
    onToggleMute: (ConversationItem) -> Unit,
    onToggleArchive: (ConversationItem) -> Unit,
    onMarkUnread: (ConversationItem) -> Unit,
    onDeleteConversation: (ConversationItem) -> Unit
) {
    var showArchived by remember { mutableStateOf(false) }
    var contextMenuTarget by remember { mutableStateOf<ConversationItem?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf<String?>(null) } // "PRIVATE", "GROUP", "INVITE"

    val filteredConversations = remember(conversations, searchQuery, showArchived) {
        val q = searchQuery.trim().lowercase()
        conversations.filter { conv ->
            val archiveMatches = if (showArchived) conv.archived else !conv.archived
            val queryMatches = q.isEmpty() ||
                conv.title.lowercase().contains(q) ||
                conv.subtitle.lowercase().contains(q) ||
                conv.lastMessagePreview.lowercase().contains(q) ||
                conv.lastMessageSender.lowercase().contains(q) ||
                conv.inviteCode.lowercase().contains(q)
            archiveMatches && queryMatches
        }
    }

    val pinnedConversations = remember(filteredConversations) {
        filteredConversations.filter { it.pinned }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chats_conversation_list"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. Header (Part 5.3)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem_1793896222776),
                            contentDescription = "PaperMessages logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PaperMessages",
                                style = MaterialTheme.typography.headlineSmall,
                                color = PaperTextPrimary
                            )
                            Text(
                                text = "Encrypted & Threaded Workspace",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaperAccentSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassSurface(
                            intensity = GlassIntensity.LEVEL_1_SUBTLE,
                            shape = CircleShape,
                            onClick = { showFabMenu = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("header_new_conversation_button")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "New Conversation",
                                    tint = PaperTextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .combinedClickable(onClick = onOpenProfileTab)
                                .testTag("header_profile_avatar")
                        ) {
                            PaperAvatar(
                                name = userProfile?.displayName?.ifBlank { "You" } ?: "You",
                                avatarHex = userProfile?.avatarColor ?: "#8B7CFF",
                                size = 42.dp,
                                isOnline = true
                            )
                        }
                    }
                }
            }

            // 2. Floating Glass Search Component (Part 5.3)
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Search messages, people and conversations...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PaperTextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = PaperAccentSecondary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = PaperTextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = paperTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chats_search_input")
                    )
                }
            }

            // Local E2EE Decrypted Message Search Results (Part 6)
            if (searchQuery.trim().length >= 2 && localSearchHits.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "ENCRYPTED LOCAL INDEX MATCHES (${localSearchHits.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperHighlight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        localSearchHits.take(5).forEach { hit ->
                            LiquidGlassSurface(
                                intensity = GlassIntensity.LEVEL_1_SUBTLE,
                                shape = RoundedCornerShape(14.dp),
                                onClick = { onOpenConversation(hit.conversationId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${hit.conversationTitle} · #${hit.topic}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = PaperAccentSecondary
                                        )
                                        Text(
                                            text = hit.senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PaperTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = hit.decryptedPlaintext,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PaperTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Pinned Conversations Horizontal Carousel (Part 5.3)
            if (pinnedConversations.isNotEmpty() && !showArchived) {
                item {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = PaperHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED SPACES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaperTextSecondary
                                )
                            }
                            if (conversations.any { it.archived }) {
                                TextButton(onClick = { showArchived = !showArchived }) {
                                    Text(
                                        text = "Archived (${conversations.count { it.archived }})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PaperAccentSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pinnedConversations, key = { "pin_${it.id}" }) { conv ->
                                PinnedConversationCard(
                                    conversation = conv,
                                    onClick = { onOpenConversation(conv.id) }
                                )
                            }
                        }
                    }
                }
            } else if (conversations.any { it.archived }) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showArchived = !showArchived }) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Toggle archived",
                                tint = PaperAccentSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showArchived) "Back to Active Chats" else "View Archived (${conversations.count { it.archived }})",
                                style = MaterialTheme.typography.labelMedium,
                                color = PaperAccentSecondary
                            )
                        }
                    }
                }
            }

            // 4. Conversation List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showArchived) "ARCHIVED CONVERSATIONS" else "ALL CONVERSATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary
                    )
                    Text(
                        text = "Long-press for options",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextMuted
                    )
                }
            }

            // 5. Conversation List Rows (Part 5.3)
            items(filteredConversations, key = { it.id }) { conv ->
                ConversationListRow(
                    conversation = conv,
                    onClick = { onOpenConversation(conv.id) },
                    onLongClick = { contextMenuTarget = conv }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 84.dp, end = 20.dp),
                    thickness = 0.5.dp,
                    color = PaperGlassBorderL1
                )
            }
        }

        // 6. Floating Glass Action Button (Part 5.3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
        ) {
            LiquidGlassSurface(
                intensity = GlassIntensity.LEVEL_3_ELEVATED,
                shape = RoundedCornerShape(20.dp),
                accentTint = PaperAccentPrimary,
                onClick = { showFabMenu = true },
                modifier = Modifier
                    .size(58.dp)
                    .testTag("chats_fab_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    PaperAccentPrimary.copy(alpha = 0.85f),
                                    PaperAccentBlue.copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Conversation Action",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Contextual Long-Press Action Sheet (Pin, Mute, Archive, Mark as Unread, Delete)
    val selectedTarget = contextMenuTarget
    if (selectedTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { contextMenuTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PaperSurfaceElevated,
            contentColor = PaperTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = selectedTarget.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PaperTextPrimary
                )
                Text(
                    text = selectedTarget.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                ContextActionRow(
                    icon = Icons.Default.PushPin,
                    label = if (selectedTarget.pinned) "Unpin conversation" else "Pin conversation",
                    onClick = {
                        onTogglePin(selectedTarget)
                        contextMenuTarget = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.NotificationsOff,
                    label = if (selectedTarget.muted) "Unmute notifications" else "Mute notifications",
                    onClick = {
                        onToggleMute(selectedTarget)
                        contextMenuTarget = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.Archive,
                    label = if (selectedTarget.archived) "Unarchive conversation" else "Archive conversation",
                    onClick = {
                        onToggleArchive(selectedTarget)
                        contextMenuTarget = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.MarkChatUnread,
                    label = if (selectedTarget.unreadCount > 0L) "Mark as read" else "Mark as unread",
                    onClick = {
                        onMarkUnread(selectedTarget)
                        contextMenuTarget = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete conversation",
                    tint = PaperError,
                    onClick = {
                        onDeleteConversation(selectedTarget)
                        contextMenuTarget = null
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // FAB Menu Sheet (New Private Chat, Create Group, Join Using Invitation Link)
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PaperSurfaceElevated,
            contentColor = PaperTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Start Communicating",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PaperTextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                ContextActionRow(
                    icon = Icons.Default.PersonAdd,
                    label = "New Private Chat (1:1 E2EE)",
                    onClick = {
                        showFabMenu = false
                        dialogMode = "PRIVATE"
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.GroupAdd,
                    label = "Create Threaded Group",
                    onClick = {
                        showFabMenu = false
                        dialogMode = "GROUP"
                    }
                )
                ContextActionRow(
                    icon = Icons.Default.Link,
                    label = "Join Using Invitation Link / Code",
                    onClick = {
                        showFabMenu = false
                        dialogMode = "INVITE"
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Creation / Invite Dialogs
    if (dialogMode != null) {
        var inputTitle by remember { mutableStateOf("") }
        var inputSubtitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { dialogMode = null },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = {
                Text(
                    when (dialogMode) {
                        "PRIVATE" -> "New Encrypted Private Chat"
                        "GROUP" -> "Create Threaded Group Space"
                        else -> "Join via Invitation Code"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = {
                            Text(
                                when (dialogMode) {
                                    "PRIVATE" -> "Contact Name or @username"
                                    "GROUP" -> "Group Name"
                                    else -> "Invite Code (e.g., PAPER-CORE-01)"
                                }
                            )
                        },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (dialogMode != "INVITE") {
                        OutlinedTextField(
                            value = inputSubtitle,
                            onValueChange = { inputSubtitle = it },
                            label = {
                                Text(
                                    if (dialogMode == "GROUP") "Description / Purpose" else "Verification Note"
                                )
                            },
                            singleLine = true,
                            colors = paperTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                LiquidGlassButton(
                    text = if (dialogMode == "INVITE") "Join Space" else "Create",
                    enabled = inputTitle.isNotBlank(),
                    onClick = {
                        val mode = dialogMode
                        dialogMode = null
                        if (mode == "INVITE") {
                            onJoinByInviteCode(inputTitle)
                        } else if (mode != null) {
                            onCreateConversation(inputTitle, inputSubtitle, mode)
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { dialogMode = null }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PinnedConversationCard(
    conversation: ConversationItem,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = Modifier
            .width(142.dp)
            .height(106.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PaperAvatar(
                    name = conversation.title,
                    avatarHex = conversation.avatarColor,
                    size = 40.dp,
                    isOnline = conversation.onlineCount > 0L,
                    isGroup = conversation.type == "GROUP"
                )
                if (conversation.unreadCount > 0L) {
                    Surface(
                        color = PaperAccentPrimary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PaperTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (conversation.type == "GROUP") "Threaded Group" else "E2EE Private",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = PaperTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationListRow(
    conversation: ConversationItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timestampText = remember(conversation.updatedAt, conversation.createdAt) {
        val ts = conversation.updatedAt ?: conversation.createdAt
        if (ts != null) timeFormatter.format(ts.toDate()) else "Now"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 13.dp)
            .testTag("conversation_row_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaperAvatar(
            name = conversation.title,
            avatarHex = conversation.avatarColor,
            size = 52.dp,
            isOnline = conversation.onlineCount > 0L,
            isGroup = conversation.type == "GROUP"
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (conversation.encryptionMode.startsWith("E2EE")) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = PaperAccentSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PaperTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.muted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = PaperTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timestampText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversation.unreadCount > 0L) PaperHighlight else PaperTextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (conversation.type == "GROUP" && conversation.lastMessageSender.isNotBlank()) {
                        "${conversation.lastMessageSender}: ${conversation.lastMessagePreview}"
                    } else {
                        conversation.lastMessagePreview
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0L) PaperTextPrimary else PaperTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (conversation.unreadCount > 0L) {
                    Surface(
                        color = PaperAccentPrimary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = PaperAccentSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextActionRow(
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = tint
            )
        }
    }
}
