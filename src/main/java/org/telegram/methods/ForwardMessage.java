package org.telegram.methods;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to send text messages. On success, the sent Message is returned.
 * @date 20 of June of 2015
 */
public class ForwardMessage {
    public static final String PATH = "forwardmessage";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String FROMCHATID_FIELD = "from_chat_id";
    private Integer fromChatId; ///< Unique identifier for the chat where the original message was sent — User or GroupChat id
    public static final String MESSAGEID_FIELD = "message_id";
    private Integer messageId; ///< Unique message identifier

    public ForwardMessage() {
        super();
    }

    public Integer getChatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
        this.chatId = chatId;
    }

    public Integer getFromChatId() {
        return fromChatId;
    }

    public void setFromChatId(Integer fromChatId) {
        this.fromChatId = fromChatId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }
}
