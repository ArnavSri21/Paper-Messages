package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.CallRecordItem
import com.example.data.model.ChannelItem
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityItem
import com.example.data.model.ConversationItem
import com.example.data.model.ModerationReportItem
import com.example.data.model.OperationType
import com.example.data.model.ThreadReplyItem
import com.example.data.model.UserProfile
import com.example.data.model.handleFirestoreError
import com.example.security.CryptoEngine
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PaperRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // =========================================================================
    // 1. USER PROFILE & ONBOARDING
    // =========================================================================

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.GET, docRef.path)
                }
        )
    }

    suspend fun getUserProfile(userId: String = requireUserId()): Result<UserProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snap = docRef.get().await()
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertUserProfile(
        displayName: String,
        username: String,
        phoneNumber: String,
        countryCode: String = "+1",
        bio: String = "Conversations deserve structure, space and clarity.",
        avatarColor: String = "#8B7CFF",
        onboardingCompleted: Boolean = true,
        privacyOnline: String = "EVERYONE",
        privacyLastSeen: String = "EVERYONE",
        privacyReadReceipts: Boolean = true,
        privacyPhoneLookup: String = "CONTACTS",
        reducedMotion: Boolean = false,
        blockedUsersCsv: String = ""
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanHandle = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").let {
            if (it.length < 3) "${it}_pm" else it.take(32)
        }
        val pubKey = CryptoEngine.getPublicKeyBase64(uid)
        val fingerprint = CryptoEngine.computeSafetyFingerprint(uid)
        val deviceLabel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(64)

        try {
            val existingSnap = docRef.get().await()
            val existingProfile = existingSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val model = UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.trim().ifBlank { "Paper User" }.take(80),
                username = cleanHandle,
                phoneNumber = phoneNumber.trim().take(24),
                countryCode = countryCode.trim().ifBlank { "+1" }.take(8),
                bio = bio.trim().take(240),
                avatarColor = avatarColor,
                publicKeyBase64 = pubKey,
                keyFingerprint = fingerprint,
                deviceName = deviceLabel.ifBlank { "Android Device" },
                onboardingCompleted = onboardingCompleted,
                online = true,
                privacyOnline = privacyOnline,
                privacyLastSeen = privacyLastSeen,
                privacyReadReceipts = privacyReadReceipts,
                privacyPhoneLookup = privacyPhoneLookup,
                reducedMotion = reducedMotion,
                blockedUsersCsv = blockedUsersCsv,
                createdAt = existingProfile?.createdAt,
                updatedAt = null
            )
            docRef.set(model).await()
            model
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun rotateDeviceKeys(): Result<UserProfile?> = runCatching {
        val uid = requireUserId()
        CryptoEngine.rotateIdentityKeyPair(uid)
        val current = getUserProfile(uid).getOrNull() ?: return@runCatching null
        upsertUserProfile(
            displayName = current.displayName,
            username = current.username,
            phoneNumber = current.phoneNumber,
            countryCode = current.countryCode,
            bio = current.bio,
            avatarColor = current.avatarColor,
            onboardingCompleted = current.onboardingCompleted,
            privacyOnline = current.privacyOnline,
            privacyLastSeen = current.privacyLastSeen,
            privacyReadReceipts = current.privacyReadReceipts,
            privacyPhoneLookup = current.privacyPhoneLookup,
            reducedMotion = current.reducedMotion,
            blockedUsersCsv = current.blockedUsersCsv
        ).getOrThrow()
    }

    suspend fun toggleBlockUser(targetHandleOrName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val current = getUserProfile(uid).getOrNull() ?: return@runCatching
        val list = current.blockedList().toMutableList()
        if (list.contains(targetHandleOrName)) {
            list.remove(targetHandleOrName)
        } else {
            list.add(targetHandleOrName)
        }
        upsertUserProfile(
            displayName = current.displayName,
            username = current.username,
            phoneNumber = current.phoneNumber,
            countryCode = current.countryCode,
            bio = current.bio,
            avatarColor = current.avatarColor,
            onboardingCompleted = current.onboardingCompleted,
            privacyOnline = current.privacyOnline,
            privacyLastSeen = current.privacyLastSeen,
            privacyReadReceipts = current.privacyReadReceipts,
            privacyPhoneLookup = current.privacyPhoneLookup,
            reducedMotion = current.reducedMotion,
            blockedUsersCsv = list.joinToString(",").take(1000)
        ).getOrThrow()
    }

    // =========================================================================
    // 2. CONVERSATIONS (PRIVATE & THREADED GROUPS)
    // =========================================================================

    fun observeConversations(userId: String = requireUserId()): Flow<List<ConversationItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("conversations")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ConversationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<ConversationItem> { it.pinned }
                                .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun getConversations(userId: String = requireUserId()): Result<List<ConversationItem>> = runCatching {
        val colRef = db.collection("users").document(userId).collection("conversations")
        try {
            val snap = colRef.whereEqualTo("userId", userId).get().await()
            snap.toObjects(ConversationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun createConversation(
        title: String,
        subtitle: String,
        type: String, // "PRIVATE" or "GROUP"
        topicsCsv: String = "General,Announcements,Resources,Questions",
        resourcesCsv: String = "",
        pinned: Boolean = false,
        avatarColor: String = "#8B7CFF",
        customId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convId = customId ?: "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("conversations").document(convId)
        val encryptionMode = if (type == "PRIVATE") "E2EE_SIGNAL" else "E2EE_MLS"
        val item = ConversationItem(
            id = convId,
            userId = uid,
            title = title.trim().ifBlank { "New Conversation" }.take(100),
            subtitle = subtitle.trim().ifBlank {
                if (type == "GROUP") "12 members · 4 online" else "Online · End-to-End Encrypted"
            }.take(160),
            type = type,
            encryptionMode = encryptionMode,
            memberCount = if (type == "GROUP") 12L else 2L,
            onlineCount = if (type == "GROUP") 4L else 2L,
            lastMessagePreview = "Conversation created",
            lastMessageSender = "System",
            unreadCount = 0L,
            pinned = pinned,
            muted = false,
            archived = false,
            topicsCsv = topicsCsv.take(300),
            resourcesCsv = resourcesCsv.take(1500),
            inviteCode = "PAPER-${convId.takeLast(6).uppercase()}",
            avatarColor = avatarColor,
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(item).await()
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateConversationFlags(
        conversation: ConversationItem,
        pinned: Boolean = conversation.pinned,
        muted: Boolean = conversation.muted,
        archived: Boolean = conversation.archived,
        unreadCount: Long = conversation.unreadCount,
        resourcesCsv: String = conversation.resourcesCsv
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(conversation.id.isNotBlank()) { "Conversation ID required" }
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversation.id)
        try {
            val snap = docRef.get().await()
            val existing = snap.toObject(ConversationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: conversation
            val updated = existing.copy(
                pinned = pinned,
                muted = muted,
                archived = archived,
                unreadCount = unreadCount,
                resourcesCsv = resourcesCsv.take(1500),
                updatedAt = null
            )
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // 3. MESSAGES & DENORMALIZED THREAD PARENTS
    // =========================================================================

    fun observeMessages(
        conversationId: String,
        userId: String = requireUserId()
    ): Flow<List<ChatMessage>> = flow {
        val colRef = db.collection("users")
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderRole: String = "Member",
        topic: String = "General",
        content: String,
        attachmentType: String = "NONE",
        attachmentName: String = "",
        attachmentMeta: String = "",
        readCount: Long = 1L,
        readersCsv: String = "",
        customMessageId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgId = customMessageId ?: "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val convRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document(msgId)

        val envelope = CryptoEngine.encryptMessage(content, uid, conversationId)
        val msg = ChatMessage(
            id = msgId,
            userId = uid,
            conversationId = conversationId,
            senderId = senderId.ifBlank { uid },
            senderName = senderName.trim().ifBlank { "You" }.take(80),
            senderRole = senderRole.take(32),
            topic = topic.ifBlank { "General" }.take(48),
            content = content.trim().take(2000),
            cipherText = envelope.cipherTextBase64.take(4000),
            ivBase64 = envelope.ivBase64.take(128),
            attachmentType = attachmentType,
            attachmentName = attachmentName.take(255),
            attachmentMeta = attachmentMeta.take(1000),
            replyCount = 0L,
            lastReplyPreview = "",
            lastReplySender = "",
            followingThread = true,
            threadMuted = false,
            unreadReplyCount = 0L,
            reactionsSummary = "",
            readCount = readCount,
            readersCsv = readersCsv.ifBlank { senderName }.take(500),
            pinned = false,
            edited = false,
            deleted = false,
            createdAt = null,
            updatedAt = null
        )

        try {
            msgRef.set(msg).await()
            // Update parent conversation lastMessagePreview
            val convSnap = convRef.get().await()
            val existingConv = convSnap.toObject(
                ConversationItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (existingConv != null) {
                val preview = when (attachmentType) {
                    "VOICE" -> "🎤 Voice note (${attachmentMeta.ifBlank { "0:14" }})"
                    "POLL" -> "📊 Poll: ${content.take(80)}"
                    "IMAGE" -> "🖼 Photo: ${attachmentName.ifBlank { content }}"
                    "DOCUMENT" -> "📄 Document: ${attachmentName.ifBlank { content }}"
                    else -> content
                }.take(280)
                convRef.set(
                    existingConv.copy(
                        lastMessagePreview = preview,
                        lastMessageSender = senderName.take(80),
                        unreadCount = 0L,
                        updatedAt = null
                    )
                ).await()
            }
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun toggleMessageReaction(
        conversationId: String,
        message: ChatMessage,
        emoji: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("users")
            .document(uid)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(message.id)
        try {
            val snap = msgRef.get().await()
            val current = snap.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: message
            val map = current.parsedReactions().toMutableMap()
            val existingCount = map[emoji] ?: 0
            if (existingCount > 0) {
                if (existingCount == 1) map.remove(emoji) else map[emoji] = existingCount - 1
            } else {
                map[emoji] = 1
            }
            val summary = map.entries.joinToString(",") { "${it.key}:${it.value}" }.take(500)
            msgRef.set(current.copy(reactionsSummary = summary, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun updateMessageState(
        conversationId: String,
        message: ChatMessage,
        newContent: String = message.content,
        pinned: Boolean = message.pinned,
        edited: Boolean = message.edited,
        deleted: Boolean = message.deleted,
        followingThread: Boolean = message.followingThread,
        threadMuted: Boolean = message.threadMuted,
        unreadReplyCount: Long = message.unreadReplyCount,
        attachmentMeta: String = message.attachmentMeta
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("users")
            .document(uid)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(message.id)
        try {
            val snap = msgRef.get().await()
            val current = snap.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: message
            val envelope = CryptoEngine.encryptMessage(newContent, uid, conversationId)
            msgRef.set(
                current.copy(
                    content = newContent.take(2000),
                    cipherText = envelope.cipherTextBase64.take(4000),
                    ivBase64 = envelope.ivBase64.take(128),
                    pinned = pinned,
                    edited = edited,
                    deleted = deleted,
                    followingThread = followingThread,
                    threadMuted = threadMuted,
                    unreadReplyCount = unreadReplyCount,
                    attachmentMeta = attachmentMeta.take(1000),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    // =========================================================================
    // 4. THREAD REPLIES — SIGNATURE GROUP THREAD EXPERIENCE
    // =========================================================================

    fun observeThreadReplies(
        conversationId: String,
        parentMessageId: String,
        userId: String = requireUserId()
    ): Flow<List<ThreadReplyItem>> = flow {
        val colRef = db.collection("users")
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(parentMessageId)
            .collection("replies")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ThreadReplyItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun addThreadReply(
        conversationId: String,
        parentMessageId: String,
        senderId: String,
        senderName: String,
        senderRole: String = "Member",
        content: String,
        quotedSenderName: String = "",
        quotedPreview: String = "",
        mentionsSummary: String = "",
        customReplyId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val replyId = customReplyId ?: "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = db.collection("users")
            .document(uid)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(parentMessageId)
        val replyRef = msgRef.collection("replies").document(replyId)

        val extractedMentions = if (mentionsSummary.isNotBlank()) {
            mentionsSummary
        } else {
            Regex("@([a-zA-Z0-9_]+)").findAll(content).joinToString(",") { it.value }
        }

        val reply = ThreadReplyItem(
            id = replyId,
            userId = uid,
            conversationId = conversationId,
            parentMessageId = parentMessageId,
            senderId = senderId.ifBlank { uid },
            senderName = senderName.trim().ifBlank { "You" }.take(80),
            senderRole = senderRole.take(32),
            content = content.trim().take(2000),
            quotedSenderName = quotedSenderName.take(80),
            quotedPreview = quotedPreview.take(300),
            mentionsSummary = extractedMentions.take(300),
            reactionsSummary = "",
            edited = false,
            createdAt = null,
            updatedAt = null
        )

        try {
            replyRef.set(reply).await()

            // Update denormalized thread summary on parent message
            val parentSnap = msgRef.get().await()
            val parent = parentSnap.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (parent != null) {
                msgRef.set(
                    parent.copy(
                        replyCount = parent.replyCount + 1L,
                        lastReplyPreview = content.trim().take(280),
                        lastReplySender = senderName.trim().take(80),
                        unreadReplyCount = 0L,
                        updatedAt = null
                    )
                ).await()
            }
            replyId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, replyRef.path)
            throw e
        }
    }

    suspend fun toggleThreadReplyReaction(
        conversationId: String,
        parentMessageId: String,
        reply: ThreadReplyItem,
        emoji: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val replyRef = db.collection("users")
            .document(uid)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(parentMessageId)
            .collection("replies")
            .document(reply.id)
        try {
            val snap = replyRef.get().await()
            val current = snap.toObject(ThreadReplyItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: reply
            val map = current.parsedReactions().toMutableMap()
            val existingCount = map[emoji] ?: 0
            if (existingCount > 0) {
                if (existingCount == 1) map.remove(emoji) else map[emoji] = existingCount - 1
            } else {
                map[emoji] = 1
            }
            val summary = map.entries.joinToString(",") { "${it.key}:${it.value}" }.take(500)
            replyRef.set(current.copy(reactionsSummary = summary, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, replyRef.path)
            throw e
        }
    }

    // =========================================================================
    // 5. COMMUNITIES & CHANNELS
    // =========================================================================

    fun observeCommunities(userId: String = requireUserId()): Flow<List<CommunityItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("communities")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.memberCount }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun createCommunity(
        name: String,
        handle: String,
        description: String,
        category: String,
        publicAccess: Boolean = true,
        avatarColor: String = "#8B7CFF"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val commId = "comm_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val linkedConvId = createConversation(
            title = name,
            subtitle = "1 member · Community Hub",
            type = "GROUP",
            topicsCsv = "General,Announcements,Resources,Questions",
            avatarColor = avatarColor
        ).getOrThrow()

        val docRef = db.collection("users").document(uid).collection("communities").document(commId)
        val item = CommunityItem(
            id = commId,
            userId = uid,
            name = name.trim().take(80),
            handle = handle.trim().lowercase().replace(" ", "_").take(40).ifBlank { "community_space" },
            description = description.trim().take(500),
            category = category.trim().ifBlank { "Technology" }.take(40),
            memberCount = 1L,
            unreadCount = 0L,
            joined = true,
            pendingInvite = false,
            publicAccess = publicAccess,
            inviteCode = "COMM-${commId.takeLast(5).uppercase()}",
            linkedConversationId = linkedConvId,
            avatarColor = avatarColor,
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(item).await()
            commId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleJoinCommunity(community: CommunityItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("communities").document(community.id)
        try {
            val snap = docRef.get().await()
            val current = snap.toObject(CommunityItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: community
            val nowJoined = !current.joined
            val delta = if (nowJoined) 1L else -1L
            docRef.set(
                current.copy(
                    joined = nowJoined,
                    pendingInvite = false,
                    memberCount = (current.memberCount + delta).coerceAtLeast(1L),
                    unreadCount = 0L,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeChannels(userId: String = requireUserId()): Flow<List<ChannelItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("channels")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChannelItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.subscriberCount }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun voteInChannelPoll(channel: ChannelItem, optionKey: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("channels").document(channel.id)
        try {
            val snap = docRef.get().await()
            val current = snap.toObject(ChannelItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: channel
            if (current.userVotedOption == optionKey) return@runCatching
            val updated = current.copy(
                pollVotesA = if (optionKey == "A") current.pollVotesA + 1L else current.pollVotesA,
                pollVotesB = if (optionKey == "B") current.pollVotesB + 1L else current.pollVotesB,
                pollVotesC = if (optionKey == "C") current.pollVotesC + 1L else current.pollVotesC,
                userVotedOption = optionKey,
                updatedAt = null
            )
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun postChannelAnnouncement(channel: ChannelItem, newAnnouncement: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("channels").document(channel.id)
        try {
            val snap = docRef.get().await()
            val current = snap.toObject(ChannelItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: channel
            docRef.set(
                current.copy(
                    latestAnnouncement = newAnnouncement.trim().take(1500),
                    pinnedPostText = newAnnouncement.trim().take(500),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // 6. VOICE & VIDEO CALLS + MODERATION REPORTS
    // =========================================================================

    fun observeCalls(userId: String = requireUserId()): Flow<List<CallRecordItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("calls")
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallRecordItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, colRef.path)
                }
        )
    }

    suspend fun logCallRecord(
        conversationId: String,
        contactName: String,
        callType: String, // "VOICE" or "VIDEO"
        direction: String = "OUTGOING",
        status: String = "ENDED",
        durationSeconds: Long = 0L,
        avatarColor: String = "#8B7CFF"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("users").document(uid).collection("calls").document(callId)
        val record = CallRecordItem(
            id = callId,
            userId = uid,
            conversationId = conversationId,
            contactName = contactName.take(80),
            callType = callType,
            direction = direction,
            status = status,
            durationSeconds = durationSeconds,
            avatarColor = avatarColor,
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(record).await()
            callId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun submitModerationReport(
        targetType: String,
        targetId: String,
        targetName: String,
        reason: String,
        details: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("users").document(uid).collection("reports").document(reportId)
        val report = ModerationReportItem(
            id = reportId,
            userId = uid,
            targetType = targetType,
            targetId = targetId.ifBlank { "unknown" }.take(128),
            targetName = targetName.ifBlank { "Target" }.take(100),
            reason = reason.ifBlank { "Spam or Policy Violation" }.take(80),
            details = details.take(1000),
            status = "OPEN",
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(report).await()
            reportId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // 7. FIRST-LAUNCH STARTER WORKSPACE SEEDING
    // =========================================================================

    suspend fun ensureStarterWorkspaceSeeded(displayName: String = "You"): Result<Unit> = runCatching {
        val uid = requireUserId()
        val existingConvs = getConversations(uid).getOrDefault(emptyList())
        if (existingConvs.isNotEmpty()) return@runCatching

        // 1. Seed Signature Threaded Group: "Paper Architecture & Design"
        val groupConvId = "conv_paper_arch"
        val groupRef = db.collection("users").document(uid).collection("conversations").document(groupConvId)
        groupRef.set(
            ConversationItem(
                id = groupConvId,
                userId = uid,
                title = "Paper Architecture & Design",
                subtitle = "28 members · 9 online",
                type = "GROUP",
                encryptionMode = "E2EE_MLS",
                memberCount = 28L,
                onlineCount = 9L,
                lastMessagePreview = "How should we approach the upcoming project?",
                lastMessageSender = "Elena Rostova",
                unreadCount = 2L,
                pinned = true,
                muted = false,
                archived = false,
                topicsCsv = "General,Announcements,Resources,Questions",
                resourcesCsv = "Liquid_Glass_Spec_v3.pdf|MLS_TreeKEM_Audit.pdf|Motion_Tokens.json",
                inviteCode = "PAPER-CORE-01",
                avatarColor = "#8B7CFF"
            )
        ).await()

        // Parent Message 1 (Signature Thread Anchor with replies)
        val parentMsg1Id = "msg_arch_thread_1"
        val parentMsg1Content = "How should we approach the upcoming project? Let's keep all architecture proposals structured inside this thread so the main timeline stays clean."
        val env1 = CryptoEngine.encryptMessage(parentMsg1Content, uid, groupConvId)
        groupRef.collection("messages").document(parentMsg1Id).set(
            ChatMessage(
                id = parentMsg1Id,
                userId = uid,
                conversationId = groupConvId,
                senderId = "member_elena",
                senderName = "Elena Rostova",
                senderRole = "Lead Architect",
                topic = "General",
                content = parentMsg1Content,
                cipherText = env1.cipherTextBase64,
                ivBase64 = env1.ivBase64,
                attachmentType = "NONE",
                attachmentName = "",
                attachmentMeta = "",
                replyCount = 4L,
                lastReplyPreview = "@elena_r Denormalizing replyCount on parent messages keeps scrolling at 120fps.",
                lastReplySender = "Marcus Chen",
                followingThread = true,
                threadMuted = false,
                unreadReplyCount = 2L,
                reactionsSummary = "🚀:5,💜:3,✨:2",
                readCount = 19L,
                readersCsv = "Elena Rostova,Marcus Chen,Aria Lin,Devon Brooks,$displayName",
                pinned = true,
                edited = false,
                deleted = false
            )
        ).await()

        // Seed 4 Chronological Thread Replies on Parent Message 1
        val replies = listOf(
            Triple("rep_1", "Marcus Chen" to "Systems Engineer", "We should partition the backend into a Modular Monolith first—12 clean domain modules over PostgreSQL + Redis without microservice overhead."),
            Triple("rep_2", "Aria Lin" to "Security Architect", "Agreed @marcus_c. For encryption, X3DH + Double Ratchet for 1:1 and MLS TreeKEM for private groups gives us forward secrecy with O(log N) updates."),
            Triple("rep_3", "Devon Brooks" to "Design Lead", "On the UI side, Glass Level 2 for floating controls and Glass Level 3 for the slide-in Thread Panel keeps text contrast crisp on #080A10."),
            Triple("rep_4", "Marcus Chen" to "Systems Engineer", "@elena_r Denormalizing replyCount on parent messages keeps scrolling at 120fps.")
        )
        for ((repId, authorPair, body) in replies) {
            groupRef.collection("messages").document(parentMsg1Id).collection("replies").document(repId).set(
                ThreadReplyItem(
                    id = repId,
                    userId = uid,
                    conversationId = groupConvId,
                    parentMessageId = parentMsg1Id,
                    senderId = "member_${authorPair.first.lowercase().replace(" ", "_")}",
                    senderName = authorPair.first,
                    senderRole = authorPair.second,
                    content = body,
                    quotedSenderName = if (repId == "rep_4") "Elena Rostova" else "",
                    quotedPreview = if (repId == "rep_4") "How should we approach the upcoming project?" else "",
                    mentionsSummary = if (body.contains("@")) "@elena_r,@marcus_c" else "",
                    reactionsSummary = "🔥:3,👍:2",
                    edited = false
                )
            ).await()
        }

        // Parent Message 2 (Resources Topic with Document Attachment)
        val parentMsg2Id = "msg_arch_resource_2"
        val parentMsg2Content = "Uploaded the finalized Liquid Glass v3 token specification and contrast audit."
        val env2 = CryptoEngine.encryptMessage(parentMsg2Content, uid, groupConvId)
        groupRef.collection("messages").document(parentMsg2Id).set(
            ChatMessage(
                id = parentMsg2Id,
                userId = uid,
                conversationId = groupConvId,
                senderId = "member_devon",
                senderName = "Devon Brooks",
                senderRole = "Design Lead",
                topic = "Resources",
                content = parentMsg2Content,
                cipherText = env2.cipherTextBase64,
                ivBase64 = env2.ivBase64,
                attachmentType = "DOCUMENT",
                attachmentName = "Liquid_Glass_Spec_v3.pdf",
                attachmentMeta = "2.4 MB · AES-256-GCM Verified",
                replyCount = 1L,
                lastReplyPreview = "Reviewed the Glass Level 3 border opacity—looks razor sharp.",
                lastReplySender = "Elena Rostova",
                followingThread = true,
                threadMuted = false,
                unreadReplyCount = 0L,
                reactionsSummary = "✨:4",
                readCount = 22L,
                readersCsv = "Devon Brooks,Elena Rostova,Aria Lin,Marcus Chen",
                pinned = false,
                edited = false,
                deleted = false
            )
        ).await()

        groupRef.collection("messages").document(parentMsg2Id).collection("replies").document("rep_res_1").set(
            ThreadReplyItem(
                id = "rep_res_1",
                userId = uid,
                conversationId = groupConvId,
                parentMessageId = parentMsg2Id,
                senderId = "member_elena",
                senderName = "Elena Rostova",
                senderRole = "Lead Architect",
                content = "Reviewed the Glass Level 3 border opacity—looks razor sharp.",
                quotedSenderName = "",
                quotedPreview = "",
                mentionsSummary = "",
                reactionsSummary = "💜:2",
                edited = false
            )
        ).await()

        // Parent Message 3 (Announcements Topic)
        val parentMsg3Id = "msg_arch_announce_3"
        val parentMsg3Content = "Milestone 3 Release Candidate is live: Threaded group conversations now support side-by-side split panels on tablets and instant jump-to-parent navigation."
        val env3 = CryptoEngine.encryptMessage(parentMsg3Content, uid, groupConvId)
        groupRef.collection("messages").document(parentMsg3Id).set(
            ChatMessage(
                id = parentMsg3Id,
                userId = uid,
                conversationId = groupConvId,
                senderId = "member_aria",
                senderName = "Aria Lin",
                senderRole = "Security Architect",
                topic = "Announcements",
                content = parentMsg3Content,
                cipherText = env3.cipherTextBase64,
                ivBase64 = env3.ivBase64,
                attachmentType = "NONE",
                attachmentName = "",
                attachmentMeta = "",
                replyCount = 0L,
                lastReplyPreview = "",
                lastReplySender = "",
                followingThread = true,
                threadMuted = false,
                unreadReplyCount = 0L,
                reactionsSummary = "🎉:6,🚀:4",
                readCount = 26L,
                readersCsv = "Aria Lin,Elena Rostova,Marcus Chen,Devon Brooks",
                pinned = true,
                edited = false,
                deleted = false
            )
        ).await()

        // 2. Seed Private 1:1 E2EE Conversation: "Elena Rostova"
        val privConvId = "conv_private_elena"
        val privRef = db.collection("users").document(uid).collection("conversations").document(privConvId)
        privRef.set(
            ConversationItem(
                id = privConvId,
                userId = uid,
                title = "Elena Rostova",
                subtitle = "Online · Verified E2EE Key",
                type = "PRIVATE",
                encryptionMode = "E2EE_SIGNAL",
                memberCount = 2L,
                onlineCount = 2L,
                lastMessagePreview = "Sent you the updated ECDH pre-key bundle for our 1:1 session.",
                lastMessageSender = "Elena Rostova",
                unreadCount = 1L,
                pinned = true,
                muted = false,
                archived = false,
                topicsCsv = "General",
                resourcesCsv = "Safety_Fingerprint_Cert.sig",
                inviteCode = "PRIV-ELENA",
                avatarColor = "#54D6E8"
            )
        ).await()

        val privMsg1 = "Hey! Welcome to PaperMessages. Every 1:1 message here is sealed with Android Keystore ECDH + AES-GCM."
        val privEnv1 = CryptoEngine.encryptMessage(privMsg1, uid, privConvId)
        privRef.collection("messages").document("msg_priv_1").set(
            ChatMessage(
                id = "msg_priv_1",
                userId = uid,
                conversationId = privConvId,
                senderId = "member_elena",
                senderName = "Elena Rostova",
                senderRole = "Contact",
                topic = "General",
                content = privMsg1,
                cipherText = privEnv1.cipherTextBase64,
                ivBase64 = privEnv1.ivBase64,
                attachmentType = "NONE",
                attachmentName = "",
                attachmentMeta = "",
                replyCount = 0L,
                lastReplyPreview = "",
                lastReplySender = "",
                followingThread = false,
                threadMuted = false,
                unreadReplyCount = 0L,
                reactionsSummary = "🔒:2",
                readCount = 2L,
                readersCsv = "Elena Rostova,$displayName",
                pinned = false,
                edited = false,
                deleted = false
            )
        ).await()

        val privMsg2 = "Sent you the updated ECDH pre-key bundle for our 1:1 session."
        val privEnv2 = CryptoEngine.encryptMessage(privMsg2, uid, privConvId)
        privRef.collection("messages").document("msg_priv_2").set(
            ChatMessage(
                id = "msg_priv_2",
                userId = uid,
                conversationId = privConvId,
                senderId = "member_elena",
                senderName = "Elena Rostova",
                senderRole = "Contact",
                topic = "General",
                content = privMsg2,
                cipherText = privEnv2.cipherTextBase64,
                ivBase64 = privEnv2.ivBase64,
                attachmentType = "VOICE",
                attachmentName = "Voice_Note_Elena.m4a",
                attachmentMeta = "0:18 · E2EE Audio",
                replyCount = 0L,
                lastReplyPreview = "",
                lastReplySender = "",
                followingThread = false,
                threadMuted = false,
                unreadReplyCount = 0L,
                reactionsSummary = "✨:1",
                readCount = 2L,
                readersCsv = "Elena Rostova,$displayName",
                pinned = false,
                edited = false,
                deleted = false
            )
        ).await()

        // 3. Seed Second Group Conversation: "Cryptography & Privacy Guild"
        val cryptoConvId = "conv_crypto_guild"
        db.collection("users").document(uid).collection("conversations").document(cryptoConvId).set(
            ConversationItem(
                id = cryptoConvId,
                userId = uid,
                title = "Cryptography & Privacy Guild",
                subtitle = "164 members · 31 online",
                type = "GROUP",
                encryptionMode = "E2EE_MLS",
                memberCount = 164L,
                onlineCount = 31L,
                lastMessagePreview = "RFC 9420 TreeKEM epoch rotation benchmark results are in.",
                lastMessageSender = "Aria Lin",
                unreadCount = 0L,
                pinned = false,
                muted = false,
                archived = false,
                topicsCsv = "General,Announcements,Resources,Questions",
                resourcesCsv = "MLS_RFC9420_Notes.pdf|ZeroKnowledge_Identity.pdf",
                inviteCode = "PAPER-MLS-99",
                avatarColor = "#9B75FF"
            )
        ).await()

        // 4. Seed Communities (Joined, Discover, and Pending Invitations)
        val communities = listOf(
            CommunityItem(
                id = "comm_paper_builders",
                userId = uid,
                name = "PaperMessages Builders",
                handle = "paper_builders",
                description = "Official community for designers, systems engineers, and privacy advocates crafting next-gen threaded communication.",
                category = "Technology",
                memberCount = 1420L,
                unreadCount = 3L,
                joined = true,
                pendingInvite = false,
                publicAccess = true,
                inviteCode = "PAPER-BUILD",
                linkedConversationId = groupConvId,
                avatarColor = "#8B7CFF"
            ),
            CommunityItem(
                id = "comm_applied_crypto",
                userId = uid,
                name = "Applied Cryptography Collective",
                handle = "applied_crypto",
                description = "Deep dives into Signal Double Ratchet, MLS RFC 9420, Android Keystore hardware attestation, and post-quantum primitives.",
                category = "Security",
                memberCount = 890L,
                unreadCount = 0L,
                joined = true,
                pendingInvite = false,
                publicAccess = true,
                inviteCode = "CRYPTO-LAB",
                linkedConversationId = cryptoConvId,
                avatarColor = "#54D6E8"
            ),
            CommunityItem(
                id = "comm_spatial_ui",
                userId = uid,
                name = "Spatial & Liquid Glass UI",
                handle = "liquid_glass_ui",
                description = "Exploring translucent surfaces, specular borders, fluid spring motion, and dark-mode visual ergonomics.",
                category = "Design",
                memberCount = 3150L,
                unreadCount = 0L,
                joined = false,
                pendingInvite = false,
                publicAccess = true,
                inviteCode = "GLASS-UI-26",
                linkedConversationId = groupConvId,
                avatarColor = "#9B75FF"
            ),
            CommunityItem(
                id = "comm_founders_circle",
                userId = uid,
                name = "Zero-Rewrite Founders Circle",
                handle = "founders_circle",
                description = "Private invite-only forum on modular monoliths, sustainable cloud unit economics, and high-trust product launches.",
                category = "Startups",
                memberCount = 215L,
                unreadCount = 1L,
                joined = false,
                pendingInvite = true,
                publicAccess = false,
                inviteCode = "FOUNDER-VIP",
                linkedConversationId = groupConvId,
                avatarColor = "#43D9A3"
            )
        )
        for (comm in communities) {
            db.collection("users").document(uid).collection("communities").document(comm.id).set(comm).await()
        }

        // 5. Seed Broadcast Channels (with Polls & Announcements)
        val channels = listOf(
            ChannelItem(
                id = "chan_paper_releases",
                userId = uid,
                name = "PaperMessages Official Releases",
                handle = "paper_releases",
                description = "Product updates, security advisories, and Liquid Glass design system drops.",
                subscriberCount = 18400L,
                subscribed = true,
                adminRole = true,
                latestAnnouncement = "PaperMessages 2.4 introduces synchronized split-screen Thread Panels on foldables & tablets, plus hardware-backed Safety Number verification.",
                pinnedPostText = "Pinned: End-to-End Encryption Security Whitepaper & Modular Monolith Architecture Blueprint",
                pollQuestion = "Which thread workflow enhancement should we ship next?",
                pollOptionA = "AI-free local full-text regex filters inside threads",
                pollVotesA = 412L,
                pollOptionB = "Cross-topic thread linking & backlinks",
                pollVotesB = 628L,
                pollOptionC = "Scheduled thread digests for muted topics",
                pollVotesC = 295L,
                userVotedOption = "",
                reactionsSummary = "🚀:142,💜:89,🔥:64",
                avatarColor = "#8B7CFF"
            ),
            ChannelItem(
                id = "chan_security_bulletins",
                userId = uid,
                name = "Zero-Trust Security Bulletins",
                handle = "security_bulletins",
                description = "Independent cryptographic audit digests and key transparency checkpoints.",
                subscriberCount = 9320L,
                subscribed = true,
                adminRole = false,
                latestAnnouncement = "All 1:1 attachments are encrypted client-side with ephemeral 256-bit AES-GCM keys prior to pre-signed cloud upload.",
                pinnedPostText = "Quarterly Cryptographic Audit: 0 Critical or High findings.",
                pollQuestion = "Preferred default key rotation cadence for linked secondary devices?",
                pollOptionA = "Every 7 days (Recommended)",
                pollVotesA = 530L,
                pollOptionB = "Every 14 days",
                pollVotesB = 180L,
                pollOptionC = "Every 30 days",
                pollVotesC = 94L,
                userVotedOption = "A",
                reactionsSummary = "🔒:210,✨:54",
                avatarColor = "#54D6E8"
            )
        )
        for (chan in channels) {
            db.collection("users").document(uid).collection("channels").document(chan.id).set(chan).await()
        }

        // 6. Seed Call History
        val calls = listOf(
            CallRecordItem(
                id = "call_seed_1",
                userId = uid,
                conversationId = privConvId,
                contactName = "Elena Rostova",
                callType = "VIDEO",
                direction = "INCOMING",
                status = "ENDED",
                durationSeconds = 845L,
                avatarColor = "#54D6E8"
            ),
            CallRecordItem(
                id = "call_seed_2",
                userId = uid,
                conversationId = groupConvId,
                contactName = "Paper Architecture & Design",
                callType = "VOICE",
                direction = "OUTGOING",
                status = "ENDED",
                durationSeconds = 1920L,
                avatarColor = "#8B7CFF"
            ),
            CallRecordItem(
                id = "call_seed_3",
                userId = uid,
                conversationId = cryptoConvId,
                contactName = "Marcus Chen",
                callType = "VOICE",
                direction = "MISSED",
                status = "MISSED",
                durationSeconds = 0L,
                avatarColor = "#FF667D"
            )
        )
        for (call in calls) {
            db.collection("users").document(uid).collection("calls").document(call.id).set(call).await()
        }
    }

    private fun rethrowOrLog(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }
}
