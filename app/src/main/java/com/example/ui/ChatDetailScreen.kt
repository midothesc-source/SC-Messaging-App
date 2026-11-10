package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.ChatMessage
import com.example.data.ChatThread
import kotlinx.coroutines.delay

@Composable
fun ChatDetailPane(
    chat: ChatThread,
    messagesState: DataUiState<List<ChatMessage>>,
    summaryState: SummaryUiState,
    smartDrafts: List<String>,
    isGeneratingDrafts: Boolean,
    isContactTyping: Boolean,
    translatingMessageIds: Set<String>,
    language: AppLanguage,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onSendVoiceNote: (Long, String, String) -> Unit,
    onSendHdMedia: (Boolean) -> Unit,
    onSummarizeUnread: (List<ChatMessage>) -> Unit,
    onDismissSummary: () -> Unit,
    onGenerateSmartDrafts: (List<ChatMessage>) -> Unit,
    onTranslateMessage: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler { onBack() }
    }

    val isAr = language == AppLanguage.AR
    var messageInput by remember { mutableStateOf("") }
    var globalTranslateActive by remember { mutableStateOf(false) }
    val translatedBubbleToggles = remember { mutableStateMapOf<String, Boolean>() }
    val expandedTranscriptIds = remember { mutableStateMapOf<String, Boolean>() }
    var playingVoiceMessageId by remember { mutableStateOf<String?>(null) }
    var inspectedHdMedia by remember { mutableStateOf<ChatMessage?>(null) }
    var useDesertNext by remember { mutableStateOf(false) }

    val messages = (messagesState as? DataUiState.Success)?.data ?: emptyList()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isContactTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-stop voice note simulation after 5 seconds
    LaunchedEffect(playingVoiceMessageId) {
        if (playingVoiceMessageId != null) {
            delay(5000L)
            playingVoiceMessageId = null
        }
    }

    val avatarColor = remember(chat.avatarColorHex) {
        try {
            Color(AndroidColor.parseColor(chat.avatarColorHex))
        } catch (e: Exception) {
            Color(0xFFD4AF37)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // 1. Chat Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Chats"
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = avatarColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, avatarColor),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chat.avatarInitials,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = avatarColor
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isAr) chat.nameAr.ifBlank { chat.name } else chat.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified SC Contact",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (isContactTyping) {
                                if (isAr) "يكتب الآن..." else "typing smart response..."
                            } else {
                                if (isAr) "${chat.roleOrTitleAr} • متصل الآن" else "${chat.roleOrTitle} • Online E2EE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isContactTyping) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = chat.spaceCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 2. Built-in SC AI Assistant Bar (✨ Summarize Unread | 🌐 Instant Translate | ⚡ AI Smart Replies)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "SC AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isAr) "مساعد SC الذكي" else "SC AI BAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // ✨ Summarize Unread Button
                        AssistChip(
                            onClick = { onSummarizeUnread(messages) },
                            modifier = Modifier.testTag("ai_summarize_unread_button"),
                            label = {
                                Text(
                                    text = if (isAr) "✨ تلخيص المحادثة" else "✨ Summarize Unread",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // 🌐 Instant Translate All Toggle
                        AssistChip(
                            onClick = { globalTranslateActive = !globalTranslateActive },
                            modifier = Modifier.testTag("ai_instant_translate_all_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Instant Translate",
                                    tint = if (globalTranslateActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (globalTranslateActive) {
                                        if (isAr) "🌐 الترجمة الفورية: مفعلة" else "🌐 Instant Translate: ON"
                                    } else {
                                        if (isAr) "🌐 ترجمة فورية للكل" else "🌐 Instant Translate"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )

                        // ⚡ AI Smart Replies Generator
                        AssistChip(
                            onClick = { onGenerateSmartDrafts(messages) },
                            modifier = Modifier.testTag("ai_smart_drafts_button"),
                            label = {
                                Text(
                                    text = if (isGeneratingDrafts) {
                                        if (isAr) "⚡ جاري التوليد..." else "⚡ Drafting..."
                                    } else {
                                        if (isAr) "⚡ ردود ذكية" else "⚡ Smart Replies"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }
        }

        // 3. Sleek Glassmorphic 3-Bullet Summary Card (Pop-up inside chat when triggered)
        AnimatedVisibility(visible = summaryState !is SummaryUiState.Hidden) {
            GlassmorphicSummaryCard(
                summaryState = summaryState,
                isAr = isAr,
                onDismiss = onDismissSummary
            )
        }

        // 4. Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id.ifEmpty { it.hashCode().toString() } }) { message ->
                val isBubbleTranslated = globalTranslateActive || (translatedBubbleToggles[message.id] == true)
                val isTranscriptExpanded = expandedTranscriptIds[message.id] == true
                val isTranslatingThis = translatingMessageIds.contains(message.id)

                MessageBubbleItem(
                    message = message,
                    isAr = isAr,
                    showTranslated = isBubbleTranslated,
                    isTranslating = isTranslatingThis,
                    isTranscriptExpanded = isTranscriptExpanded,
                    isPlayingVoice = playingVoiceMessageId == message.id,
                    onToggleTranslate = {
                        val nextState = !(translatedBubbleToggles[message.id] ?: false)
                        translatedBubbleToggles[message.id] = nextState
                        if (nextState && message.translatedText.isBlank()) {
                            onTranslateMessage(message)
                        }
                    },
                    onToggleVoicePlay = {
                        playingVoiceMessageId = if (playingVoiceMessageId == message.id) null else message.id
                    },
                    onToggleTranscript = {
                        expandedTranscriptIds[message.id] = !isTranscriptExpanded
                    },
                    onInspectHdMedia = {
                        inspectedHdMedia = message
                    }
                )
            }

            if (isContactTyping) {
                item {
                    TypingIndicatorBubble(senderName = if (isAr) chat.nameAr else chat.name, isAr = isAr)
                }
            }
        }

        // 5. Optional AI Smart Reply Suggestions Bar
        AnimatedVisibility(visible = smartDrafts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                smartDrafts.forEachIndexed { idx, draft ->
                    AssistChip(
                        onClick = { onSendText(draft) },
                        label = { Text(draft, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("smart_draft_chip_$idx")
                    )
                }
            }
        }

        // 6. Rich Next-Gen Composer Bar (Text + Voice Note w/ Transcript + 100% HD Media Card)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Simulated Voice Note Sender Button
                    IconButton(
                        onClick = {
                            onSendVoiceNote(
                                16L,
                                "Confirmed! I reviewed the 100% HD renders and synced the updated schedule to SC Vault.",
                                "تم التأكيد! راجعت التصاميم بدقة 100% HD وقمت بمزامنة الجدول المحدث في خزنة SC."
                            )
                        },
                        modifier = Modifier.testTag("send_voice_note_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Send Voice Note with 1-Tap Transcript"
                        )
                    }

                    // 100% HD Media Sender Button
                    IconButton(
                        onClick = {
                            onSendHdMedia(useDesertNext)
                            useDesertNext = !useDesertNext
                        },
                        modifier = Modifier.testTag("send_hd_media_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hd,
                            contentDescription = "Send 100% HD Original Quality Media"
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        placeholder = {
                            Text(
                                text = if (isAr) "اكتب رسالة مشفرة..." else "Message in ${chat.spaceCategory} space...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    FilledIconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendText(messageInput)
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message"
                        )
                    }
                }
            }
        }
    }

    // Full-resolution HD Media Inspector Modal
    if (inspectedHdMedia != null) {
        val mediaMsg = inspectedHdMedia!!
        val drawableId = if (mediaMsg.mediaDrawableName == "img_hd_desert_dunes") {
            R.drawable.img_hd_desert_dunes
        } else {
            R.drawable.img_hd_architecture
        }
        AlertDialog(
            onDismissRequest = { inspectedHdMedia = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = "100% HD",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = mediaMsg.mediaTitle,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = mediaMsg.mediaTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "${mediaMsg.mediaResolution} • ${mediaMsg.mediaSizeLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAr) mediaMsg.textAr else mediaMsg.textEn,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { inspectedHdMedia = null }) {
                    Text(if (isAr) "إغلاق" else "Close Preview")
                }
            }
        )
    }
}

@Composable
private fun GlassmorphicSummaryCard(
    summaryState: SummaryUiState,
    isAr: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("ai_summary_glass_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "SC AI Summary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isAr) "ملخص الذكاء الاصطناعي (3 نقاط)" else "SC AI • 3-Bullet Conversation Summary",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Summary",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                when (summaryState) {
                    is SummaryUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (isAr) "يقوم مساعد SC بتحليل الرسائل..." else "Analyzing conversation context via Gemini...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is SummaryUiState.Error -> {
                        Text(
                            text = summaryState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is SummaryUiState.Success -> {
                        summaryState.bullets.forEach { bullet ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✦",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (if (isAr) "الإجراء المقترح: " else "Action Item: ") + summaryState.actionItem,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    SummaryUiState.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    isAr: Boolean,
    showTranslated: Boolean,
    isTranslating: Boolean,
    isTranscriptExpanded: Boolean,
    isPlayingVoice: Boolean,
    onToggleTranslate: () -> Unit,
    onToggleVoicePlay: () -> Unit,
    onToggleTranscript: () -> Unit,
    onInspectHdMedia: () -> Unit
) {
    val isMe = message.senderId == "me"
    val primaryText = if (isAr) message.textAr.ifBlank { message.textEn } else message.textEn.ifBlank { message.textAr }
    val alternateBilingualText = if (isAr) message.textEn else message.textAr
    val translatedDisplay = message.translatedText.ifBlank {
        if (alternateBilingualText != primaryText) alternateBilingualText else ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isMe) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }
            ),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message.messageType) {
                    "VOICE" -> {
                        VoiceNoteBubbleContent(
                            message = message,
                            isAr = isAr,
                            isPlaying = isPlayingVoice,
                            isTranscriptExpanded = isTranscriptExpanded,
                            onTogglePlay = onToggleVoicePlay,
                            onToggleTranscript = onToggleTranscript
                        )
                    }
                    "HD_MEDIA" -> {
                        HdMediaCardContent(
                            message = message,
                            isAr = isAr,
                            onInspect = onInspectHdMedia
                        )
                    }
                    else -> {
                        Text(
                            text = primaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 🌐 Instant Translate Expansion inside Bubble
                AnimatedVisibility(visible = showTranslated) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translated",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                if (isTranslating && translatedDisplay.isBlank()) {
                                    Text(
                                        text = if (isAr) "جاري الترجمة الفورية..." else "Translating via SC AI...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = translatedDisplay.ifBlank { primaryText },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Metadata Row: Instant Translate Toggle + Delivery Status Ticks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (showTranslated) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onToggleTranslate() }
                            .testTag("translate_bubble_${message.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Instant Translate Bubble",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isAr) "ترجمة فورية" else "Translate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isMe) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = message.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (message.status == "SENT") Icons.Default.Check else Icons.Default.DoneAll,
                                contentDescription = message.status,
                                tint = if (message.status == "READ") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteBubbleContent(
    message: ChatMessage,
    isAr: Boolean,
    isPlaying: Boolean,
    isTranscriptExpanded: Boolean,
    onTogglePlay: () -> Unit,
    onToggleTranscript: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePhase"
    )

    val barHeights = listOf(10, 18, 24, 14, 28, 20, 12, 26, 16, 22, 14, 25, 19, 12, 20, 15)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("voice_play_button_${message.id}"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note"
                )
            }

            // Animated Waveform Bars
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                barHeights.forEachIndexed { idx, baseHeight ->
                    val dynamicScale = if (isPlaying) {
                        if (idx % 2 == 0) wavePhase else (1.35f - wavePhase)
                    } else {
                        1f
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((baseHeight * dynamicScale).dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isPlaying || idx < 6) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                    )
                }
            }

            Text(
                text = "0:${message.voiceDurationSec.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 1-Tap Transcript Pill Button
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleTranscript() }
                .testTag("voice_transcript_button_${message.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = "1-Tap Transcript",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isTranscriptExpanded) {
                        if (isAr) "إخفاء النص الصوتي" else "Hide 1-Tap Transcript"
                    } else {
                        if (isAr) "تفريغ نصي بنقرة واحدة ✨" else "1-Tap Transcript ✨"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(visible = isTranscriptExpanded) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) {
                        message.voiceTranscriptAr.ifBlank { message.voiceTranscriptEn }
                    } else {
                        message.voiceTranscriptEn.ifBlank { message.voiceTranscriptAr }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun HdMediaCardContent(
    message: ChatMessage,
    isAr: Boolean,
    onInspect: () -> Unit
) {
    val drawableId = if (message.mediaDrawableName == "img_hd_desert_dunes") {
        R.drawable.img_hd_desert_dunes
    } else {
        R.drawable.img_hd_architecture
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("hd_media_card_${message.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = message.mediaTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Top-right 100% HD Original Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC0A0B0E),
                border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = "100% HD",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isAr) "جودة أصلية 100% HD" else "ORIGINAL QUALITY 100% HD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }

        Text(
            text = message.mediaTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "${message.mediaResolution} • ${message.mediaSizeLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (isAr) message.textAr.ifBlank { message.textEn } else message.textEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TypingIndicatorBubble(senderName: String, isAr: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Typing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isAr) "$senderName يكتب رداً ذكياً..." else "$senderName is typing...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
