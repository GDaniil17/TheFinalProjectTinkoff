import org.telegram.telegrambots.meta.api.objects.Message

data class MessageValue(
    val messages: MutableMap<Int, MutableList<String>>
)