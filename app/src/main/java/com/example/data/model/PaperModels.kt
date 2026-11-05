package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "+1",
    val bio: String = "",
    val avatarColor: String = "#8B7CFF",
    val publicKeyBase64: String = "",
    val keyFingerprint: String = "",
    val deviceName: String = "Android Device",
    val onboardingCompleted: Boolean = false,
    val online: Boolean = true,
    val privacyOnline: String = "EVERYONE",
    val privacyLastSeen: String = "EVERYONE",
    val privacyReadReceipts: Boolean = true,
    val privacyPhoneLookup: String = "CONTACTS",
    val reducedMotion: Boolean = false,
    val blockedUsersCsv: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun blockedList(): List<String> =
        blockedUsersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@IgnoreExtraProperties
data class ConversationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: String = "GROUP", // PRIVATE or GROUP
    val encryptionMode: String = "E2EE_MLS", // E2EE_SIGNAL, E2EE_MLS, TRANSPORT_TLS
    val memberCount: Long = 1L,
    val onlineCount: Long = 1L,
    val lastMessagePreview: String = "",
    val lastMessageSender: String = "",
    val unreadCount: Long = 0L,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val archived: Boolean = false,
    val topicsCsv: String = "General,Announcements,Resources,Questions",
    val resourcesCsv: String = "",
    val inviteCode: String = "",
    val avatarColor: String = "#8B7CFF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun topicsList(): List<String> =
        topicsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("General") }

    fun resourcesList(): List<String> =
        resourcesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "Member",
    val topic: String = "General",
    val content: String = "",
    val cipherText: String = "",
    val ivBase64: String = "",
    val attachmentType: String = "NONE", // NONE, IMAGE, VIDEO, DOCUMENT, VOICE, POLL
    val attachmentName: String = "",
    val attachmentMeta: String = "",
    val replyCount: Long = 0L,
    val lastReplyPreview: String = "",
    val lastReplySender: String = "",
    val followingThread: Boolean = true,
    val threadMuted: Boolean = false,
    val unreadReplyCount: Long = 0L,
    val reactionsSummary: String = "",
    val readCount: Long = 1L,
    val readersCsv: String = "",
    val pinned: Boolean = false,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedReactions(): Map<String, Int> {
        if (reactionsSummary.isBlank()) return emptyMap()
        return reactionsSummary.split(",")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size == 2) {
                    val emoji = parts[0].trim()
                    val count = parts[1].trim().toIntOrNull() ?: 0
                    if (emoji.isNotEmpty() && count > 0) emoji to count else null
                } else null
            }.toMap()
    }

    fun readersList(): List<String> =
        readersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@IgnoreExtraProperties
data class ThreadReplyItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val parentMessageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "Member",
    val content: String = "",
    val quotedSenderName: String = "",
    val quotedPreview: String = "",
    val mentionsSummary: String = "",
    val reactionsSummary: String = "",
    val edited: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedReactions(): Map<String, Int> {
        if (reactionsSummary.isBlank()) return emptyMap()
        return reactionsSummary.split(",")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size == 2) {
                    val emoji = parts[0].trim()
                    val count = parts[1].trim().toIntOrNull() ?: 0
                    if (emoji.isNotEmpty() && count > 0) emoji to count else null
                } else null
            }.toMap()
    }

    fun mentionsList(): List<String> =
        mentionsSummary.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@IgnoreExtraProperties
data class CommunityItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val handle: String = "",
    val description: String = "",
    val category: String = "Technology",
    val memberCount: Long = 1L,
    val unreadCount: Long = 0L,
    val joined: Boolean = false,
    val pendingInvite: Boolean = false,
    val publicAccess: Boolean = true,
    val inviteCode: String = "",
    val linkedConversationId: String = "",
    val avatarColor: String = "#8B7CFF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val handle: String = "",
    val description: String = "",
    val subscriberCount: Long = 1L,
    val subscribed: Boolean = true,
    val adminRole: Boolean = false,
    val latestAnnouncement: String = "",
    val pinnedPostText: String = "",
    val pollQuestion: String = "",
    val pollOptionA: String = "",
    val pollVotesA: Long = 0L,
    val pollOptionB: String = "",
    val pollVotesB: Long = 0L,
    val pollOptionC: String = "",
    val pollVotesC: Long = 0L,
    val userVotedOption: String = "",
    val reactionsSummary: String = "",
    val avatarColor: String = "#54D6E8",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun totalPollVotes(): Long = (pollVotesA + pollVotesB + pollVotesC).coerceAtLeast(1L)
}

@IgnoreExtraProperties
data class CallRecordItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val contactName: String = "",
    val callType: String = "VOICE", // VOICE or VIDEO
    val direction: String = "OUTGOING", // INCOMING, OUTGOING, MISSED
    val status: String = "ENDED", // RINGING, ACTIVE, ENDED, MISSED
    val durationSeconds: Long = 0L,
    val avatarColor: String = "#8B7CFF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModerationReportItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "MESSAGE",
    val targetId: String = "",
    val targetName: String = "",
    val reason: String = "Spam or Abuse",
    val details: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
