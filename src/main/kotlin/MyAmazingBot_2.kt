import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class MyAmazingBot_2 : TelegramLongPollingBot() {
    private var dates = mutableMapOf<Int, Int>()
    private var names = mutableMapOf<String, Long>()
    private val messages = mutableMapOf<MessageKey, MessageValue>()

    override fun onUpdateReceived(update: Update) {
        if (update.hasEditedMessage()) {
            if (MessageKey(update.editedMessage.from.id, update.editedMessage.chatId) in messages) {
                if (MessageKey(
                        update.editedMessage.from.id,
                        update.editedMessage.chatId
                    ) !in messages
                ) {
                    messages[MessageKey(
                        update.editedMessage.from.id,
                        update.editedMessage.chatId
                    )] = MessageValue(mutableMapOf())
                }
                messages[MessageKey(
                    update.editedMessage.from.id,
                    update.editedMessage.chatId
                )]?.messages?.get(update.editedMessage.messageId)?.add(update.editedMessage.text)

                val userId = update.editedMessage.from.id
                names.getOrPut(update.editedMessage.from.firstName) { userId }
                var date = 0
                if (update.editedMessage.date != null) {
                    date = update.editedMessage.editDate
                }
                dates[update.editedMessage.messageId] = date
            }
        }
        if (update.message != null && update.message.isReply) {
            if (update.message.text.contains("/all_messages")) {
                getAllMessagesByUserId(
                    update.message.replyToMessage.from.id,
                    update.message.chatId,
                    update.message.replyToMessage.from.firstName
                )
            } else if (update.message.text.contains("/get_all_messages_in_certain_period")) {
                var messageText = update.message.text
                try {
                    messageText = messageText.replace("/get_all_messages_in_certain_period ", "")
                    val tmp = messageText.split(" ").toTypedArray()
                    val startDate = tmp[0]
                    val endDate = tmp[1]
                    var name = update.message.replyToMessage.from.id
                    if (tmp.size >= 3) {
                        name = names[tmp[2]]!!
                    }
                    getAllMessagesInCertainPeriod(
                        startDate, endDate,
                        name, update.message.replyToMessage.chatId
                    )

                } catch (e: Exception) {
                }
            } else
                if (update.message.text.contains("/get_all_versions")) {
                    getAllVersions(
                        update.message.replyToMessage.text,
                        update.message.replyToMessage.from.id,
                        update.message.replyToMessage.chatId
                    )
                }
        }
        if (update.hasMessage() && update.message.hasSticker()) {
            sendMessage(update.message.chatId, "\uD83D\uDE00")
        }
        if (update.message != null && !update.message.isReply && update.hasMessage() && update.message.hasText()) {
            val message = SendMessage()
            val chatId = update.message.chatId
            message.text = update.message.text

            if (message.text.isNotEmpty()) {
                var messageText = message.text
                when {
                    messageText.startsWith("/start") -> {
                        sendNotification(chatId, "Добро пожаловать! ${update.message.from.firstName}")

                    }
                    messageText.startsWith("/help") -> {
                        sendMessage(
                            chatId,
                            "/start - чтобы запустить бота \n/get_all_messages_in_certain_period dd.mm.yyyy dd.mm.yyyy UserFirstName \n/get_all_versions When I was walking the rain started; UserFirstName\n/all_messages Name - чтобы получить все сообщения пользователя"
                        )

                    }
                    messageText.startsWith("/all_messages") -> {
                        try {
                            if (messageText.isNotEmpty()) {
                                if (messageText.contains("/all_messages ")) {
                                    val name = messageText.replace(
                                        "/all_messages ",
                                        ""
                                    )
                                    names[name]?.let {
                                        getAllMessagesByUserId(
                                            it,
                                            chatId,
                                            name
                                        )
                                    }
                                } else {
                                    getAllMessagesByUserId(
                                        update.message.from.id,
                                        chatId,
                                        update.message.from.firstName
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            sendMessage(chatId, "Sorry, but the script is incorrect")
                        }
                    }

                    messageText.startsWith("/get_all_messages_in_certain_period") -> {
                        try {
                            messageText = messageText.replace("/get_all_messages_in_certain_period ", "")
                            val tmp = messageText.split(" ").toTypedArray()
                            val startDate = tmp[0]
                            val endDate = tmp[1]
                            var name = update.message.from.id

                            if (tmp.size >= 3) {
                                name = names[tmp[2]]!!
                            }

                            getAllMessagesInCertainPeriod(
                                startDate, endDate,
                                name, message.chatId.toLong()
                            )

                        } catch (e: Exception) {
                            sendMessage(chatId, "Sorry, but the script is incorrect")
                        }
                    }
                    messageText.startsWith("/get_all_versions") -> {
                        try {
                            if (update.message.text != null) {
                                messageText = messageText.replace("/get_all_versions ", "").replace("; ", ";")
                                val tmp = messageText.split(";").toTypedArray()
                                val text = tmp[0]
                                var name = update.message.from.firstName
                                if (tmp.size >= 2) {
                                    name = tmp[1]
                                }
                                names[name]?.let { getAllVersions(text, it, update.message.chatId) }
                            } else {
                                if (update.message.replyToMessage.text.count {
                                        update.message.replyToMessage.text.contains(
                                            " |"
                                        )
                                    } >= 2) {
                                    val text = update.message.replyToMessage.text.split(" |")[0]
                                    val name =
                                        update.message.replyToMessage.text.split(" |")[2].replace(" by ", "")
                                    names[name]?.let { getAllVersions(text, it, update.message.replyToMessage.chatId) }
                                } else {
                                    val text = update.message.replyToMessage.text
                                    val name = update.message.replyToMessage.from.firstName
                                    names[name]?.let { getAllVersions(text, it, update.message.replyToMessage.chatId) }
                                }

                            }
                        } catch (e: Exception) {
                            sendMessage(chatId, "Sorry, but the script is incorrect")
                        }
                    }
                    else -> {
                        if (MessageKey(
                                update.message.from.id,
                                update.message.chatId
                            ) !in messages
                        ) {
                            messages[MessageKey(
                                update.message.from.id,
                                update.message.chatId
                            )] = MessageValue(mutableMapOf())
                        }
                        messages[MessageKey(
                            update.message.from.id,
                            update.message.chatId
                        )]?.messages?.put(update.message.messageId, mutableListOf(update.message.text))

                        val userId = update.message.from.id
                        names.getOrPut(update.message.from.firstName) { userId }
                        var date = 0
                        if (update.message.date != null) {
                            date = update.message.date
                        }
                        dates[update.message.messageId] = date
                    }
                }
            } else {

            }
        }
    }


    private fun sendMessage(chatId: Long, text: String) {
        execute(SendMessage(chatId.toString(), text))
    }

    private fun getAllVersions(messageText: String, userId: Long, chatId: Long) {
        try {
            for (i in
            messages[MessageKey(
                userId,
                chatId
            )]?.messages?.values!!) {
                if (i.contains(messageText)) {
                    sendMessage(
                        chatId,
                        "Versions: \n${
                            i.joinToString(prefix = "[", postfix = "]", separator = "; \n")
                        }"
                    )
                    return
                }
            }
            sendMessage(chatId, "Sorry, but the script is incorrect")
        } catch (e: Exception) {
        }
    }

    private fun sendNotification(chatId: Long, responseText: String) {

        val responseMessage = SendMessage(chatId.toString(), responseText)
        responseMessage.parseMode = "Markdown"
        responseMessage.replyMarkup = getReplyMarkup(
            listOf(
                listOf("/start", "/help"),
                listOf("/get_all_versions", "/all_messages")
            )
        )
        execute(responseMessage)
    }

    private fun getReplyMarkup(allButtons: List<List<String>>): ReplyKeyboardMarkup {
        val markup = ReplyKeyboardMarkup()
        markup.keyboard = allButtons.map { rowButtons ->
            val row = KeyboardRow()
            rowButtons.forEach { rowButton -> row.add(rowButton) }
            row
        }
        return markup
    }

    private fun getAllMessagesByUserId(userId: Long, chatIdentifier: Long, name: String) {
        if (MessageKey(userId, chatIdentifier) in messages.keys) {
            for (i in messages[MessageKey(
                userId,
                chatIdentifier
            )]?.messages?.keys!!) {
                val time = Date.from(Instant.ofEpochSecond(dates[i]!!.toLong()))
                sendMessage(
                    chatIdentifier, "${
                        messages[MessageKey(
                            userId,
                            chatIdentifier
                        )]?.messages?.get(i)
                    } | at $time | by $name"
                )

            }
        } else {
            sendMessage(chatIdentifier, "$name has not typed anything yet")
        }
    }

    private fun getAllMessagesInCertainPeriod(startDate: String, endDate: String, userId: Long, chatIdAdr: Long) {

        val start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val unixStart = start.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd.MM.yyyy")).plusDays(1)

        val unixEnd = end.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond

        for (i in messages[MessageKey(
            userId,
            chatIdAdr
        )]?.messages?.keys!!) {
            if ((unixStart <= dates[i]!!) && (dates[i]!! <= unixEnd)) {
                val time = Date.from(dates[i]?.let { Instant.ofEpochSecond(it.toLong()) })
                sendMessage(
                    chatIdAdr, "${
                        messages[MessageKey(
                            userId,
                            chatIdAdr
                        )]?.messages?.get(i)
                    } | at $time | by $userId"
                )
            }

        }

    }

    override fun getBotUsername(): String {
        return "FinalProjectTinkoff"
    }

    override fun getBotToken(): String {
        return "token"
    }
}

