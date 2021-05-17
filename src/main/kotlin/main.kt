import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.telegram.telegrambots.meta.TelegramBotsApi


fun main() {
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(MyAmazingBot())
    } catch (e: Exception) {
        println(e)
    }
}
