package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppLanguage
import com.example.data.AppThemeMode
import com.example.data.ScRepository
import com.example.data.UserPreferencesRepository
import com.example.data.configureAppCheck
import com.example.ui.AuthScreen
import com.example.ui.ChatDetailPane
import com.example.ui.ChatListAndVaultPane
import com.example.ui.DataUiState
import com.example.ui.ScViewModel
import com.example.ui.signOut
import com.example.ui.theme.ScTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ScRootApp()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun ScRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember { UserPreferencesRepository(context.applicationContext) }
    val themeMode by prefsRepo.themeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.OBSIDIAN_GOLD)
    val language by prefsRepo.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    ScTheme(themeMode = themeMode, language = language) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                language = language,
                themeMode = themeMode,
                onToggleLanguage = {
                    scope.launch {
                        val next = if (language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
                        prefsRepo.setLanguage(next)
                    }
                },
                onSelectTheme = { mode ->
                    scope.launch {
                        prefsRepo.setThemeMode(mode)
                    }
                },
                onAuthSuccess = {}
            )
        } else {
            key(user.uid) {
                AuthenticatedScWorkspace(currentUserId = user.uid)
            }
        }
    }
}

@Composable
fun AuthenticatedScWorkspace(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ScViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ScViewModel(
                    repository = ScRepository(db),
                    preferencesRepository = UserPreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val vaultItemsState by viewModel.vaultItemsState.collectAsStateWithLifecycle()
    val selectedSpace by viewModel.selectedSpace.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()
    val smartDrafts by viewModel.smartDrafts.collectAsStateWithLifecycle()
    val isGeneratingDrafts by viewModel.isGeneratingDrafts.collectAsStateWithLifecycle()
    val isContactTyping by viewModel.isContactTyping.collectAsStateWithLifecycle()
    val translatingMessageIds by viewModel.translatingMessageIds.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val allChats = (chatsState as? DataUiState.Success)?.data ?: emptyList()
    val activeChat = allChats.find { it.id == selectedChatId }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 720.dp

            if (isExpandedScreen) {
                // Canonical List-Detail Desktop / Tablet Pro Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    ChatListAndVaultPane(
                        chatsState = chatsState,
                        vaultItemsState = vaultItemsState,
                        selectedSpace = selectedSpace,
                        searchQuery = searchQuery,
                        activeChatId = selectedChatId,
                        language = language,
                        themeMode = themeMode,
                        onSelectSpace = viewModel::selectSpace,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onOpenChat = viewModel::openChat,
                        onToggleChatPin = viewModel::toggleChatPin,
                        onToggleLanguage = viewModel::toggleLanguage,
                        onSelectTheme = viewModel::setThemeMode,
                        onAddNewChat = viewModel::addNewChatThread,
                        onAddVaultItem = viewModel::addVaultItem,
                        onToggleVaultPin = viewModel::toggleVaultPin,
                        onDeleteVaultItem = viewModel::deleteVaultItem,
                        onSignOut = {
                            signOut(credentialManager, scope)
                        },
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                    )

                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    if (activeChat != null) {
                        ChatDetailPane(
                            chat = activeChat,
                            messagesState = activeMessagesState,
                            summaryState = summaryState,
                            smartDrafts = smartDrafts,
                            isGeneratingDrafts = isGeneratingDrafts,
                            isContactTyping = isContactTyping,
                            translatingMessageIds = translatingMessageIds,
                            language = language,
                            showBackButton = false,
                            onBack = viewModel::closeChat,
                            onSendText = { text -> viewModel.sendTextMessage(activeChat, text) },
                            onSendVoiceNote = { dur, en, ar -> viewModel.sendVoiceNoteMessage(activeChat, dur, en, ar) },
                            onSendHdMedia = { useDesert -> viewModel.sendHdMediaCard(activeChat, useDesert) },
                            onSummarizeUnread = { msgs -> viewModel.summarizeCurrentChat(activeChat, msgs) },
                            onDismissSummary = viewModel::dismissSummary,
                            onGenerateSmartDrafts = { msgs -> viewModel.generateSmartDrafts(activeChat, msgs) },
                            onTranslateMessage = { msg -> viewModel.translateMessageWithAi(activeChat.id, msg) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.AR) {
                                    "اختر محادثة لفتح واجهة SC الفائقة"
                                } else {
                                    "Select a conversation to open the SC Super-Messaging workspace"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Mobile-First Layout
                if (activeChat != null) {
                    ChatDetailPane(
                        chat = activeChat,
                        messagesState = activeMessagesState,
                        summaryState = summaryState,
                        smartDrafts = smartDrafts,
                        isGeneratingDrafts = isGeneratingDrafts,
                        isContactTyping = isContactTyping,
                        translatingMessageIds = translatingMessageIds,
                        language = language,
                        showBackButton = true,
                        onBack = viewModel::closeChat,
                        onSendText = { text -> viewModel.sendTextMessage(activeChat, text) },
                        onSendVoiceNote = { dur, en, ar -> viewModel.sendVoiceNoteMessage(activeChat, dur, en, ar) },
                        onSendHdMedia = { useDesert -> viewModel.sendHdMediaCard(activeChat, useDesert) },
                        onSummarizeUnread = { msgs -> viewModel.summarizeCurrentChat(activeChat, msgs) },
                        onDismissSummary = viewModel::dismissSummary,
                        onGenerateSmartDrafts = { msgs -> viewModel.generateSmartDrafts(activeChat, msgs) },
                        onTranslateMessage = { msg -> viewModel.translateMessageWithAi(activeChat.id, msg) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChatListAndVaultPane(
                        chatsState = chatsState,
                        vaultItemsState = vaultItemsState,
                        selectedSpace = selectedSpace,
                        searchQuery = searchQuery,
                        activeChatId = selectedChatId,
                        language = language,
                        themeMode = themeMode,
                        onSelectSpace = viewModel::selectSpace,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onOpenChat = viewModel::openChat,
                        onToggleChatPin = viewModel::toggleChatPin,
                        onToggleLanguage = viewModel::toggleLanguage,
                        onSelectTheme = viewModel::setThemeMode,
                        onAddNewChat = viewModel::addNewChatThread,
                        onAddVaultItem = viewModel::addVaultItem,
                        onToggleVaultPin = viewModel::toggleVaultPin,
                        onDeleteVaultItem = viewModel::deleteVaultItem,
                        onSignOut = {
                            signOut(credentialManager, scope)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
