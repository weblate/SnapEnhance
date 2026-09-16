package me.rhunk.snapenhance.core.action.impl

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.database.impl.FriendFeedEntry
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedMiddle
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.core.action.AbstractAction
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.logger.CoreLogger
import me.rhunk.snapenhance.core.messaging.ConversationExporter
import me.rhunk.snapenhance.core.messaging.ExportFormat
import me.rhunk.snapenhance.core.messaging.ExportParams
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.wrapper.impl.Message
import java.io.File
import kotlin.math.absoluteValue

class ExportChatMessages : AbstractAction() {
    private val translation by lazy { context.translation.getCategory("chat_export") }
    private val dialogLogs = mutableListOf<String>()
    private var currentActionDialog: AlertDialog? = null

    private fun logDialog(message: String) {
        context.runOnUiThread {
            if (dialogLogs.size > 10) dialogLogs.removeAt(0)
            dialogLogs.add(message)
            context.log.debug("dialog: $message", "ExportChatMessages")
            currentActionDialog!!.setMessage(dialogLogs.joinToString("\n"))
        }
    }

    private fun setStatus(message: String) {
        context.runOnUiThread {
            currentActionDialog!!.setTitle(message)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
    @Composable
    private fun ExporterDialog(
        getDialog: () -> AlertDialog? = { null }
    ) {
        val exporterTranslation = remember {
            translation.getCategory("exporter_dialog")
        }

        var feedEntries by remember { mutableStateOf(emptyList<FriendFeedEntry>()) }
        var exportType by remember { mutableStateOf(ExportFormat.HTML) }
        val selectedFeedEntries = remember { mutableStateListOf<FriendFeedEntry>() }
        val messageTypeFilter = remember { mutableStateListOf<ContentType>() }
        var amountOfMessages by remember { mutableIntStateOf(-1) }
        var downloadMedias by remember { mutableStateOf(false) }
        val allFriends by rememberAsyncMutableState(null) { context.database.getAllFriends().associateBy { it.userId!! } }
        val myUserId = context.database.myUserId

        var selectConversationMenuExpanded by remember { mutableStateOf(false) }
        var exportFileFormatMenuExpanded by remember { mutableStateOf(false) }
        var messageFilterMenuExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(remember { ScrollState(0) })
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Export chat messages",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectConversationMenuExpanded = !selectConversationMenuExpanded }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exporterTranslation["select_conversations_title"],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = exporterTranslation.format(
                                    "text_field_selection",
                                    "amount" to selectedFeedEntries.size.toString()
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    DropdownMenuPopup(
                        expanded = selectConversationMenuExpanded,
                        onDismissRequest = { selectConversationMenuExpanded = false }
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(0, 1),
                        ) {
                            val totalItems = feedEntries.size

                            feedEntries.fastForEachIndexed { index, feedEntry ->
                                val isChecked = selectedFeedEntries.contains(feedEntry)

                                DropdownMenuItem(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) selectedFeedEntries -= feedEntry
                                        else selectedFeedEntries += feedEntry
                                    },
                                    shapes = MenuDefaults.itemShape(index, totalItems),
                                    text = {
                                        Text(
                                            text = remember(feedEntry) {
                                                (if (feedEntry.conversationType == 1) feedEntry.feedDisplayName
                                                else feedEntry.participants?.filter { it != myUserId }
                                                    ?.firstOrNull()?.let { userId ->
                                                    allFriends?.get(userId)?.let { friend ->
                                                        friend.displayName?.let { "$it (${friend.mutableUsername})" }
                                                            ?: friend.mutableUsername
                                                    }
                                                }) ?: "Unknown"
                                            },
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                Box {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportFileFormatMenuExpanded = !exportFileFormatMenuExpanded }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exporterTranslation["export_file_format_title"],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = exportType.extension,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    DropdownMenuPopup(
                        expanded = exportFileFormatMenuExpanded,
                        onDismissRequest = { exportFileFormatMenuExpanded = false }
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(0, 1)
                        ) {
                            val formats = ExportFormat.entries
                            val totalItems = formats.size

                            formats.fastForEachIndexed { index, exportFormat ->
                                val isSelected = exportFormat == exportType

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = exportFormat.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        exportType = exportFormat
                                        exportFileFormatMenuExpanded = false
                                    },
                                    selected = isSelected,
                                    shapes = MenuDefaults.itemShape(index, totalItems)
                                )
                            }
                        }
                    }
                }

                Box {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { messageFilterMenuExpanded = !messageFilterMenuExpanded }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exporterTranslation["message_type_filter_title"],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = messageTypeFilter.takeIf { it.isNotEmpty() }?.let {
                                    exporterTranslation.format(
                                        "text_field_selection",
                                        "amount" to it.size.toString()
                                    )
                                } ?: exporterTranslation["text_field_selection_all"],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    DropdownMenuPopup(
                        expanded = messageFilterMenuExpanded,
                        onDismissRequest = { messageFilterMenuExpanded = false }
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(0, 1)
                        ) {
                            val contentTypes = remember {
                                arrayOf(
                                    ContentType.CHAT,
                                    ContentType.SNAP,
                                    ContentType.EXTERNAL_MEDIA,
                                    ContentType.NOTE,
                                    ContentType.STICKER
                                )
                            }
                            val totalItems = contentTypes.size

                            contentTypes.forEachIndexed { index, contentType ->
                                val isChecked = messageTypeFilter.contains(contentType)

                                DropdownMenuItem(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) messageTypeFilter -= contentType
                                        else messageTypeFilter += contentType
                                    },
                                    shapes = MenuDefaults.itemShape(index, totalItems),
                                    text = { Text(text = contentType.name) }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeGroupedBottom,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { downloadMedias = !downloadMedias }
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exporterTranslation["download_medias_title"],
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(checked = downloadMedias, onCheckedChange = { downloadMedias = it })
                    }
                }
            }

            val focusManager = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            val isKeyboardVisible = WindowInsets.isImeVisible

            LaunchedEffect(isKeyboardVisible) {
                if (!isKeyboardVisible) {
                    focusManager.clearFocus()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        })
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(exporterTranslation["amount_of_messages_title"])

                OutlinedTextField(
                    value = amountOfMessages.takeIf { it != -1 }?.toString() ?: "",
                    onValueChange = { amountOfMessages = it.toIntOrNull()?.absoluteValue ?: -1 },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    )
                )
            }

            Row(
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { getDialog()?.dismiss() },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(text = translation["dialog_negative_button"])
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    enabled = selectedFeedEntries.isNotEmpty(),
                    onClick = {
                        exportChatForConversations(selectedFeedEntries, ExportParams(
                            exportFormat = exportType,
                            messageTypeFilter = messageTypeFilter.takeIf { it.isNotEmpty() },
                            amountOfMessages = amountOfMessages.takeIf { it != -1 },
                            downloadMedias = downloadMedias
                        ))
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = translation["dialog_positive_button"])
                }
            }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    feedEntries = context.database.getFeedEntries(500)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun run() {
        context.coroutineScope.launch(Dispatchers.Main) {
            createComposeAlertDialog(context.mainActivity!!) { alertDialog ->
                ExporterDialog { alertDialog }
            }.apply {
                setCanceledOnTouchOutside(false)
                show()
            }
        }
    }

    private suspend fun fetchMessagesPaginated(conversationId: String, lastMessageId: Long, amount: Int): List<Message> = runBlocking {
        for (i in 0..5) {
            val messages: List<Message>? = suspendCancellableCoroutine { continuation ->
                context.feature(Messaging::class).conversationManager?.fetchConversationWithMessagesPaginated(conversationId,
                    lastMessageId,
                    amount, onSuccess = { messages ->
                        continuation.resumeWith(Result.success(messages))
                    }, onError = {
                        continuation.resumeWith(Result.success(null))
                    }) ?: continuation.resumeWith(Result.success(null))
            }
            if (messages != null) return@runBlocking messages
            logDialog("Retrying in 1 second...")
            delay(1000)
        }
        logDialog("Failed to fetch messages")
        emptyList()
    }

    private fun exportChatForConversations(
        conversations: List<FriendFeedEntry>,
        exportParams: ExportParams,
    ) {
        dialogLogs.clear()
        val jobs = mutableListOf<Job>()

        currentActionDialog = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            .setTitle(translation["exporting_chats"])
            .setCancelable(false)
            .setMessage("")
            .create()
        
        val conversationSize = translation.format("processing_chats", "amount" to conversations.size.toString())
        
        logDialog(conversationSize)

        context.coroutineScope.launch {
            conversations.forEach { conversation ->
                launch {
                    runCatching {
                        exportFullConversation(conversation, exportParams)
                    }.onFailure {
                        logDialog(translation.format("export_fail", "conversation" to conversation.key.toString()))
                        logDialog(it.stackTraceToString())
                        CoreLogger.xposedLog(it)
                    }
                }.also { jobs.add(it) }
            }
            jobs.joinAll()
            logDialog(translation["finished"])
        }.also {
            currentActionDialog?.setButton(DialogInterface.BUTTON_POSITIVE, translation["dialog_negative_button"]) { dialog, _ ->
                it.cancel()
                jobs.forEach { it.cancel() }
                dialog.dismiss()
            }
        }

        currentActionDialog!!.also {
            it.setCanceledOnTouchOutside(false)
        }.show()
    }

    private suspend fun exportFullConversation(
        feedEntry: FriendFeedEntry,
        exportParams: ExportParams,
    ) {
        //first fetch the first message
        val conversationId = feedEntry.key!!
        val conversationParticipants = context.database.getConversationParticipants(feedEntry.key!!, useCache = false)
            ?.mapNotNull {
                context.database.getFriendInfo(it)
            }?.associateBy { it.userId!! } ?: emptyMap()

        val conversationName = feedEntry.feedDisplayName ?: conversationParticipants.values.take(3).joinToString("_") { it.mutableUsername ?: "" }

        val publicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SE Extended").also { if (!it.exists()) it.mkdirs() }
        val outputFile = publicFolder.resolve("conversation_${conversationName}_${System.currentTimeMillis()}.${exportParams.exportFormat.extension}")

        logDialog(translation.format("exporting_message", "conversation" to conversationName))

        val conversationExporter = ConversationExporter(
            context = context,
            friendFeedEntry = feedEntry,
            conversationParticipants = conversationParticipants,
            exportParams = exportParams,
            cacheFolder = publicFolder.resolve("cache").also { if (!it.exists()) it.mkdirs() },
            outputFile = outputFile,
        ).apply { init(); printLog = {
            logDialog(it.toString())
        } }

        var foundMessageCount = 0

        var lastMessageId = fetchMessagesPaginated(conversationId, Long.MAX_VALUE, amount = 1).firstOrNull()?.also {
            conversationExporter.readMessage(it)
            foundMessageCount++
        }?.messageDescriptor?.messageId ?: run {
            logDialog(translation["no_messages_found"])
            return
        }

        while (true) {
            val fetchedMessages = fetchMessagesPaginated(conversationId, lastMessageId, amount = 500).toMutableList()
            if (fetchedMessages.isEmpty()) break

            fetchedMessages.firstOrNull()?.let {
                lastMessageId = it.messageDescriptor!!.messageId!!
            }

            exportParams.messageTypeFilter?.let { filter ->
                fetchedMessages.removeIf { message ->
                    !filter.contains(message.messageContent?.contentType ?: return@removeIf false)
                }
            }

            foundMessageCount += fetchedMessages.size

            if (exportParams.amountOfMessages != null && foundMessageCount >= exportParams.amountOfMessages) {
                fetchedMessages.reversed().subList(0, exportParams.amountOfMessages - (foundMessageCount - fetchedMessages.size)).forEach { message ->
                    conversationExporter.readMessage(message)
                }
                break
            }

            fetchedMessages.reversed().forEach { message ->
                conversationExporter.readMessage(message)
            }

            setStatus("Exporting (found ${foundMessageCount})")
        }

        if (exportParams.exportFormat == ExportFormat.HTML) conversationExporter.awaitDownload()
        conversationExporter.close()
        logDialog(translation["writing_output"])
        dialogLogs.clear()
        logDialog("\n" + translation.format("exported_to",
            "path" to outputFile.absolutePath.toString()
        ) + "\n")
    }
}