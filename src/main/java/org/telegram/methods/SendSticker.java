package org.telegram.methods;

import org.telegram.api.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief TODO
 * @date 20 of June of 2015
 */
public class SendSticker {
    public static final String PATH = "sendsticker";

    public static final String CHATID_FIELD = "document";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String STICKER_FIELD = "sticker";
    private String sticker; ///< Sticker file to send. file_id as String to resend a sticker that is already on the Telegram servers
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private Integer replayToMessageId; ///< Optional. If the message is a reply, ID of the original message
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    private ReplyKeyboard replayMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard
}
