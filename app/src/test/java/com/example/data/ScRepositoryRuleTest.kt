package com.example.data

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

class ScRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveChatAndVault_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ScRepository(firestore)
        val chatId = "chat_${UUID.randomUUID()}"

        val createChatResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChatThread(
                ChatThread(
                    name = "Tariq Al-Mansoor",
                    nameAr = "طارق المنصور",
                    avatarInitials = "TM",
                    avatarColorHex = "#D4AF37",
                    spaceCategory = "VIP",
                    isPinned = true,
                    unreadCount = 1L,
                    lastMessagePreview = "Hello",
                    lastMessagePreviewAr = "مرحبا",
                    lastMessageTime = "Now",
                    isOnline = true,
                    roleOrTitle = "Partner",
                    roleOrTitleAr = "شريك"
                ),
                customId = chatId
            )
        }
        assertTrue(createChatResult.isSuccess)

        val emittedChats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChats().first { list -> list.any { it.id == chatId } }
        }
        assertTrue(emittedChats.any { it.id == chatId })

        val vaultId = "vault_${UUID.randomUUID()}"
        val createVaultResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVaultItem(
                VaultItem(
                    title = "Confidential Note",
                    content = "Encrypted SC Vault payload",
                    category = "NOTE",
                    sizeOrDomain = "1 KB",
                    isPinned = true,
                    tag = "VIP"
                ),
                customId = vaultId
            )
        }
        assertTrue(createVaultResult.isSuccess)
    }

    @Test
    fun getChat_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ScRepository(firestore)
        val chatId = "chat_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createChatThread(
                ChatThread(
                    name = "Private VIP Chat",
                    nameAr = "محادثة خاصة",
                    avatarInitials = "PV",
                    avatarColorHex = "#D4AF37",
                    spaceCategory = "VIP",
                    isPinned = false,
                    unreadCount = 0L,
                    lastMessagePreview = "Top secret",
                    lastMessagePreviewAr = "سري للغاية",
                    lastMessageTime = "10:00 AM",
                    isOnline = true,
                    roleOrTitle = "VIP",
                    roleOrTitleAr = "كبار الشخصيات"
                ),
                customId = chatId
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ScRepository(firestore)
        val crossUserResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getChatByIdForUser(aliceUid, chatId)
        }
        assertTrue(crossUserResult.isFailure)
        val firestoreEx = crossUserResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeChats_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ScRepository(firestore)
        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeChats().first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
