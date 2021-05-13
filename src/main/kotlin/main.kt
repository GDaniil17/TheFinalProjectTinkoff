import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import java.net.URI
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.sql.SQLException

import java.net.URISyntaxException
import java.sql.Connection


fun main(args: Array<String>) {
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(MyAmazingBot())
        KeyboardButton.builder()
            .build()
        MyAmazingBot().getConnection()
    } catch (e: Exception) {
        println(e)
    }
}

class MyAmazingBot : TelegramLongPollingBot() {
    private var i: Int = 0
    var records = mutableMapOf<Long, MutableList<Pair<Int, String>>>()
    var dates = mutableMapOf<Int, Int>()
    var names = mutableMapOf<String, User>()
    var chatId = mutableMapOf<Int, Long>()
    var versions = mutableMapOf<Int, MutableList<String>>()

    fun getConnection(): Connection? {
        try {
            return DriverManager.getConnection(
                "jdbc:postgresql://rfroahjigykcla:d8cda2b0e73fb49cfc674b79c46f904cba2b097b129e9c568bf5c4a41315ade8@ec2-3-233-43-103.compute-1.amazonaws.com:5432/d92f5ndp5s8m99",
                "rfroahjigykcla",
                "d8cda2b0e73fb49cfc674b79c46f904cba2b097b129e9c568bf5c4a41315ade8"
            )
        }catch (e: Exception){
            println(e)
        }
        return null
    }

    fun getAllVersions(messageText: String, userId: String) {
        println("1) $messageText")
        println("2) $userId")
        for (i in records[names[userId]?.id]!!) {
            println(i.second)
            if (i.second.contains(messageText)) {
                val message = SendMessage() // Create a SendMessage object with mandatory fields
                message.text = versions[i.first].toString()
                execute(SendMessage(chatId[i.first].toString(), "Versions: ${message.text}"))
                break
            }
        }
    }

    fun getAllMessagesByUserId(userId: String) {
        for (i in records[names[userId]?.id]!!) {
            //dates[chatId[i.first]]
            val message = SendMessage() // Create a SendMessage object with mandatory fields
            message.text = records[i]?.size.toString()
            execute(SendMessage(chatId[i.first].toString(), "All: ${i.second}"))
        }
    }

    fun getAllMessagesInCertainPeriod(startDate: String, endDate: String, userId: String, chatIdAdr: Long) {
        val start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val unixStart = start.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val unixEnd = end.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        for (i in records[names[userId]?.id]!!) {
            if ((unixStart <= dates[i.first]!!) && (dates[i.first]!! <= unixEnd) && chatIdAdr == chatId[i.first]) {
                val message = SendMessage() // Create a SendMessage object with mandatory fields
                message.text = records[i]?.size.toString()
                var time =
                    Date.from(Instant.ofEpochSecond(dates[i.first]!!.toLong()))//java.time.format.DateTimeFormatter.ISO_INSTANT
                //.format(dates[i.first]?.let { java.time.Instant.ofEpochSecond(it.toLong()) })
                execute(SendMessage(chatId[i.first].toString(), "${i.second} | at $time | by ${userId}"))
            }
        }
    }

    override fun onUpdateReceived(update: Update) {
        // We check if the update has a message and the message has text
        if (update.hasEditedMessage()) {
            //println(versions[update.editedMessage.messageId])
            versions[update.editedMessage.messageId]?.add(update.editedMessage.text)
            records.getOrPut(update.editedMessage.from.id) { mutableListOf() }
                .add(Pair(update.editedMessage.messageId, update.editedMessage.text))
            //println(versions[update.editedMessage.messageId])
        }
        if (update.hasMessage()) { //&& update.message.hasText()) {
            val message = SendMessage()
            message.chatId = update.message.chatId.toString()
            message.text = update.message.text

            val responseText = if (!message.text.isNullOrEmpty()) {
                var messageText = message.text
                when {
                    messageText.startsWith("/start") -> {
                        execute(
                            SendMessage(
                                message.chatId,
                                """
                                    Добро пожаловать!
                                """
                            )
                        )
                    }
                    messageText.startsWith("/help") -> {
                        execute(
                            SendMessage(
                                message.chatId,
                                "/start - чтобы запустить бота \n/get_all_messages_in_certain_period dd.mm.yyyy dd.mm.yyyy UserFirstName \n/get_all_versions When I was walking the rain started; UserFirstName"
                            )
                        )
                    }
                    messageText.startsWith("/all messages") -> getAllMessagesByUserId(
                        messageText.replace(
                            "/all messages ",
                            ""
                        )
                    )
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
                            //println(startDate + endDate + name);
                            getAllMessagesInCertainPeriod(startDate, endDate, name, message.chatId.toLong())
                        } catch (e: Exception) {
                            execute(SendMessage(message.chatId, "Sorry, but the script is incorrect"))
                        }
                    }
                    messageText.startsWith("/get_all_versions") -> {
                        try {
                            if (update.message.replyToMessage.text == null) {
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
                                    val name = update.message.replyToMessage.text.split(" |")[2].replace(" by ", "")
                                    println(text + name)
                                    println(names)
                                    getAllVersions(text, name)
                                } else {
                                    val text = update.message.replyToMessage.text
                                    val name = update.message.replyToMessage.from.firstName
                                    println(text + name)
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
                        /*
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
                                        "${userId.firstName} sent ${message.text} messages"
                                    )
                                )
                                k = 0
                                for (j in records[i]!!) {
                                    if (chatId[j.first].toString() == message.chatId) {
                                        k += 1
                                        message.chatId = update.message.chatId.toString()
                                        val time = Date.from(Instant.ofEpochSecond(dates[j.first]!!.toLong()))
                                        //execute(SendMessage(message.chatId, "$k) ${j.second} at ${time}"))
                                    }
                                }
                            }
                        } catch (e: TelegramApiException) {
                            e.printStackTrace()
                        }

                         */
                        //val sendMessageRequest = SendMessage()
                        //sendMessageRequest.chatId =
                        //    message.chatId //who should get the message? the sender from which we got the message...

                        //sendMessageRequest.text = "you said: " + message.text
                        //try {
                        //    execute(sendMessageRequest)
                        //} catch (e: TelegramApiException) {

                        // }
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
        return "1753707611:AAH2-wHcBeRK5TxQqUjQ6N9OQCjC22AaPZU"
    }
}

