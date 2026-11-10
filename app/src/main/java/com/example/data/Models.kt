package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class AppThemeMode(val id: String, val labelEn: String, val labelAr: String) {
    OBSIDIAN_GOLD("obsidian_gold", "SC Obsidian Gold", "إس سي أوبسيديان الذهبي"),
    EMERALD_DARK("emerald_dark", "Emerald Dark", "الزمرد الداكن"),
    CLEAN_LIGHT("clean_light", "Clean Light", "الوضع النقي الفاتح");

    companion object {
        fun fromId(id: String): AppThemeMode = entries.find { it.id == id } ?: OBSIDIAN_GOLD
    }
}

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "EN"),
    AR("ar", "عربي");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: EN
    }
}

enum class SpaceFilter(val key: String, val labelEn: String, val labelAr: String) {
    ALL("All", "All", "الكل"),
    VIP("VIP", "VIP ⭐", "كبار الشخصيات ⭐"),
    WORK("Work", "Work 💼", "العمل 💼"),
    FAMILY("Family", "Family 🏠", "العائلة 🏠"),
    VAULT("Vault", "SC Vault 🔒", "خزنة SC 🔒")
}

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val nameAr: String = "",
    val avatarInitials: String = "",
    val avatarColorHex: String = "#D4AF37",
    val spaceCategory: String = "VIP", // "VIP", "Work", "Family"
    @get:PropertyName("isPinned") @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    val unreadCount: Long = 0L,
    val lastMessagePreview: String = "",
    val lastMessagePreviewAr: String = "",
    val lastMessageTime: String = "",
    @get:PropertyName("isOnline") @set:PropertyName("isOnline")
    var isOnline: Boolean = true,
    val roleOrTitle: String = "",
    val roleOrTitleAr: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val senderId: String = "me", // "me", "contact", "ai"
    val senderName: String = "",
    val textEn: String = "",
    val textAr: String = "",
    val messageType: String = "TEXT", // "TEXT", "VOICE", "HD_MEDIA"
    val voiceDurationSec: Long = 0L,
    val voiceTranscriptEn: String = "",
    val voiceTranscriptAr: String = "",
    val mediaTitle: String = "",
    val mediaResolution: String = "",
    val mediaSizeLabel: String = "",
    val mediaDrawableName: String = "",
    val status: String = "READ", // "SENT", "DELIVERED", "READ"
    val translatedText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VaultItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "NOTE", // "NOTE", "LINK", "FILE"
    val sizeOrDomain: String = "",
    @get:PropertyName("isPinned") @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    val tag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
