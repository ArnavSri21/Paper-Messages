package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalIndexedMessage
import com.example.data.local.LocalSearchDao
import com.example.data.model.CallRecordItem
import com.example.data.model.ChannelItem
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityItem
import com.example.data.model.ConversationItem
import com.example.data.model.ThreadReplyItem
import com.example.data.model.UserProfile
import com.example.data.repository.PaperRepository
import com.example.security.CryptoEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveCallSession(
    val conversationId: String,
    val contactName: String,
    val avatarColor: String,
    val isVideo: Boolean,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val elapsedSeconds: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class PaperViewModel(
    private val repository: PaperRepository,
    private val searchDao: LocalSearchDao?,
    val currentUserId: String,
    val initialDisplayName: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _accentHex = MutableStateFlow("#8B7CFF")
    val accentHex: StateFlow<String> = _accentHex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _localIndexedResults = MutableStateFlow<List<LocalIndexedMessage>>(emptyList())
    val localIndexedResults: StateFlow<List<LocalIndexedMessage>> = _localIndexedResults.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _selectedTopic = MutableStateFlow("All")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _activeThreadParentId = MutableStateFlow<String?>(null)
    val activeThreadParentId: StateFlow<String?> = _activeThreadParentId.asStateFlow()

    private val _threadSearchQuery = MutableStateFlow("")
    val threadSearchQuery: StateFlow<String> = _threadSearchQuery.asStateFlow()

    private val _activeCallSession = MutableStateFlow<ActiveCallSession?>(null)
    val activeCallSession: StateFlow<ActiveCallSession?> = _activeCallSession.asStateFlow()
    private var callTimerJob: Job? = null

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndReport(error, "Failed to load user profile")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val conversations: StateFlow<List<ConversationItem>> = repository.observeConversations(currentUserId)
        .catch { error ->
            unwrapAndReport(error, "Failed to load conversations")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeMessages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeMessages(convId, currentUserId)
            }
        }
        .catch { error ->
            unwrapAndReport(error, "Failed to load messages")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeThreadReplies: StateFlow<List<ThreadReplyItem>> = _activeThreadParentId
        .flatMapLatest { parentId ->
            val convId = _activeConversationId.value
            if (convId.isNullOrBlank() || parentId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeThreadReplies(convId, parentId, currentUserId)
            }
        }
        .catch { error ->
            unwrapAndReport(error, "Failed to load thread replies")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val communities: StateFlow<List<CommunityItem>> = repository.observeCommunities(currentUserId)
        .catch { error ->
            unwrapAndReport(error, "Failed to load communities")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val channels: StateFlow<List<ChannelItem>> = repository.observeChannels(currentUserId)
        .catch { error ->
            unwrapAndReport(error, "Failed to load channels")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val calls: StateFlow<List<CallRecordItem>> = repository.observeCalls(currentUserId)
        .catch { error ->
            unwrapAndReport(error, "Failed to load call history")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.purgeLegacySeededData(initialDisplayName)
            listOf("conv_paper_arch", "conv_private_elena", "conv_crypto_guild").forEach { seededId ->
                searchDao?.deleteConversationIndex(currentUserId, seededId)
            }
        }
        // Index active messages in local Room FTS cache whenever they update
        viewModelScope.launch(Dispatchers.IO) {
            activeMessages.collect { msgs ->
                val convId = _activeConversationId.value ?: return@collect
                val convTitle = conversations.value.find { it.id == convId }?.title ?: "Conversation"
                val indexed = msgs.map { m ->
                    val decrypted = CryptoEngine.decryptMessage(
                        cipherTextBase64 = m.cipherText,
                        ivBase64 = m.ivBase64,
                        userId = currentUserId,
                        conversationId = convId,
                        fallbackPlaintext = m.content
                    )
                    LocalIndexedMessage(
                        messageId = m.id,
                        userId = currentUserId,
                        conversationId = convId,
                        conversationTitle = convTitle,
                        senderName = m.senderName,
                        topic = m.topic,
                        decryptedPlaintext = decrypted,
                        updatedAtEpochMs = m.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                searchDao?.upsertIndexedMessages(indexed)
            }
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun showStatus(message: String) {
        _statusBanner.value = message
    }

    fun setThemeMode(isDark: Boolean) {
        _darkTheme.value = isDark
    }

    fun setAccentColor(hex: String) {
        _accentHex.value = hex
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch(Dispatchers.IO) {
            if (query.trim().length >= 2 && searchDao != null) {
                _localIndexedResults.value = searchDao.searchDecryptedMessages(currentUserId, query.trim())
            } else {
                _localIndexedResults.value = emptyList()
            }
        }
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _selectedTopic.value = "All"
        _activeThreadParentId.value = null
        val conv = conversations.value.find { it.id == conversationId }
        if (conv != null && conv.unreadCount > 0L) {
            viewModelScope.launch {
                repository.updateConversationFlags(conv, unreadCount = 0L)
            }
        }
    }

    fun closeConversation() {
        _activeThreadParentId.value = null
        _activeConversationId.value = null
    }

    fun selectTopic(topic: String) {
        _selectedTopic.value = topic
    }

    fun openThread(parentMessageId: String) {
        _activeThreadParentId.value = parentMessageId
        _threadSearchQuery.value = ""
        val convId = _activeConversationId.value ?: return
        val parent = activeMessages.value.find { it.id == parentMessageId } ?: return
        if (parent.unreadReplyCount > 0L) {
            viewModelScope.launch {
                repository.updateMessageState(convId, parent, unreadReplyCount = 0L)
            }
        }
    }

    fun closeThread() {
        _activeThreadParentId.value = null
        _threadSearchQuery.value = ""
    }

    fun updateThreadSearchQuery(query: String) {
        _threadSearchQuery.value = query
    }

    fun completePhoneAndHandleOnboarding(
        displayName: String,
        username: String,
        phoneNumber: String,
        countryCode: String,
        bio: String
    ) {
        viewModelScope.launch {
            repository.upsertUserProfile(
                displayName = displayName,
                username = username,
                phoneNumber = phoneNumber,
                countryCode = countryCode,
                bio = bio,
                onboardingCompleted = true
            ).onSuccess {
                showStatus("Profile verified & E2EE identity keys generated")
            }.onFailure {
                showStatus(it.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun updateProfileAndPrivacy(
        displayName: String,
        username: String,
        bio: String,
        privacyOnline: String,
        privacyLastSeen: String,
        privacyReadReceipts: Boolean,
        privacyPhoneLookup: String,
        reducedMotion: Boolean
    ) {
        val current = userProfile.value
        viewModelScope.launch {
            repository.upsertUserProfile(
                displayName = displayName,
                username = username,
                phoneNumber = current?.phoneNumber ?: "",
                countryCode = current?.countryCode ?: "+1",
                bio = bio,
                avatarColor = current?.avatarColor ?: _accentHex.value,
                onboardingCompleted = true,
                privacyOnline = privacyOnline,
                privacyLastSeen = privacyLastSeen,
                privacyReadReceipts = privacyReadReceipts,
                privacyPhoneLookup = privacyPhoneLookup,
                reducedMotion = reducedMotion,
                blockedUsersCsv = current?.blockedUsersCsv ?: ""
            ).onSuccess {
                showStatus("Profile & privacy settings updated")
            }
        }
    }

    fun rotateIdentityKeys() {
        viewModelScope.launch {
            repository.rotateDeviceKeys().onSuccess {
                showStatus("Hardware E2EE identity & signed pre-keys rotated")
            }
        }
    }

    fun toggleBlockUser(handleOrName: String) {
        viewModelScope.launch {
            repository.toggleBlockUser(handleOrName).onSuccess {
                showStatus("Updated blocked accounts list")
            }
        }
    }

    fun createNewConversation(
        title: String,
        subtitle: String,
        type: String,
        topicsCsv: String = "General,Announcements,Resources,Questions"
    ) {
        viewModelScope.launch {
            repository.createConversation(
                title = title,
                subtitle = subtitle,
                type = type,
                topicsCsv = topicsCsv
            ).onSuccess { convId ->
                val senderName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: "You"
                repository.sendMessage(
                    conversationId = convId,
                    senderId = currentUserId,
                    senderName = senderName,
                    senderRole = if (type == "GROUP") "Admin" else "Contact",
                    topic = "General",
                    content = if (type == "GROUP") {
                        "Welcome to $title! Use Topics above or tap any message to start a focused Thread."
                    } else {
                        "Started an end-to-end encrypted conversation with $title."
                    }
                )
                openConversation(convId)
            }
        }
    }

    fun joinByInviteCode(inviteCode: String) {
        val clean = inviteCode.trim().uppercase()
        if (clean.isBlank()) return
        val existing = conversations.value.find { it.inviteCode.equals(clean, ignoreCase = true) }
        if (existing != null) {
            openConversation(existing.id)
            showStatus("Joined ${existing.title}")
        } else {
            createNewConversation(
                title = "Invited Group ($clean)",
                subtitle = "1 member · Joined via Invite Link",
                type = "GROUP"
            )
            showStatus("Joined group via invite code $clean")
        }
    }

    fun togglePinConversation(conv: ConversationItem) {
        viewModelScope.launch {
            repository.updateConversationFlags(conv, pinned = !conv.pinned)
        }
    }

    fun toggleMuteConversation(conv: ConversationItem) {
        viewModelScope.launch {
            repository.updateConversationFlags(conv, muted = !conv.muted)
        }
    }

    fun toggleArchiveConversation(conv: ConversationItem) {
        viewModelScope.launch {
            repository.updateConversationFlags(conv, archived = !conv.archived)
            showStatus(if (!conv.archived) "Conversation archived" else "Unarchived conversation")
        }
    }

    fun markConversationUnread(conv: ConversationItem) {
        viewModelScope.launch {
            repository.updateConversationFlags(conv, unreadCount = if (conv.unreadCount > 0L) 0L else 1L)
        }
    }

    fun deleteConversation(conv: ConversationItem) {
        viewModelScope.launch {
            if (_activeConversationId.value == conv.id) {
                closeConversation()
            }
            repository.deleteConversation(conv.id)
            searchDao?.deleteConversationIndex(currentUserId, conv.id)
            showStatus("Deleted ${conv.title}")
        }
    }

    fun sendChatMessage(
        content: String,
        topic: String? = null,
        attachmentType: String = "NONE",
        attachmentName: String = "",
        attachmentMeta: String = ""
    ) {
        val convId = _activeConversationId.value ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty() && attachmentType == "NONE") return
        val effectiveTopic = topic ?: _selectedTopic.value.let { if (it == "All") "General" else it }
        val senderName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: "You"

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                senderId = currentUserId,
                senderName = senderName,
                senderRole = "Owner",
                topic = effectiveTopic,
                content = trimmed.ifEmpty { attachmentName },
                attachmentType = attachmentType,
                attachmentName = attachmentName,
                attachmentMeta = attachmentMeta
            )
            // If a document or media resource was shared in a group, append to group resource library
            if (attachmentType in listOf("DOCUMENT", "IMAGE", "VIDEO") && attachmentName.isNotBlank()) {
                val conv = conversations.value.find { it.id == convId }
                if (conv != null) {
                    val existingResources = conv.resourcesList().toMutableList()
                    if (!existingResources.contains(attachmentName)) {
                        existingResources.add(0, attachmentName)
                        repository.updateConversationFlags(
                            conv,
                            resourcesCsv = existingResources.joinToString("|")
                        )
                    }
                }
            }
        }
    }

    fun toggleReactionOnMessage(message: ChatMessage, emoji: String) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.toggleMessageReaction(convId, message, emoji)
        }
    }

    fun editMessage(message: ChatMessage, updatedText: String) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageState(
                conversationId = convId,
                message = message,
                newContent = updatedText,
                edited = true
            )
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageState(
                conversationId = convId,
                message = message,
                newContent = "This message was deleted",
                deleted = true
            )
        }
    }

    fun togglePinMessage(message: ChatMessage) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageState(
                conversationId = convId,
                message = message,
                pinned = !message.pinned
            )
            showStatus(if (!message.pinned) "Message pinned to group" else "Message unpinned")
        }
    }

    fun forwardMessage(message: ChatMessage, targetConversation: ConversationItem) {
        val senderName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: "You"
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = targetConversation.id,
                senderId = currentUserId,
                senderName = senderName,
                topic = "General",
                content = "↪ Forwarded from ${message.senderName}: ${message.content}",
                attachmentType = message.attachmentType,
                attachmentName = message.attachmentName,
                attachmentMeta = message.attachmentMeta
            )
            showStatus("Forwarded to ${targetConversation.title}")
        }
    }

    fun sendThreadReply(
        content: String,
        quotedSenderName: String = "",
        quotedPreview: String = ""
    ) {
        val convId = _activeConversationId.value ?: return
        val parentId = _activeThreadParentId.value ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val senderName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: "You"

        viewModelScope.launch {
            repository.addThreadReply(
                conversationId = convId,
                parentMessageId = parentId,
                senderId = currentUserId,
                senderName = senderName,
                senderRole = "Owner",
                content = trimmed,
                quotedSenderName = quotedSenderName,
                quotedPreview = quotedPreview
            )
        }
    }

    fun toggleFollowThread(parentMessage: ChatMessage) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageState(
                conversationId = convId,
                message = parentMessage,
                followingThread = !parentMessage.followingThread
            )
            showStatus(
                if (!parentMessage.followingThread) "Following thread updates" else "Unfollowed thread"
            )
        }
    }

    fun toggleMuteThread(parentMessage: ChatMessage) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageState(
                conversationId = convId,
                message = parentMessage,
                threadMuted = !parentMessage.threadMuted
            )
            showStatus(
                if (!parentMessage.threadMuted) "Thread notifications muted" else "Thread unmuted"
            )
        }
    }

    fun toggleThreadReplyReaction(reply: ThreadReplyItem, emoji: String) {
        val convId = _activeConversationId.value ?: return
        val parentId = _activeThreadParentId.value ?: return
        viewModelScope.launch {
            repository.toggleThreadReplyReaction(convId, parentId, reply, emoji)
        }
    }

    fun createNewCommunity(
        name: String,
        handle: String,
        description: String,
        category: String,
        publicAccess: Boolean
    ) {
        viewModelScope.launch {
            repository.createCommunity(
                name = name,
                handle = handle,
                description = description,
                category = category,
                publicAccess = publicAccess
            ).onSuccess {
                showStatus("Community '$name' created")
            }
        }
    }

    fun toggleJoinCommunity(community: CommunityItem) {
        viewModelScope.launch {
            repository.toggleJoinCommunity(community).onSuccess {
                showStatus(if (!community.joined) "Joined ${community.name}" else "Left ${community.name}")
            }
        }
    }

    fun voteInChannelPoll(channel: ChannelItem, optionKey: String) {
        viewModelScope.launch {
            repository.voteInChannelPoll(channel, optionKey)
        }
    }

    fun postChannelAnnouncement(channel: ChannelItem, text: String) {
        viewModelScope.launch {
            repository.postChannelAnnouncement(channel, text).onSuccess {
                showStatus("Broadcast posted to ${channel.name}")
            }
        }
    }

    fun createNewChannel(
        name: String,
        handle: String,
        description: String,
        announcement: String,
        pollQuestion: String = "",
        pollOptionA: String = "",
        pollOptionB: String = "",
        pollOptionC: String = ""
    ) {
        viewModelScope.launch {
            repository.createChannel(
                name = name,
                handle = handle,
                description = description,
                announcement = announcement,
                pollQuestion = pollQuestion,
                pollOptionA = pollOptionA,
                pollOptionB = pollOptionB,
                pollOptionC = pollOptionC
            ).onSuccess {
                showStatus("Broadcast channel '$name' created")
            }
        }
    }

    fun startCall(
        conversationId: String,
        contactName: String,
        avatarColor: String,
        isVideo: Boolean
    ) {
        callTimerJob?.cancel()
        _activeCallSession.value = ActiveCallSession(
            conversationId = conversationId,
            contactName = contactName,
            avatarColor = avatarColor,
            isVideo = isVideo
        )
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeCallSession.value ?: break
                _activeCallSession.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    fun updateActiveCallControls(
        isMuted: Boolean? = null,
        isSpeakerOn: Boolean? = null,
        isCameraEnabled: Boolean? = null,
        isFrontCamera: Boolean? = null
    ) {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(
            isMuted = isMuted ?: current.isMuted,
            isSpeakerOn = isSpeakerOn ?: current.isSpeakerOn,
            isCameraEnabled = isCameraEnabled ?: current.isCameraEnabled,
            isFrontCamera = isFrontCamera ?: current.isFrontCamera
        )
    }

    fun endActiveCall() {
        val session = _activeCallSession.value ?: return
        callTimerJob?.cancel()
        _activeCallSession.value = null
        viewModelScope.launch {
            repository.logCallRecord(
                conversationId = session.conversationId,
                contactName = session.contactName,
                callType = if (session.isVideo) "VIDEO" else "VOICE",
                direction = "OUTGOING",
                status = "ENDED",
                durationSeconds = session.elapsedSeconds.toLong().coerceAtLeast(1L),
                avatarColor = session.avatarColor
            )
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetName: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            repository.submitModerationReport(
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                reason = reason,
                details = details
            ).onSuccess {
                showStatus("Report submitted to PaperMessages Trust & Safety")
            }
        }
    }

    private fun unwrapAndReport(error: Throwable, fallbackMsg: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        _statusBanner.value = rootError.localizedMessage ?: fallbackMsg
    }
}
