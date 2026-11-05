package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createConversationAndThreadedReply_updatesParentSummary() = runBlocking {
        val uid = signInTestUser("alice_thread@test.com")
        val repository = PaperRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                displayName = "Alice Vance",
                username = "alice_vance",
                phoneNumber = "+15550199"
            )
        }
        assertTrue(profileResult.isSuccess)

        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConversation(
                title = "Engineering Thread Test",
                subtitle = "Test Group",
                type = "GROUP",
                customId = "conv_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            ).getOrThrow()
        }

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                conversationId = convId,
                senderId = uid,
                senderName = "Alice Vance",
                topic = "General",
                content = "How should we structure the thread panel?"
            ).getOrThrow()
        }

        val replyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addThreadReply(
                conversationId = convId,
                parentMessageId = msgId,
                senderId = uid,
                senderName = "Alice Vance",
                content = "@alice_vance Slide-in on phones and split-pane on tablets!"
            ).getOrThrow()
        }
        assertTrue(replyId.isNotEmpty())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(convId, uid).first { list ->
                list.any { it.id == msgId && it.replyCount == 1L }
            }
        }
        val updatedParent = messages.first { it.id == msgId }
        assertEquals(1L, updatedParent.replyCount)
        assertTrue(updatedParent.lastReplyPreview.contains("Slide-in on phones"))
    }

    @Test
    fun observeConversations_unauthenticated_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_unauth@test.com")
        auth.signOut()
        val repository = PaperRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeConversations(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected exception when unauthenticated", caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceConversations() = runBlocking {
        val aliceUid = signInTestUser("alice_owner@test.com")
        val aliceRepo = PaperRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createConversation(
                title = "Alice Private Group",
                subtitle = "Private",
                type = "GROUP"
            ).getOrThrow()
        }

        signInTestUser("bob_intruder@test.com")
        val bobRepo = PaperRepository(firestore)
        val bobAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getConversations(aliceUid)
        }
        assertTrue(bobAttempt.isFailure)
        val firestoreException = bobAttempt.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
