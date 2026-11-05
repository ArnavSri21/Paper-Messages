package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelItem
import com.example.data.model.CommunityItem
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PaperAvatar
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSuccess
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary

@Composable
fun CommunitiesAndChannelsScreen(
    communities: List<CommunityItem>,
    channels: List<ChannelItem>,
    onOpenLinkedConversation: (String) -> Unit,
    onToggleJoinCommunity: (CommunityItem) -> Unit,
    onCreateCommunity: (name: String, handle: String, description: String, category: String, isPublic: Boolean) -> Unit,
    onVoteChannelPoll: (ChannelItem, String) -> Unit,
    onPostChannelAnnouncement: (ChannelItem, String) -> Unit
) {
    var activeSubTab by remember { mutableStateOf("COMMUNITIES") } // COMMUNITIES or CHANNELS
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }
    var channelForPostDialog by remember { mutableStateOf<ChannelItem?>(null) }

    val categories = remember { listOf("All", "Technology", "Security", "Design", "Startups") }

    val joinedCommunities = remember(communities) {
        communities.filter { it.joined && !it.pendingInvite }
    }
    val pendingInvites = remember(communities) {
        communities.filter { it.pendingInvite }
    }
    val discoverCommunities = remember(communities, selectedCategory) {
        communities.filter { comm ->
            !comm.pendingInvite && (selectedCategory == "All" || comm.category.equals(selectedCategory, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("communities_and_channels_list"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. Header (Part 5.6 & 5.7)
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
                        text = if (activeSubTab == "COMMUNITIES") "Communities" else "Broadcast Channels",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PaperTextPrimary
                    )
                    Text(
                        text = if (activeSubTab == "COMMUNITIES") {
                            "Structured multi-topic spaces & invitations"
                        } else {
                            "High-signal announcements & live community polls"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperAccentSecondary
                    )
                }

                LiquidGlassButton(
                    text = "Create",
                    icon = Icons.Default.Add,
                    onClick = { showCreateCommunityDialog = true },
                    testTag = "create_community_button"
                )
            }
        }

        // 2. Segmented Switcher: Communities vs Broadcast Channels
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassSurface(
                    intensity = if (activeSubTab == "COMMUNITIES") GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(16.dp),
                    accentTint = if (activeSubTab == "COMMUNITIES") PaperAccentPrimary else null,
                    onClick = { activeSubTab = "COMMUNITIES" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_communities")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Communities (${communities.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }

                LiquidGlassSurface(
                    intensity = if (activeSubTab == "CHANNELS") GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(16.dp),
                    accentTint = if (activeSubTab == "CHANNELS") PaperAccentSecondary else null,
                    onClick = { activeSubTab = "CHANNELS" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_channels")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Channels (${channels.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (activeSubTab == "COMMUNITIES") {
            // Section A: Your Communities
            if (joinedCommunities.isNotEmpty()) {
                item {
                    Text(
                        text = "YOUR COMMUNITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(joinedCommunities, key = { "joined_${it.id}" }) { comm ->
                            JoinedCommunityCompactCard(
                                community = comm,
                                onClick = {
                                    if (comm.linkedConversationId.isNotBlank()) {
                                        onOpenLinkedConversation(comm.linkedConversationId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section B: Pending Invitations (Part 5.6)
            if (pendingInvites.isNotEmpty()) {
                item {
                    Text(
                        text = "PENDING INVITATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperHighlight,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                items(pendingInvites, key = { "invite_${it.id}" }) { inviteComm ->
                    CommunityDiscoveryCard(
                        community = inviteComm,
                        onOpenSpace = {
                            if (inviteComm.linkedConversationId.isNotBlank()) {
                                onOpenLinkedConversation(inviteComm.linkedConversationId)
                            }
                        },
                        onToggleJoin = { onToggleJoinCommunity(inviteComm) }
                    )
                }
            }

            // Section C: Discover Public Communities
            item {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "DISCOVER COMMUNITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val selected = cat == selectedCategory
                            LiquidGlassSurface(
                                intensity = if (selected) GlassIntensity.LEVEL_2_STANDARD else GlassIntensity.LEVEL_1_SUBTLE,
                                shape = RoundedCornerShape(12.dp),
                                accentTint = if (selected) PaperAccentPrimary else null,
                                onClick = { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else PaperTextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(discoverCommunities, key = { "disc_${it.id}" }) { comm ->
                CommunityDiscoveryCard(
                    community = comm,
                    onOpenSpace = {
                        if (comm.linkedConversationId.isNotBlank()) {
                            onOpenLinkedConversation(comm.linkedConversationId)
                        }
                    },
                    onToggleJoin = { onToggleJoinCommunity(comm) }
                )
            }
        } else {
            // CHANNELS SCREEN (Part 5.7)
            items(channels, key = { it.id }) { channel ->
                BroadcastChannelCard(
                    channel = channel,
                    onVoteOption = { opt -> onVoteChannelPoll(channel, opt) },
                    onPostAnnouncement = { channelForPostDialog = channel }
                )
            }
        }
    }

    // Create Community Dialog
    if (showCreateCommunityDialog) {
        var name by remember { mutableStateOf("") }
        var handle by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Technology") }
        var isPublic by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateCommunityDialog = false },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Create PaperMessages Community") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (handle.isBlank()) {
                                handle = it.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
                            }
                        },
                        label = { Text("Community Name") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(30) },
                        label = { Text("Community Handle") },
                        prefix = { Text("@") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Technology, Design, Security...)") },
                        singleLine = true,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        colors = paperTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Text("Publicly discoverable community", color = PaperTextPrimary)
                    }
                }
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Launch Community",
                    enabled = name.isNotBlank(),
                    onClick = {
                        showCreateCommunityDialog = false
                        onCreateCommunity(name, handle, description, category, isPublic)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateCommunityDialog = false }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }

    // Channel Admin Broadcast Post Dialog
    val activeChannelPost = channelForPostDialog
    if (activeChannelPost != null) {
        var announcementText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { channelForPostDialog = null },
            containerColor = PaperSurfaceElevated,
            titleContentColor = PaperTextPrimary,
            textContentColor = PaperTextSecondary,
            title = { Text("Broadcast to ${activeChannelPost.name}") },
            text = {
                OutlinedTextField(
                    value = announcementText,
                    onValueChange = { announcementText = it },
                    label = { Text("Announcement content") },
                    maxLines = 4,
                    colors = paperTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                LiquidGlassButton(
                    text = "Publish Announcement",
                    enabled = announcementText.isNotBlank(),
                    onClick = {
                        onPostChannelAnnouncement(activeChannelPost, announcementText)
                        channelForPostDialog = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { channelForPostDialog = null }) {
                    Text("Cancel", color = PaperTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun JoinedCommunityCompactCard(
    community: CommunityItem,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = Modifier
            .width(175.dp)
            .height(118.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PaperAvatar(
                    name = community.name,
                    avatarHex = community.avatarColor,
                    size = 42.dp,
                    isGroup = true
                )
                if (community.unreadCount > 0L) {
                    Surface(color = PaperAccentPrimary, shape = CircleShape) {
                        Text(
                            text = "${community.unreadCount} new",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PaperTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${community.memberCount} members · ${community.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CommunityDiscoveryCard(
    community: CommunityItem,
    onOpenSpace: () -> Unit,
    onToggleJoin: () -> Unit
) {
    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(22.dp),
        onClick = onOpenSpace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaperAvatar(
                    name = community.name,
                    avatarHex = community.avatarColor,
                    size = 48.dp,
                    isGroup = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PaperTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (community.publicAccess) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = null,
                            tint = PaperAccentSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "@${community.handle} · ${community.memberCount} members · ${community.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperTextSecondary
                    )
                }

                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(12.dp),
                    accentTint = if (community.joined) PaperSuccess else PaperAccentPrimary,
                    onClick = onToggleJoin
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (community.pendingInvite) {
                                Icons.Default.MailOutline
                            } else if (community.joined) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.Add
                            },
                            contentDescription = null,
                            tint = if (community.joined) PaperSuccess else PaperHighlight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                community.pendingInvite -> "Accept Invite"
                                community.joined -> "Joined"
                                else -> "Join"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (community.joined) PaperSuccess else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = community.description,
                style = MaterialTheme.typography.bodyMedium,
                color = PaperTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Invite Link: papermessages.app/c/${community.inviteCode}",
                style = MaterialTheme.typography.labelSmall,
                color = PaperTextMuted
            )
        }
    }
}

@Composable
private fun BroadcastChannelCard(
    channel: ChannelItem,
    onVoteOption: (String) -> Unit,
    onPostAnnouncement: () -> Unit
) {
    val totalVotes = channel.totalPollVotes()

    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_2_STANDARD,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaperAvatar(
                    name = channel.name,
                    avatarHex = channel.avatarColor,
                    size = 48.dp,
                    isGroup = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PaperTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Channel",
                            tint = PaperAccentSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "@${channel.handle} · ${channel.subscriberCount} subscribers",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperAccentSecondary
                    )
                }

                if (channel.adminRole) {
                    LiquidGlassSurface(
                        intensity = GlassIntensity.LEVEL_1_SUBTLE,
                        shape = RoundedCornerShape(12.dp),
                        accentTint = PaperAccentPrimary,
                        onClick = onPostAnnouncement
                    ) {
                        Text(
                            text = "Broadcast",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PaperHighlight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (channel.pinnedPostText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = PaperHighlight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = channel.pinnedPostText,
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = channel.latestAnnouncement,
                style = MaterialTheme.typography.bodyLarge,
                color = PaperTextPrimary
            )

            // Interactive Channel Poll (Part 5.7)
            if (channel.pollQuestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                LiquidGlassSurface(
                    intensity = GlassIntensity.LEVEL_1_SUBTLE,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Poll,
                                contentDescription = "Poll",
                                tint = PaperAccentSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = channel.pollQuestion,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PaperTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        PollVoteRow(
                            label = channel.pollOptionA,
                            votes = channel.pollVotesA,
                            total = totalVotes,
                            isSelected = channel.userVotedOption == "A",
                            onClick = { onVoteOption("A") }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PollVoteRow(
                            label = channel.pollOptionB,
                            votes = channel.pollVotesB,
                            total = totalVotes,
                            isSelected = channel.userVotedOption == "B",
                            onClick = { onVoteOption("B") }
                        )
                        if (channel.pollOptionC.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            PollVoteRow(
                                label = channel.pollOptionC,
                                votes = channel.pollVotesC,
                                targetOption = "C",
                                total = totalVotes,
                                isSelected = channel.userVotedOption == "C",
                                onClick = { onVoteOption("C") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PollVoteRow(
    label: String,
    votes: Long,
    total: Long,
    isSelected: Boolean,
    targetOption: String = "",
    onClick: () -> Unit
) {
    val ratio = (votes.toFloat() / total.toFloat()).coerceIn(0.05f, 1f)
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "poll_bar")
    val pct = ((votes.toDouble() / total.toDouble()) * 100).toInt()

    LiquidGlassSurface(
        intensity = GlassIntensity.LEVEL_1_SUBTLE,
        shape = RoundedCornerShape(10.dp),
        accentTint = if (isSelected) PaperAccentPrimary else null,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(38.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PaperAccentPrimary.copy(alpha = if (isSelected) 0.42f else 0.2f),
                                PaperAccentBlue.copy(alpha = if (isSelected) 0.35f else 0.12f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label + if (isSelected) " ✓" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = PaperTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$pct% ($votes)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PaperHighlight
                )
            }
        }
    }
}
