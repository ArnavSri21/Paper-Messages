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
                if (type == "GROUP") "1 member · 1 online" else "Online · End-to-End Encrypted"
            }.take(160),
            type = type,
            encryptionMode = encryptionMode,
            memberCount = if (type == "GROUP") 1L else 2L,
            onlineCount = 1L,
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

    suspend fun createChannel(
        name: String,
        handle: String,
        description: String,
        announcement: String,
        pollQuestion: String = "",
        pollOptionA: String = "",
        pollOptionB: String = "",
        pollOptionC: String = "",
        avatarColor: String = "#8B7CFF"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val chanId = "chan_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("users").document(uid).collection("channels").document(chanId)
        val cleanHandle = handle.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").take(40).ifBlank { "broadcast_channel" }
        val cleanAnnouncement = announcement.trim().ifBlank { description.trim().ifBlank { "Welcome to $name" } }.take(1500)
        val item = ChannelItem(
            id = chanId,
            userId = uid,
            name = name.trim().take(80),
            handle = cleanHandle,
            description = description.trim().take(500),
            subscriberCount = 1L,
            subscribed = true,
            adminRole = true,
            latestAnnouncement = cleanAnnouncement,
            pinnedPostText = cleanAnnouncement.take(500),
            pollQuestion = pollQuestion.trim().take(300),
            pollOptionA = pollOptionA.trim().take(120),
            pollVotesA = 0L,
            pollOptionB = pollOptionB.trim().take(120),
            pollVotesB = 0L,
            pollOptionC = pollOptionC.trim().take(120),
            pollVotesC = 0L,
            userVotedOption = "",
            reactionsSummary = "",
            avatarColor = avatarColor,
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(item).await()
            chanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
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
    // 7. LEGACY SEEDED DATA CLEANUP
    // =========================================================================

    suspend fun purgeLegacySeededData(fallbackDisplayName: String = ""): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)

        // 1. Delete legacy seeded conversations and their nested messages/replies
        val seededConversations = mapOf(
            "conv_paper_arch" to mapOf(
                "msg_arch_thread_1" to listOf("rep_1", "rep_2", "rep_3", "rep_4"),
                "msg_arch_resource_2" to listOf("rep_res_1"),
                "msg_arch_announce_3" to emptyList()
            ),
            "conv_private_elena" to mapOf(
                "msg_priv_1" to emptyList(),
                "msg_priv_2" to emptyList()
            ),
            "conv_crypto_guild" to emptyMap()
        )
        for ((convId, messagesMap) in seededConversations) {
            val convRef = userRef.collection("conversations").document(convId)
            for ((msgId, replyIds) in messagesMap) {
                val msgRef = convRef.collection("messages").document(msgId)
                for (repId in replyIds) {
                    runCatching { msgRef.collection("replies").document(repId).delete().await() }
                }
                runCatching { msgRef.delete().await() }
            }
            runCatching { convRef.delete().await() }
        }

        // 2. Delete legacy seeded communities
        val seededCommunities = listOf(
            "comm_paper_builders",
            "comm_applied_crypto",
            "comm_spatial_ui",
            "comm_founders_circle"
        )
        for (commId in seededCommunities) {
            runCatching { userRef.collection("communities").document(commId).delete().await() }
        }

        // 3. Delete legacy seeded channels
        val seededChannels = listOf(
            "chan_paper_releases",
            "chan_security_bulletins"
        )
        for (chanId in seededChannels) {
            runCatching { userRef.collection("channels").document(chanId).delete().await() }
        }

        // 4. Delete legacy seeded calls
        val seededCalls = listOf(
            "call_seed_1",
            "call_seed_2",
            "call_seed_3"
        )
        for (callId in seededCalls) {
            runCatching { userRef.collection("calls").document(callId).delete().await() }
        }

        // 5. If user profile still has legacy placeholder "Alex Mercer" or "5550192834", clean it
        runCatching {
            val snap = userRef.get().await()
            val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (profile != null && (profile.displayName == "Alex Mercer" || profile.phoneNumber == "5550192834")) {
                val realName = fallbackDisplayName.trim().takeIf { it.isNotBlank() && it != "Alex Mercer" } ?: "Paper User"
                val realHandle = if (profile.username == "alex_mercer") {
                    realName.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_').let {
                        if (it.length < 3) "user_${uid.take(6).lowercase()}" else it.take(24)
                    }
                } else {
                    profile.username
                }
                val realPhone = if (profile.phoneNumber == "5550192834") "" else profile.phoneNumber
                userRef.set(
                    profile.copy(
                        displayName = realName,
                        username = realHandle,
                        phoneNumber = realPhone,
                        updatedAt = null
                    )
                ).await()
            }
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
