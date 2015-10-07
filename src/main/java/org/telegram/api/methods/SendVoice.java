package org.telegram.api.methods;

import org.telegram.api.objects.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to send voice notes, if you want Telegram clients to display
 * the file as a playable voice message.
 * For this to work, your audio must be in an .ogg file encoded with OPUS
 * (other formats may be sent as Audio or Document).
 * @date 16 of July of 2015
 */
public class SendVoice {
    public static final String PATH = "sendvoice";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String VOICE_FIELD = "voice";
    private String voice; ///< Audio file to send. file_id as String to resend an audio that is already on the Telegram servers
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private Integer replayToMessageId; ///< Optional. If the message is a reply, ID of the original message
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    private ReplyKeyboard replayMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard
    public static final String DURATION_FIELD = "duration";
    private Integer duration; ///< Optional. Duration of sent audio in seconds

    public SendVoice() {
        super();
    }
}
