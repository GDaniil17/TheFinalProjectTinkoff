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

class MyAmazingBot : TelegramLongPollingBot() {
    var records = mutableMapOf<Long, MutableList<Pair<Int, String>>>()
    var dates = mutableMapOf<Int, Int>()
    var names = mutableMapOf<String, User>()
    var chatId = mutableMapOf<Int, Long>()
    var versions = mutableMapOf<Int, MutableList<String>>()

    private fun getAllVersions(messageText: String, userId: String) {
        try {
            for (i in records[names[userId]?.id]!!) {
                if (i.second.contains(messageText)) {
                    val message = SendMessage() // Create a SendMessage object with mandatory fields
                    message.text =
                        versions[i.first]?.joinToString(prefix = "[", postfix = "]", separator = "; \n").toString()
                    execute(SendMessage(chatId[i.first].toString(), "Versions: \n${message.text}"))
                    break
                }
            }
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

    private fun getAllMessagesByUserId(userId: String, chatIdentifier: String) {
        if (userId in names.keys) {
            for (i in records[names[userId]?.id]!!) {
                val message = SendMessage()
                message.text = records[i]?.size.toString()
                if (chatId[i.first].toString() == chatIdentifier) {
                    val time = Date.from(Instant.ofEpochSecond(dates[i.first]!!.toLong()))
                    execute(SendMessage(chatId[i.first].toString(), "${i.second} | at $time | by ${userId}"))
                }
            }
        } else {
            execute(SendMessage(chatIdentifier, "${userId} has not typed anything yet"))
        }
    }

    private fun getAllMessagesInCertainPeriod(startDate: String, endDate: String, userId: String, chatIdAdr: Long) {
        val start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val unixStart = start.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val unixEnd = end.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        for (i in records[names[userId]?.id]!!) {
            if ((unixStart <= dates[i.first]!!) && (dates[i.first]!! <= unixEnd) && chatIdAdr == chatId[i.first]) {
                val message = SendMessage() // Create a SendMessage object with mandatory fields
                message.text = records[i]?.size.toString()
                val time = Date.from(Instant.ofEpochSecond(dates[i.first]!!.toLong()))
                execute(SendMessage(chatId[i.first].toString(), "${i.second} | at $time | by ${userId}"))
            }
        }
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasEditedMessage()) {
            versions[update.editedMessage.messageId]?.add(update.editedMessage.text)
            records.getOrPut(update.editedMessage.from.id) { mutableListOf() }
                .add(Pair(update.editedMessage.messageId, update.editedMessage.text))
        }
        if (update.message.isReply && update.message != null) {
            if (update.message.text.contains("/all_messages")) {
                getAllMessagesByUserId(update.message.replyToMessage.from.firstName, update.message.chatId.toString())
            } else if (update.message.text.contains("/get_all_messages_in_certain_period")) {
                var messageText = update.message.text
                try {
                    for (i in records.keys) {
                        val message = SendMessage()
                        message.chatId = update.message.chatId.toString()
                        var k = 0
                        for (j in records[i]!!) {
                            if (chatId[j.first].toString() == message.chatId) {
                                k += 1
                            }
                        }
                        message.text = k.toString()
                        execute(
                            SendMessage(
                                message.chatId,
                                "${update.message.replyToMessage.from.firstName} sent ${message.text} messages"
                            )
                        )
                    }
                    messageText = messageText.replace("/get_all_messages_in_certain_period ", "")
                    val tmp = messageText.split(" ").toTypedArray()
                    val startDate = tmp[0]
                    val endDate = tmp[1]
                    var name = update.message.replyToMessage.from.firstName
                    if (tmp.size >= 3) {
                        name = tmp[2]
                    }
                    getAllMessagesInCertainPeriod(startDate, endDate, name, update.message.replyToMessage.chatId)
                } catch (e: Exception) {
                }
            } else
                if (update.message.text.contains("/get_all_versions")) {
                    getAllVersions(update.message.replyToMessage.text, update.message.replyToMessage.from.firstName)
                }
        }
        if (update.hasMessage() && update.message.hasSticker()) {
            execute(SendMessage(update.message.chatId.toString(), "\uD83D\uDE00"))
        }
        if (!update.message.isReply && update.hasMessage() && update.message.hasText()) {
            val message = SendMessage()
            message.chatId = update.message.chatId.toString()
            message.text = update.message.text

            if (!message.text.isEmpty()) {
                var messageText = message.text
                when {
                    messageText.startsWith("/start") -> {
                        sendNotification(
                            update.message.chatId,
                            "Добро пожаловать! ${update.message.from.firstName}"
                        )
                    }
                    messageText.startsWith("/help") -> {
                        execute(
                            SendMessage(
                                message.chatId,
                                "/start - чтобы запустить бота \n/get_all_messages_in_certain_period dd.mm.yyyy dd.mm.yyyy UserFirstName \n/get_all_versions When I was walking the rain started; UserFirstName\n/all_messages Name - чтобы получить все сообщения пользователя"
                            )
                        )
                    }
                    messageText.startsWith("/all_messages") -> {
                        try {
                            if (!messageText.isEmpty()) {
                                if (messageText.contains("/all_messages ")) {
                                    getAllMessagesByUserId(
                                        messageText.replace(
                                            "/all_messages ",
                                            ""
                                        ),
                                        update.message.chatId.toString()
                                    )
                                } else {
                                    getAllMessagesByUserId(
                                        update.message.from.firstName,
                                        update.message.chatId.toString()
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            execute(SendMessage(message.chatId, "Sorry, but the script is incorrect"))
                        }
                    }

                    messageText.startsWith("/get_all_messages_in_certain_period") -> {
                        try {

                            for (i in records.keys) {
                                val message = SendMessage()
                                message.chatId = update.message.chatId.toString()
                                var k = 0
                                for (j in records[i]!!) {
                                    if (chatId[j.first].toString() == message.chatId) {
                                        k += 1
                                    }
                                }
                                message.text = k.toString()
                                execute(
                                    SendMessage(
                                        message.chatId,
                                        "${update.message.from.firstName} sent ${message.text} messages"
                                    )
                                )
                            }
                            messageText = messageText.replace("/get_all_messages_in_certain_period ", "")
                            val tmp = messageText.split(" ").toTypedArray()
                            val startDate = tmp[0]
                            val endDate = tmp[1]
                            var name = update.message.from.firstName
                            if (tmp.size >= 3) {
                                name = tmp[2]
                            }
                            getAllMessagesInCertainPeriod(startDate, endDate, name, message.chatId.toLong())
                        } catch (e: Exception) {
                            execute(SendMessage(message.chatId, "Sorry, but the script is incorrect"))
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
                                getAllVersions(text, name)
                            } else {
                                if (update.message.replyToMessage.text.count {
                                        update.message.replyToMessage.text.contains(
                                            " |"
                                        )
                                    } >= 2) {
                                    val text = update.message.replyToMessage.text.split(" |")[0]
                                    val name =
                                        update.message.replyToMessage.text.split(" |")[2].replace(" by ", "")
                                    getAllVersions(text, name)
                                } else {
                                    val text = update.message.replyToMessage.text
                                    val name = update.message.replyToMessage.from.firstName
                                    getAllVersions(text, name)
                                }

                            }
                        } catch (e: Exception) {
                            execute(SendMessage(message.chatId, "Sorry, but the script is incorrect"))
                        }
                    }
                    else -> {
                        val userId = update.message.from
                        names.getOrPut(userId.firstName) { userId }
                        var date = 0
                        if (update.message.date != null) {
                            date = update.message.date
                        }
                        records.getOrPut(userId.id) { mutableListOf() }
                            .add(Pair(update.message.messageId, update.message.text))
                        dates.getOrPut(update.message.messageId) { date }
                        chatId.getOrPut(update.message.messageId) { update.message.chatId }
                        if (update.message.messageId in versions.keys) {
                            versions[update.message.messageId]?.add(update.message.text)
                        } else {
                            versions[update.message.messageId] = mutableListOf(update.message.text)
                        }
                    }
                }
            } else {
                "Я понимаю только текст"
            }
        }
    }

    override fun getBotUsername(): String {
        return "FinalProjectTinkoff"
    }

    override fun getBotToken(): String {
        return "1753707611:AAGBfrE57M7u-p8dV4k-GFV3hx1TkfN9-Pc"
    }
}

