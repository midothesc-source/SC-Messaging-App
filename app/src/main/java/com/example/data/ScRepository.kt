package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScRepository(private val db: FirebaseFirestore) {

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

    fun observeChats(): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/chats"
        emitAll(
            db.collection("users").document(uid).collection("chats")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ChatThread> { it.isPinned }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/chats/$chatId/messages"
        emitAll(
            db.collection("users").document(uid).collection("chats").document(chatId)
                .collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeVaultItems(): Flow<List<VaultItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/vault_items"
        emitAll(
            db.collection("users").document(uid).collection("vault_items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VaultItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<VaultItem> { it.isPinned }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createChatThread(thread: ChatThread, customId: String? = null): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("chats")
        val docRef = if (!customId.isNullOrBlank()) colRef.document(customId) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to thread.name,
            "nameAr" to thread.nameAr,
            "avatarInitials" to thread.avatarInitials,
            "avatarColorHex" to thread.avatarColorHex,
            "spaceCategory" to thread.spaceCategory,
            "isPinned" to thread.isPinned,
            "unreadCount" to thread.unreadCount,
            "lastMessagePreview" to thread.lastMessagePreview,
            "lastMessagePreviewAr" to thread.lastMessagePreviewAr,
            "lastMessageTime" to thread.lastMessageTime,
            "isOnline" to thread.isOnline,
            "roleOrTitle" to thread.roleOrTitle,
            "roleOrTitleAr" to thread.roleOrTitleAr,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getChatById(chatId: String): Result<ChatThread?> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            val snap = docRef.get().await()
            snap.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun getChatByIdForUser(targetUserId: String, chatId: String): Result<ChatThread?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("chats").document(chatId)
        try {
            val snap = docRef.get().await()
            snap.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateChatMetadata(
        chatId: String,
        isPinned: Boolean? = null,
        unreadCount: Long? = null,
        lastMessagePreview: String? = null,
        lastMessagePreviewAr: String? = null,
        lastMessageTime: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isPinned != null) updates["isPinned"] = isPinned
        if (unreadCount != null) updates["unreadCount"] = unreadCount
        if (lastMessagePreview != null) updates["lastMessagePreview"] = lastMessagePreview
        if (lastMessagePreviewAr != null) updates["lastMessagePreviewAr"] = lastMessagePreviewAr
        if (lastMessageTime != null) updates["lastMessageTime"] = lastMessageTime

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendMessage(
        chatId: String,
        message: ChatMessage,
        customId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("chats").document(chatId)
            .collection("messages")
        val docRef = if (!customId.isNullOrBlank()) colRef.document(customId) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "chatId" to chatId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "textEn" to message.textEn,
            "textAr" to message.textAr,
            "messageType" to message.messageType,
            "voiceDurationSec" to message.voiceDurationSec,
            "voiceTranscriptEn" to message.voiceTranscriptEn,
            "voiceTranscriptAr" to message.voiceTranscriptAr,
            "mediaTitle" to message.mediaTitle,
            "mediaResolution" to message.mediaResolution,
            "mediaSizeLabel" to message.mediaSizeLabel,
            "mediaDrawableName" to message.mediaDrawableName,
            "status" to message.status,
            "translatedText" to message.translatedText,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMessageStatusOrTranslation(
        chatId: String,
        messageId: String,
        status: String? = null,
        translatedText: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages").document(messageId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (status != null) updates["status"] = status
        if (translatedText != null) updates["translatedText"] = translatedText
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addVaultItem(item: VaultItem, customId: String? = null): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("vault_items")
        val docRef = if (!customId.isNullOrBlank()) colRef.document(customId) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to item.title,
            "content" to item.content,
            "category" to item.category,
            "sizeOrDomain" to item.sizeOrDomain,
            "isPinned" to item.isPinned,
            "tag" to item.tag,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleVaultItemPin(itemId: String, isPinned: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("vault_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "isPinned" to isPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteVaultItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("vault_items").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val uid = auth.currentUser?.uid ?: return
        val chatsPath = "users/$uid/chats"
        try {
            val existingChats = db.collection("users").document(uid).collection("chats")
                .limit(1)
                .get()
                .await()
            if (!existingChats.isEmpty) return

            // 1. Seed 4 realistic bilingual Chat Threads across VIP, Work, and Family
            val threads = listOf(
                "chat_vip_tariq" to ChatThread(
                    name = "Tariq Al-Mansoor",
                    nameAr = "طارق المنصور",
                    avatarInitials = "TM",
                    avatarColorHex = "#D4AF37",
                    spaceCategory = "VIP",
                    isPinned = true,
                    unreadCount = 3L,
                    lastMessagePreview = "Sent 100% HD uncompressed pavilion master renders + voice brief",
                    lastMessagePreviewAr = "أرسل تصاميم الجناح بدقة 100% أصلية + ملاحظة صوتية",
                    lastMessageTime = "11:14 AM",
                    isOnline = true,
                    roleOrTitle = "Managing Partner • Sovereign Ventures",
                    roleOrTitleAr = "الشريك الإداري • سوفيرين فنتشرز"
                ),
                "chat_work_layla" to ChatThread(
                    name = "Layla Al-Hashemi",
                    nameAr = "ليلى الهاشمي",
                    avatarInitials = "LH",
                    avatarColorHex = "#10B981",
                    spaceCategory = "Work",
                    isPinned = true,
                    unreadCount = 2L,
                    lastMessagePreview = "SC Protocol v4 latency dropped below 18ms across MENA & EU nodes!",
                    lastMessagePreviewAr = "انخفض زمن استجابة بروتوكول SC v4 إلى أقل من 18 مللي ثانية!",
                    lastMessageTime = "10:45 AM",
                    isOnline = true,
                    roleOrTitle = "VP of Core Infrastructure",
                    roleOrTitleAr = "نائب رئيس البنية التحتية الأساسية"
                ),
                "chat_vip_zayd" to ChatThread(
                    name = "Zayd Vance",
                    nameAr = "زيد فانس",
                    avatarInitials = "ZV",
                    avatarColorHex = "#6366F1",
                    spaceCategory = "VIP",
                    isPinned = false,
                    unreadCount = 1L,
                    lastMessagePreview = "Private desert retreat aerials attached in 100% RAW quality.",
                    lastMessagePreviewAr = "الصور الجوية للمنتجع الصحراوي مرفقة بالجودة الأصلية 100%.",
                    lastMessageTime = "09:30 AM",
                    isOnline = false,
                    roleOrTitle = "Creative Director • Atelier Vance",
                    roleOrTitleAr = "المدير الإبداعي • أتيليه فانس"
                ),
                "chat_family_majlis" to ChatThread(
                    name = "Al-Noor Family Majlis",
                    nameAr = "مجلس عائلة النور",
                    avatarInitials = "AN",
                    avatarColorHex = "#F59E0B",
                    spaceCategory = "Family",
                    isPinned = false,
                    unreadCount = 2L,
                    lastMessagePreview = "Yasmine: Friday dinner at 8 PM on the terrace, everyone confirmed!",
                    lastMessagePreviewAr = "ياسمين: عشاء الجمعة الساعة 8 مساءً في التراس، الجميع أكد الحضور!",
                    lastMessageTime = "Yesterday",
                    isOnline = true,
                    roleOrTitle = "Family Space • 6 Members",
                    roleOrTitleAr = "مساحة العائلة • 6 أفراد"
                )
            )

            for ((chatId, thread) in threads) {
                createChatThread(thread, customId = chatId)
            }

            // 2. Seed rich messages for Tariq Al-Mansoor (VIP)
            val tariqMessages = listOf(
                ChatMessage(
                    senderId = "contact",
                    senderName = "Tariq Al-Mansoor",
                    textEn = "Good morning! The board reviewed the Dubai Glass Pavilion concept and approved the Phase 2 expansion budget ($42M).",
                    textAr = "صباح الخير! راجع مجلس الإدارة مفهوم جناح دبي الزجاجي ووافق على ميزانية توسعة المرحلة الثانية (42 مليون دولار).",
                    messageType = "TEXT",
                    status = "READ"
                ),
                ChatMessage(
                    senderId = "me",
                    senderName = "You",
                    textEn = "Fantastic news, Tariq. Please send the uncompressed master architectural renders so we don't lose any detail.",
                    textAr = "أخبار رائعة يا طارق. يرجى إرسال التصاميم المعمارية الأصلية غير المضغوطة حتى لا نفقد أي تفاصيل.",
                    messageType = "TEXT",
                    status = "READ"
                ),
                ChatMessage(
                    senderId = "contact",
                    senderName = "Tariq Al-Mansoor",
                    textEn = "Attached via SC Zero-Loss pipeline — 100% original pixel fidelity preserved.",
                    textAr = "مرفق عبر قناة SC بدون فقدان للجودة — تم الحفاظ على الدقة الأصلية بنسبة 100%.",
                    messageType = "HD_MEDIA",
                    mediaTitle = "Dubai_Pavilion_Twilight_Master_8K.raw",
                    mediaResolution = "7680×4320 • 100% HD Original",
                    mediaSizeLabel = "34.6 MB • Zero Compression",
                    mediaDrawableName = "img_hd_architecture",
                    status = "READ"
                ),
                ChatMessage(
                    senderId = "contact",
                    senderName = "Tariq Al-Mansoor",
                    textEn = "Voice Note (0:28)",
                    textAr = "ملاحظة صوتية (0:28)",
                    messageType = "VOICE",
                    voiceDurationSec = 28L,
                    voiceTranscriptEn = "Quick heads-up before our 4 PM call: we need the structural glazing vendor signed by Thursday so the acoustic glass panels ship before November 25th.",
                    voiceTranscriptAr = "تنبيه سريع قبل مكالمتنا الساعة 4 مساءً: نحتاج إلى توقيع مورد الزجاج الهيكلي بحلول يوم الخميس حتى يتم شحن الألواح الزجاجية الصوتية قبل 25 نوفمبر.",
                    status = "DELIVERED"
                )
            )
            tariqMessages.forEachIndexed { idx, msg ->
                sendMessage("chat_vip_tariq", msg, customId = "msg_tariq_$idx")
            }

            // 3. Seed messages for Layla Al-Hashemi (Work)
            val laylaMessages = listOf(
                ChatMessage(
                    senderId = "contact",
                    senderName = "Layla Al-Hashemi",
                    textEn = "We just deployed the new QUIC edge relays in Riyadh, Frankfurt, and Singapore.",
                    textAr = "لقد قمنا للتو بنشر خوادم QUIC الطرفية الجديدة في الرياض وفرانكفورت وسنغافورة.",
                    messageType = "TEXT",
                    status = "READ"
                ),
                ChatMessage(
                    senderId = "contact",
                    senderName = "Layla Al-Hashemi",
                    textEn = "Voice Note (0:19)",
                    textAr = "ملاحظة صوتية (0:19)",
                    messageType = "VOICE",
                    voiceDurationSec = 19L,
                    voiceTranscriptEn = "End-to-end encrypted voice notes now transcribe in under 300 milliseconds and 4K media transfers run at 3.2x WhatsApp speed with zero compression.",
                    voiceTranscriptAr = "يتم الآن تفريغ الملاحظات الصوتية المشفرة بالكامل إلى نص في أقل من 300 مللي ثانية، وتعمل عمليات نقل الوسائط بدقة 4K بسرعة 3.2 ضعف دون أي ضغط.",
                    status = "DELIVERED"
                ),
                ChatMessage(
                    senderId = "contact",
                    senderName = "Layla Al-Hashemi",
                    textEn = "Can you confirm if we should unlock the Obsidian Gold theme for all Founders Edition users tonight?",
                    textAr = "هل يمكنك تأكيد ما إذا كان يجب علينا تفعيل ثيم أوبسيديان الذهبي لجميع مستخدمي إصدار المؤسسين الليلة؟",
                    messageType = "TEXT",
                    status = "DELIVERED"
                )
            )
            laylaMessages.forEachIndexed { idx, msg ->
                sendMessage("chat_work_layla", msg, customId = "msg_layla_$idx")
            }

            // 4. Seed messages for Zayd Vance (VIP)
            val zaydMessages = listOf(
                ChatMessage(
                    senderId = "contact",
                    senderName = "Zayd Vance",
                    textEn = "Here is the aerial shot from the Rub' al Khali luxury desert camp shoot at golden hour.",
                    textAr = "إليك اللقطة الجوية من جلسة تصوير المخيم الصحراوي الفاخر في الربع الخالي وقت الغروب الذهبي.",
                    messageType = "HD_MEDIA",
                    mediaTitle = "Arabian_Dunes_Camp_Aerial_RAW.dng",
                    mediaResolution = "6000×3375 • 100% HD Original",
                    mediaSizeLabel = "29.2 MB • Lossless RAW",
                    mediaDrawableName = "img_hd_desert_dunes",
                    status = "READ"
                )
            )
            zaydMessages.forEachIndexed { idx, msg ->
                sendMessage("chat_vip_zayd", msg, customId = "msg_zayd_$idx")
            }

            // 5. Seed messages for Al-Noor Family Majlis (Family)
            val familyMessages = listOf(
                ChatMessage(
                    senderId = "contact",
                    senderName = "Yasmine Al-Noor",
                    textEn = "Mom made her signature saffron kunafa and pistachio baklava for Friday night!",
                    textAr = "أعدت أمي الكنافة بالزعفران والبقلاوة بالفستق المميزة لليلة الجمعة!",
                    messageType = "TEXT",
                    status = "READ"
                ),
                ChatMessage(
                    senderId = "contact",
                    senderName = "Omar Al-Noor",
                    textEn = "I'll bring the specialty Ethiopian pour-over coffee and Arabic cardamom roast. See you all at 8 PM!",
                    textAr = "سأحضر القهوة الإثيوبية المختصة والتحميص العربي بالهيل. أراكم جميعاً الساعة 8 مساءً!",
                    messageType = "TEXT",
                    status = "DELIVERED"
                )
            )
            familyMessages.forEachIndexed { idx, msg ->
                sendMessage("chat_family_majlis", msg, customId = "msg_family_$idx")
            }

            // 6. Seed SC Vault items (Notes, Links, Files)
            val vaultSamples = listOf(
                "vault_note_1" to VaultItem(
                    title = "Q4 Executive Keynotes & Sovereign Term Sheet",
                    content = "1. Finalize $42M Series B allocation with Tariq.\n2. Roll out SC AI Assistant bar (Summarize Unread + Instant Bilingual Translate).\n3. Keep 100% HD uncompressed media pipe default for VIP & Work spaces.",
                    category = "NOTE",
                    sizeOrDomain = "Encrypted Note • 2.4 KB",
                    isPinned = true,
                    tag = "Executive"
                ),
                "vault_link_1" to VaultItem(
                    title = "SC Zero-Knowledge Architecture Whitepaper",
                    content = "https://vault.sc-platform.io/specs/zk-quic-v4-architecture",
                    category = "LINK",
                    sizeOrDomain = "vault.sc-platform.io",
                    isPinned = true,
                    tag = "Engineering"
                ),
                "vault_file_1" to VaultItem(
                    title = "Dubai_Pavilion_Structural_Blueprint_Rev9.pdf",
                    content = "Uncompressed CAD & structural glazing specifications signed by Atelier Vance.",
                    category = "FILE",
                    sizeOrDomain = "48.2 MB • 100% Original PDF",
                    isPinned = false,
                    tag = "Architecture"
                )
            )
            for ((itemId, item) in vaultSamples) {
                addVaultItem(item, customId = itemId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, chatsPath)
        }
    }
}
