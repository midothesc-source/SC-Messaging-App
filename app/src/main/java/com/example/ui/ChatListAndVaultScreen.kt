package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.AppThemeMode
import com.example.data.ChatThread
import com.example.data.SpaceFilter
import com.example.data.VaultItem

@Composable
fun ChatListAndVaultPane(
    chatsState: DataUiState<List<ChatThread>>,
    vaultItemsState: DataUiState<List<VaultItem>>,
    selectedSpace: SpaceFilter,
    searchQuery: String,
    activeChatId: String?,
    language: AppLanguage,
    themeMode: AppThemeMode,
    onSelectSpace: (SpaceFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenChat: (ChatThread) -> Unit,
    onToggleChatPin: (ChatThread) -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onAddNewChat: (String, String, String, String) -> Unit,
    onAddVaultItem: (String, String, String, String) -> Unit,
    onToggleVaultPin: (VaultItem) -> Unit,
    onDeleteVaultItem: (VaultItem) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.AR
    var showThemeMenu by remember { mutableStateOf(false) }
    var showAddChatDialog by remember { mutableStateOf(false) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var selectedVaultCategory by remember { mutableStateOf("ALL") }

    val allChats = (chatsState as? DataUiState.Success)?.data ?: emptyList()
    val allVaultItems = (vaultItemsState as? DataUiState.Success)?.data ?: emptyList()

    val filteredChats = remember(allChats, selectedSpace, searchQuery) {
        allChats.filter { chat ->
            val matchesSpace = when (selectedSpace) {
                SpaceFilter.ALL -> true
                SpaceFilter.VIP -> chat.spaceCategory.equals("VIP", ignoreCase = true)
                SpaceFilter.WORK -> chat.spaceCategory.equals("Work", ignoreCase = true)
                SpaceFilter.FAMILY -> chat.spaceCategory.equals("Family", ignoreCase = true)
                SpaceFilter.VAULT -> false
            }
            val matchesQuery = searchQuery.isBlank() ||
                chat.name.contains(searchQuery, ignoreCase = true) ||
                chat.nameAr.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessagePreview.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessagePreviewAr.contains(searchQuery, ignoreCase = true)
            matchesSpace && matchesQuery
        }
    }

    val filteredVaultItems = remember(allVaultItems, selectedVaultCategory, searchQuery) {
        allVaultItems.filter { item ->
            val matchesCat = selectedVaultCategory == "ALL" || item.category == selectedVaultCategory
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.content.contains(searchQuery, ignoreCase = true) ||
                item.tag.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Executive Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_sc_logo_fg_1794338864968),
                                contentDescription = "SC Logo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SC",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "مشفر فائق" else "ULTRA HD",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when (themeMode) {
                                        AppThemeMode.OBSIDIAN_GOLD -> if (isAr) "ثيم أوبسيديان الذهبي" else "Obsidian Gold Edition"
                                        AppThemeMode.EMERALD_DARK -> if (isAr) "ثيم الزمرد الداكن" else "Emerald Dark Edition"
                                        AppThemeMode.CLEAN_LIGHT -> if (isAr) "الوضع النقي الفاتح" else "Clean Light Edition"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action Controls: Bilingual Toggle, Theme Picker, Sign Out
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onToggleLanguage() }
                                    .testTag("language_toggle_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Toggle Language",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isAr) "EN" else "عربي",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showThemeMenu = true },
                                    modifier = Modifier.testTag("theme_selector_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Change Theme",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showThemeMenu,
                                    onDismissRequest = { showThemeMenu = false }
                                ) {
                                    AppThemeMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isAr) mode.labelAr else mode.labelEn,
                                                    fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (mode == themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                onSelectTheme(mode)
                                                showThemeMenu = false
                                            },
                                            modifier = Modifier.testTag("theme_option_${mode.id}")
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        placeholder = {
                            Text(
                                text = if (isAr) "ابحث في المحادثات أو المساحات الذكية أو خزنة SC..." else "Search chats, Smart Spaces, or SC Vault...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smart Spaces Filter Bar (All, VIP ⭐, Work 💼, Family 🏠, SC Vault 🔒)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpaceFilter.entries.forEach { space ->
                            val isSelected = selectedSpace == space
                            val unreadForSpace = when (space) {
                                SpaceFilter.ALL -> allChats.sumOf { it.unreadCount }
                                SpaceFilter.VIP -> allChats.filter { it.spaceCategory.equals("VIP", true) }.sumOf { it.unreadCount }
                                SpaceFilter.WORK -> allChats.filter { it.spaceCategory.equals("Work", true) }.sumOf { it.unreadCount }
                                SpaceFilter.FAMILY -> allChats.filter { it.spaceCategory.equals("Family", true) }.sumOf { it.unreadCount }
                                SpaceFilter.VAULT -> allVaultItems.size.toLong()
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSpace(space) },
                                modifier = Modifier.testTag("space_pill_${space.key.lowercase()}"),
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) space.labelAr else space.labelEn,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        if (unreadForSpace > 0L) {
                                            Badge(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                },
                                                contentColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            ) {
                                                Text(
                                                    text = unreadForSpace.toString(),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Main Body: Either SC Vault 🔒 View or Filtered Chat List
            if (selectedSpace == SpaceFilter.VAULT) {
                ScVaultContent(
                    vaultState = vaultItemsState,
                    filteredItems = filteredVaultItems,
                    selectedCategory = selectedVaultCategory,
                    isAr = isAr,
                    onSelectCategory = { selectedVaultCategory = it },
                    onTogglePin = onToggleVaultPin,
                    onDelete = onDeleteVaultItem,
                    modifier = Modifier.weight(1f)
                )
            } else {
                when (chatsState) {
                    is DataUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is DataUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chatsState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is DataUiState.Success -> {
                        if (filteredChats.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isAr) "لا توجد محادثات في هذه المساحة" else "No conversations in this Smart Space",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isAr) "اضغط على زر + لبدء محادثة جديدة" else "Tap the + button to start a new encrypted chat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("chat_thread_list"),
                                contentPadding = PaddingValues(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 10.dp,
                                    bottom = 88.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredChats, key = { it.id }) { chat ->
                                    ChatThreadItemCard(
                                        chat = chat,
                                        isSelected = chat.id == activeChatId,
                                        isAr = isAr,
                                        onClick = { onOpenChat(chat) },
                                        onTogglePin = { onToggleChatPin(chat) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for New Chat or New Vault Item
        FloatingActionButton(
            onClick = {
                if (selectedSpace == SpaceFilter.VAULT) {
                    showAddVaultDialog = true
                } else {
                    showAddChatDialog = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("primary_create_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (selectedSpace == SpaceFilter.VAULT) "Add Vault Item" else "New Chat"
            )
        }
    }

    if (showAddChatDialog) {
        NewChatDialog(
            isAr = isAr,
            onDismiss = { showAddChatDialog = false },
            onConfirm = { nameEn, nameAr, roleEn, space ->
                onAddNewChat(nameEn, nameAr, roleEn, space)
                showAddChatDialog = false
            }
        )
    }

    if (showAddVaultDialog) {
        NewVaultItemDialog(
            isAr = isAr,
            onDismiss = { showAddVaultDialog = false },
            onConfirm = { title, content, category, tag ->
                onAddVaultItem(title, content, category, tag)
                showAddVaultDialog = false
            }
        )
    }
}

@Composable
private fun ChatThreadItemCard(
    chat: ChatThread,
    isSelected: Boolean,
    isAr: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    val avatarColor = remember(chat.avatarColorHex) {
        try {
            Color(AndroidColor.parseColor(chat.avatarColorHex))
        } catch (e: Exception) {
            Color(0xFFD4AF37)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("chat_card_${chat.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected || chat.isPinned) 1.dp else 0.5.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else if (chat.isPinned) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with Online Pulse Dot
            Box(modifier = Modifier.size(54.dp)) {
                Surface(
                    shape = CircleShape,
                    color = avatarColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, avatarColor),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chat.avatarInitials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = avatarColor
                        )
                    }
                }
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
            }

            // Middle Info Column
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Space Tag Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = when (chat.spaceCategory) {
                                "VIP" -> "VIP ⭐"
                                "Work" -> if (isAr) "العمل 💼" else "Work 💼"
                                else -> if (isAr) "العائلة 🏠" else "Family 🏠"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (isAr) chat.roleOrTitleAr.ifBlank { chat.roleOrTitle } else chat.roleOrTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isAr) chat.lastMessagePreviewAr.ifBlank { chat.lastMessagePreview } else chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chat.unreadCount > 0L) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (chat.unreadCount > 0L) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Column: Time, Pin Toggle, Unread Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = chat.lastMessageTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0L) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("pin_button_${chat.id}")
                    ) {
                        Icon(
                            imageVector = if (chat.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Chat",
                            tint = if (chat.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (chat.unreadCount > 0L) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScVaultContent(
    vaultState: DataUiState<List<VaultItem>>,
    filteredItems: List<VaultItem>,
    selectedCategory: String,
    isAr: Boolean,
    onSelectCategory: (String) -> Unit,
    onTogglePin: (VaultItem) -> Unit,
    onDelete: (VaultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // SC Vault Hero Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "SC Vault",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "خزنة SC السحابية الخاصة 🔒" else "SC Vault • Personal Cloud Space 🔒",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAr) {
                                "مساحتك المنظمة والمشفرة للملاحظات والروابط والملفات الأصلية 100% HD."
                            } else {
                                "Zero-knowledge encrypted cloud storage for your executive notes, links & 100% HD files."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vault Category Sub-Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "ALL" to (if (isAr) "الكل" else "All Vault"),
                "NOTE" to (if (isAr) "ملاحظات 📝" else "Notes 📝"),
                "LINK" to (if (isAr) "روابط 🔗" else "Links 🔗"),
                "FILE" to (if (isAr) "ملفات 📁" else "Files 📁")
            )
            categories.forEach { (catKey, label) ->
                FilterChip(
                    selected = selectedCategory == catKey,
                    onClick = { onSelectCategory(catKey) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("vault_filter_$catKey")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (vaultState) {
            is DataUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is DataUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(vaultState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is DataUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sc_vault_list"),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onTogglePin = { onTogglePin(item) },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItem,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (item.category) {
        "LINK" -> Icons.Default.Link
        "FILE" -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isPinned) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = item.category,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.sizeOrDomain,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.tag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "#${item.tag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Filled.Bookmark else Icons.Outlined.PushPin,
                        contentDescription = "Pin Vault Item",
                        tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Vault Item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewChatDialog(
    isAr: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var roleEn by remember { mutableStateOf("") }
    var space by remember { mutableStateOf("VIP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isAr) "محادثة مشفرة جديدة" else "New SC Encrypted Chat")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isAr) "الاسم (إنجليزي)" else "Contact Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isAr) "الاسم (عربي - اختياري)" else "Contact Name (Arabic - Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleEn,
                    onValueChange = { roleEn = it },
                    label = { Text(if (isAr) "المنصب أو الوصف" else "Role / Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VIP", "Work", "Family").forEach { sp ->
                        FilterChip(
                            selected = space == sp,
                            onClick = { space = sp },
                            label = { Text(sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameEn, nameAr, roleEn, space) },
                enabled = nameEn.isNotBlank()
            ) {
                Text(if (isAr) "إنشاء" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
private fun NewVaultItemDialog(
    isAr: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("NOTE") }
    var tag by remember { mutableStateOf("VIP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isAr) "إضافة عنصر إلى خزنة SC 🔒" else "Save to SC Vault 🔒")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NOTE", "LINK", "FILE").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isAr) "العنوان" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (isAr) "المحتوى أو الرابط" else "Note Content / URL / File Specs") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(if (isAr) "الوسم" else "Tag (e.g. Executive)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, category, tag) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text(if (isAr) "حفظ في الخزنة" else "Save to Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Cancel")
            }
        }
    )
}
