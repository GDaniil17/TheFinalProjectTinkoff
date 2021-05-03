import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(MyAmazingBot())
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}

class MyAmazingBot : TelegramLongPollingBot() {
    private var i: Int = 0
    var records = mutableMapOf<Long, MutableList<Pair<Int, String>>>()
    var dates = mutableMapOf<Int, Int>()
    var names = mutableMapOf<String, User>()
    var chatId = mutableMapOf<Int, Long>()

    fun getAllMessagesByUserId(userId: String) {
        for (i in records[names[userId]?.id]!!) {

            dates[chatId[i.first]]
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
                execute(SendMessage(chatId[i.first].toString(), "${i.second} at ${time}"))
            }
        }
    }

    override fun onUpdateReceived(update: Update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage()) { //&& update.message.hasText()) {
            val message = SendMessage()
            message.chatId = update.message.chatId.toString()
            message.text = update.message.text

            val responseText = if (!message.text.isNullOrEmpty()) {
                var messageText = message.text
                when {
                    messageText == "/start" -> {
                        execute(SendMessage(message.chatId,
                        """
                            Добро пожаловать!
                            
                            /start - чтобы запустить бота
                                
                            /get_all_messages_in_certain_period dd.mm.yyyy dd.mm.yyyy UserFirstName
                            
                            /get_all_versions_of_certain_message
                            """))
                    }
                    messageText.contains("/all messages") -> getAllMessagesByUserId(
                        messageText.replace(
                            "/all messages ",
                            ""
                        )
                    )
                    messageText.contains("/get_all_messages_in_certain_period") -> {
                        try {
                            messageText = messageText.replace("/get_all_messages_in_certain_period ", "")
                            val tmp = messageText.split(" ").toTypedArray()
                            val startDate = tmp[0]
                            val endDate = tmp[1]
                            val name = tmp[2]
                            getAllMessagesInCertainPeriod(startDate, endDate, name, message.chatId.toLong())
                        }catch (e: Exception) {
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

                        try {
                            for (i in records.keys) {
                                val message = SendMessage()
                                message.chatId = update.message.chatId.toString()
                                message.text = records[i]?.size.toString()
                                execute(
                                    SendMessage(
                                        message.chatId,
                                        "${userId.firstName} sent ${message.text} messages"
                                    )
                                )
                                var k = 0
                                for (j in records[i]!!) {
                                    k += 1
                                    message.chatId = update.message.chatId.toString()
                                    val time = Date.from(Instant.ofEpochSecond(dates[j.first]!!.toLong()))
                                    execute(SendMessage(message.chatId, "$k) ${j.second} at ${time}"))
                                }
                            }
                        } catch (e: TelegramApiException) {
                            e.printStackTrace()
                        }
                        val sendMessageRequest = SendMessage()
                        sendMessageRequest.chatId =
                            message.chatId //who should get the message? the sender from which we got the message...

                        sendMessageRequest.text = "you said: " + message.text
                        try {
                            execute(sendMessageRequest)
                        } catch (e: TelegramApiException) {

                        }
                    }
                }
            } else {
                "Я понимаю только текст"
            }
        }
    }

    override fun getBotUsername(): String {
        return "Kursach"
    }

    override fun getBotToken(): String {
        return "1753707611:AAH2-wHcBeRK5TxQqUjQ6N9OQCjC22AaPZU"
    }
}

