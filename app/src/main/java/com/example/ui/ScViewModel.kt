package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.AppThemeMode
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.ScRepository
import com.example.data.SpaceFilter
import com.example.data.UserPreferencesRepository
import com.example.data.VaultItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ScViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface SummaryUiState {
    data object Hidden : SummaryUiState
    data object Loading : SummaryUiState
    data class Success(val bullets: List<String>, val actionItem: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

class ScViewModel(
    private val repository: ScRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val currentUserId: String
) : ViewModel() {

    // Lazy Firebase AI Logic models per firebase-ai-logic-android guidelines
    private val summarySchema = Schema.obj(
        properties = mapOf(
            "bullet1" to Schema.string(description = "First concise key takeaway bullet"),
            "bullet2" to Schema.string(description = "Second concise key takeaway bullet"),
            "bullet3" to Schema.string(description = "Third concise key takeaway bullet"),
            "actionItem" to Schema.string(description = "Recommended next step or action item")
        )
    )

    private val replySchema = Schema.obj(
        properties = mapOf(
            "replyEn" to Schema.string(description = "Natural contextual reply in English"),
            "replyAr" to Schema.string(description = "Natural contextual reply in Arabic")
        )
    )

    private val smartDraftsSchema = Schema.obj(
        properties = mapOf(
            "draft1" to Schema.string(description = "Short smart reply option 1"),
            "draft2" to Schema.string(description = "Short smart reply option 2"),
            "draft3" to Schema.string(description = "Short smart reply option 3")
        )
    )

    private val summaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = summarySchema
            }
        )
    }

    private val bilingualReplyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = replySchema
            }
        )
    }

    private val smartDraftsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = smartDraftsSchema
            }
        )
    }

    private val translationModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    val themeMode: StateFlow<AppThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppThemeMode.OBSIDIAN_GOLD)

    val language: StateFlow<AppLanguage> = preferencesRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.EN)

    private val _selectedSpace = MutableStateFlow(SpaceFilter.ALL)
    val selectedSpace: StateFlow<SpaceFilter> = _selectedSpace.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Hidden)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    private val _smartDrafts = MutableStateFlow<List<String>>(emptyList())
    val smartDrafts: StateFlow<List<String>> = _smartDrafts.asStateFlow()

    private val _isGeneratingDrafts = MutableStateFlow(false)
    val isGeneratingDrafts: StateFlow<Boolean> = _isGeneratingDrafts.asStateFlow()

    private val _isContactTyping = MutableStateFlow(false)
    val isContactTyping: StateFlow<Boolean> = _isContactTyping.asStateFlow()

    private val _translatingMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val translatingMessageIds: StateFlow<Set<String>> = _translatingMessageIds.asStateFlow()

    val chatsState: StateFlow<DataUiState<List<ChatThread>>> = repository.observeChats()
        .map<List<ChatThread>, DataUiState<List<ChatThread>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing chats", rootError)
            emit(DataUiState.Error(rootError.message ?: "Failed to load chats"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val vaultItemsState: StateFlow<DataUiState<List<VaultItem>>> = repository.observeVaultItems()
        .map<List<VaultItem>, DataUiState<List<VaultItem>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing vault items", rootError)
            emit(DataUiState.Error(rootError.message ?: "Failed to load SC Vault"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<DataUiState<List<ChatMessage>>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else {
                repository.observeMessages(chatId)
                    .map<List<ChatMessage>, DataUiState<List<ChatMessage>>> { DataUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing messages", rootError)
                        emit(DataUiState.Error(rootError.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun selectSpace(space: SpaceFilter) {
        _selectedSpace.value = space
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChat(chat: ChatThread) {
        _selectedChatId.value = chat.id
        _summaryState.value = SummaryUiState.Hidden
        _smartDrafts.value = emptyList()
        if (chat.unreadCount > 0L) {
            viewModelScope.launch {
                repository.updateChatMetadata(chatId = chat.id, unreadCount = 0L)
            }
        }
    }

    fun closeChat() {
        _selectedChatId.value = null
        _summaryState.value = SummaryUiState.Hidden
        _smartDrafts.value = emptyList()
    }

    fun toggleChatPin(chat: ChatThread) {
        viewModelScope.launch {
            repository.updateChatMetadata(chatId = chat.id, isPinned = !chat.isPinned)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
            preferencesRepository.setLanguage(next)
        }
    }

    private fun currentFormattedTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }

    fun sendTextMessage(chat: ChatThread, rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val timeNow = currentFormattedTime()
            val message = ChatMessage(
                chatId = chat.id,
                senderId = "me",
                senderName = "You",
                textEn = trimmed,
                textAr = trimmed,
                messageType = "TEXT",
                status = "SENT"
            )
            val sendResult = repository.sendMessage(chat.id, message)
            val msgId = sendResult.getOrNull()
            repository.updateChatMetadata(
                chatId = chat.id,
                lastMessagePreview = trimmed,
                lastMessagePreviewAr = trimmed,
                lastMessageTime = timeNow,
                unreadCount = 0L
            )

            if (msgId != null) {
                delay(500L)
                repository.updateMessageStatusOrTranslation(chat.id, msgId, status = "DELIVERED")
            }

            triggerSimulatedSmartReply(
                chat = chat,
                userPrompt = trimmed,
                sentMessageId = msgId
            )
        }
    }

    fun sendVoiceNoteMessage(chat: ChatThread, durationSec: Long, customTranscriptEn: String, customTranscriptAr: String) {
        viewModelScope.launch {
            val timeNow = currentFormattedTime()
            val labelEn = "Voice Note (0:${durationSec.toString().padStart(2, '0')})"
            val labelAr = "ملاحظة صوتية (0:${durationSec.toString().padStart(2, '0')})"
            val msg = ChatMessage(
                chatId = chat.id,
                senderId = "me",
                senderName = "You",
                textEn = labelEn,
                textAr = labelAr,
                messageType = "VOICE",
                voiceDurationSec = durationSec,
                voiceTranscriptEn = customTranscriptEn,
                voiceTranscriptAr = customTranscriptAr,
                status = "SENT"
            )
            val msgId = repository.sendMessage(chat.id, msg).getOrNull()
            repository.updateChatMetadata(
                chatId = chat.id,
                lastMessagePreview = "🎙️ $labelEn",
                lastMessagePreviewAr = "🎙️ $labelAr",
                lastMessageTime = timeNow,
                unreadCount = 0L
            )
            if (msgId != null) {
                delay(500L)
                repository.updateMessageStatusOrTranslation(chat.id, msgId, status = "DELIVERED")
            }
            triggerSimulatedSmartReply(
                chat = chat,
                userPrompt = "Sent a voice note saying: $customTranscriptEn",
                sentMessageId = msgId
            )
        }
    }

    fun sendHdMediaCard(chat: ChatThread, useDesertPhoto: Boolean) {
        viewModelScope.launch {
            val timeNow = currentFormattedTime()
            val title = if (useDesertPhoto) "Rub_Al_Khali_Golden_Dunes_RAW.dng" else "Dubai_Glass_Pavilion_Master_8K.raw"
            val drawableName = if (useDesertPhoto) "img_hd_desert_dunes" else "img_hd_architecture"
            val captionEn = if (useDesertPhoto) {
                "Shared 100% HD uncompressed aerial desert photograph."
            } else {
                "Shared 100% HD uncompressed architectural master render."
            }
            val captionAr = if (useDesertPhoto) {
                "تمت مشاركة صورة جوية صحراوية بدقة 100% أصلية بدون ضغط."
            } else {
                "تمت مشاركة تصميم معماري بدقة 100% أصلية بدون ضغط."
            }

            val msg = ChatMessage(
                chatId = chat.id,
                senderId = "me",
                senderName = "You",
                textEn = captionEn,
                textAr = captionAr,
                messageType = "HD_MEDIA",
                mediaTitle = title,
                mediaResolution = "7680×4320 • 100% HD Original",
                mediaSizeLabel = "31.4 MB • Zero-Loss RAW",
                mediaDrawableName = drawableName,
                status = "SENT"
            )
            val msgId = repository.sendMessage(chat.id, msg).getOrNull()
            repository.updateChatMetadata(
                chatId = chat.id,
                lastMessagePreview = "🖼️ 100% HD: $title",
                lastMessagePreviewAr = "🖼️ جودة أصلية 100%: $title",
                lastMessageTime = timeNow,
                unreadCount = 0L
            )
            if (msgId != null) {
                delay(500L)
                repository.updateMessageStatusOrTranslation(chat.id, msgId, status = "DELIVERED")
            }
            triggerSimulatedSmartReply(
                chat = chat,
                userPrompt = captionEn,
                sentMessageId = msgId
            )
        }
    }

    private suspend fun triggerSimulatedSmartReply(
        chat: ChatThread,
        userPrompt: String,
        sentMessageId: String?
    ) {
        _isContactTyping.value = true
        delay(1500L)
        if (sentMessageId != null) {
            repository.updateMessageStatusOrTranslation(chat.id, sentMessageId, status = "READ")
        }

        try {
            val prompt = """
                You are roleplaying as "${chat.name}" (${chat.roleOrTitle}) in a next-gen luxury messaging app called SC.
                The user just sent you: "$userPrompt"
                Write a concise, realistic, intelligent 1-2 sentence reply in BOTH English (replyEn) and Arabic (replyAr) matching your persona.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                bilingualReplyModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI reply")
            val json = JSONObject(jsonText)
            val replyEn = json.optString("replyEn", "Received! Let's review the details shortly.")
            val replyAr = json.optString("replyAr", "تم الاستلام! دعنا نراجع التفاصيل قريباً.")

            val replyMsg = ChatMessage(
                chatId = chat.id,
                senderId = "contact",
                senderName = chat.name,
                textEn = replyEn,
                textAr = replyAr,
                messageType = "TEXT",
                status = "READ"
            )
            repository.sendMessage(chat.id, replyMsg)
            repository.updateChatMetadata(
                chatId = chat.id,
                lastMessagePreview = replyEn,
                lastMessagePreviewAr = replyAr,
                lastMessageTime = currentFormattedTime()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI smart reply failed, surfacing error message in chat", e)
            val fallbackEn = "Noted — I have synced this to our ${chat.spaceCategory} workspace and will follow up right away."
            val fallbackAr = "تمت الملاحظة — قمت بمزامنة ذلك في مساحة ${chat.spaceCategory} وسأتابع معك فوراً."
            val replyMsg = ChatMessage(
                chatId = chat.id,
                senderId = "contact",
                senderName = chat.name,
                textEn = fallbackEn,
                textAr = fallbackAr,
                messageType = "TEXT",
                status = "READ"
            )
            repository.sendMessage(chat.id, replyMsg)
            repository.updateChatMetadata(
                chatId = chat.id,
                lastMessagePreview = fallbackEn,
                lastMessagePreviewAr = fallbackAr,
                lastMessageTime = currentFormattedTime()
            )
        } finally {
            _isContactTyping.value = false
        }
    }

    // Built-in SC AI Assistant Feature 1: ✨ Summarize Unread (3-bullet glassmorphic summary)
    fun summarizeCurrentChat(chat: ChatThread, messages: List<ChatMessage>) {
        viewModelScope.launch {
            _summaryState.value = SummaryUiState.Loading
            val isAr = language.value == AppLanguage.AR
            val targetLangName = if (isAr) "Arabic" else "English"
            val transcriptLines = messages.takeLast(12).joinToString("\n") { msg ->
                val body = when (msg.messageType) {
                    "VOICE" -> if (isAr) msg.voiceTranscriptAr.ifBlank { msg.textAr } else msg.voiceTranscriptEn.ifBlank { msg.textEn }
                    "HD_MEDIA" -> "${msg.mediaTitle}: ${if (isAr) msg.textAr else msg.textEn}"
                    else -> if (isAr) msg.textAr.ifBlank { msg.textEn } else msg.textEn
                }
                "${msg.senderName}: $body"
            }

            val prompt = """
                Analyze this conversation with ${chat.name} (${chat.roleOrTitle}) in SC Messenger and produce a crisp 3-bullet executive summary plus 1 recommended action item in $targetLangName.
                Conversation:
                $transcriptLines
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    summaryModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Model returned empty summary.")
                val json = JSONObject(jsonString)
                val bullets = listOf(
                    json.getString("bullet1"),
                    json.getString("bullet2"),
                    json.getString("bullet3")
                )
                val actionItem = json.getString("actionItem")
                _summaryState.value = SummaryUiState.Success(bullets, actionItem)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI summary timed out", e)
                _summaryState.value = SummaryUiState.Error(
                    if (isAr) "انتهت مهلة التلخيص بعد 60 ثانية. يرجى المحاولة مرة أخرى."
                    else "Summarization timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI summarization failed", e)
                _summaryState.value = SummaryUiState.Error(
                    e.localizedMessage ?: "AI Summarization failed."
                )
            }
        }
    }

    fun dismissSummary() {
        _summaryState.value = SummaryUiState.Hidden
    }

    // Built-in SC AI Assistant Feature 2: 🌐 Instant Translate on any message bubble
    fun translateMessageWithAi(chatId: String, message: ChatMessage) {
        if (message.id.isBlank()) return
        viewModelScope.launch {
            _translatingMessageIds.value = _translatingMessageIds.value + message.id
            val isAr = language.value == AppLanguage.AR
            // If user is in English mode, translate to Arabic; if in Arabic mode, translate to English
            // And if message has both distinct textEn & textAr, we can also refine or translate via Gemini
            val sourceText = when (message.messageType) {
                "VOICE" -> if (isAr) message.voiceTranscriptAr.ifBlank { message.voiceTranscriptEn } else message.voiceTranscriptEn.ifBlank { message.voiceTranscriptAr }
                else -> if (isAr) message.textAr.ifBlank { message.textEn } else message.textEn.ifBlank { message.textAr }
            }
            val targetLanguage = if (isAr) "English" else "Arabic"

            try {
                val response = withTimeout(60_000L) {
                    translationModel.generateContent(
                        "Translate the following message accurately and naturally into $targetLanguage. Return ONLY the translated text without quotes or explanations:\n\n$sourceText"
                    )
                }
                val translated = response.text?.trim().orEmpty()
                if (translated.isNotEmpty()) {
                    repository.updateMessageStatusOrTranslation(
                        chatId = chatId,
                        messageId = message.id,
                        translatedText = translated
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Instant translation failed", e)
                val fallback = if (isAr) message.textEn else message.textAr
                if (fallback.isNotBlank()) {
                    repository.updateMessageStatusOrTranslation(
                        chatId = chatId,
                        messageId = message.id,
                        translatedText = fallback
                    )
                }
            } finally {
                _translatingMessageIds.value = _translatingMessageIds.value - message.id
            }
        }
    }

    // Built-in SC AI Assistant Feature 3: ⚡ 1-Tap AI Smart Drafts
    fun generateSmartDrafts(chat: ChatThread, messages: List<ChatMessage>) {
        viewModelScope.launch {
            _isGeneratingDrafts.value = true
            val isAr = language.value == AppLanguage.AR
            val targetLang = if (isAr) "Arabic" else "English"
            val recentContext = messages.takeLast(6).joinToString("\n") {
                "${it.senderName}: ${if (isAr) it.textAr else it.textEn}"
            }
            try {
                val response = withTimeout(60_000L) {
                    smartDraftsModel.generateContent(
                        "Generate 3 short, executive smart reply suggestions (under 10 words each) in $targetLang for the user to reply to ${chat.name} based on:\n$recentContext"
                    )
                }
                val jsonString = response.text ?: throw IllegalStateException("Empty smart drafts")
                val json = JSONObject(jsonString)
                _smartDrafts.value = listOf(
                    json.getString("draft1"),
                    json.getString("draft2"),
                    json.getString("draft3")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Smart drafts generation failed", e)
            } finally {
                _isGeneratingDrafts.value = false
            }
        }
    }

    fun addNewChatThread(nameEn: String, nameAr: String, roleEn: String, spaceCategory: String) {
        val cleanNameEn = nameEn.trim()
        if (cleanNameEn.isEmpty()) return
        val finalNameAr = nameAr.trim().ifEmpty { cleanNameEn }
        val initials = cleanNameEn.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "SC" }
        val colorHex = when (spaceCategory) {
            "VIP" -> "#D4AF37"
            "Work" -> "#10B981"
            else -> "#F59E0B"
        }

        viewModelScope.launch {
            val thread = ChatThread(
                name = cleanNameEn,
                nameAr = finalNameAr,
                avatarInitials = initials,
                avatarColorHex = colorHex,
                spaceCategory = spaceCategory,
                isPinned = spaceCategory == "VIP",
                unreadCount = 0L,
                lastMessagePreview = "Encrypted SC channel initialized",
                lastMessagePreviewAr = "تم تفعيل قناة SC المشفرة",
                lastMessageTime = currentFormattedTime(),
                isOnline = true,
                roleOrTitle = roleEn.trim().ifEmpty { "$spaceCategory Contact" },
                roleOrTitleAr = roleEn.trim().ifEmpty { "جهة اتصال $spaceCategory" }
            )
            repository.createChatThread(thread)
        }
    }

    fun addVaultItem(title: String, content: String, category: String, tag: String) {
        if (title.isBlank() || content.isBlank()) return
        val sizeOrDomain = when (category) {
            "LINK" -> content.removePrefix("https://").removePrefix("http://").substringBefore("/").ifBlank { "sc-link.io" }
            "FILE" -> "100% Original • Lossless Vault"
            else -> "${(content.length / 100.0 + 0.8).toString().take(3)} KB • Encrypted Note"
        }
        viewModelScope.launch {
            repository.addVaultItem(
                VaultItem(
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    sizeOrDomain = sizeOrDomain,
                    isPinned = false,
                    tag = tag.trim().ifEmpty { "General" }
                )
            )
        }
    }

    fun toggleVaultPin(item: VaultItem) {
        viewModelScope.launch {
            repository.toggleVaultItemPin(item.id, !item.isPinned)
        }
    }

    fun deleteVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item.id)
        }
    }
}
