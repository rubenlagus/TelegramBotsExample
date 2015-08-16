package org.telegram.methods;

import org.telegram.api.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to send audio files,
 * Use this method to send audio files, if you want Telegram clients to display them in the music player.
 * Your audio must be in an .mp3 format. On success, the sent Message is returned.
 * Bots can currently send audio files of up to 50 MB in size, this limit may be changed in the future.
 *
 * @note For backward compatibility, when both fields title and description are empty and mime-type of the sent
 * file is not “audio/mpeg”, file is sent as playable voice message.
 * In this case, your audio must be in an .ogg file encoded with OPUS.
 * This will be removed in the future. You need to use sendVoice method instead.
 *
 * @date 16 of July of 2015
 */
public class SendAudio {
    public static final String PATH = "sendaudio";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String AUDIO_FIELD = "audio";
    private String audio; ///< Audio file to send. file_id as String to resend an audio that is already on the Telegram servers
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private Integer replayToMessageId; ///< Optional. If the message is a reply, ID of the original message
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    private ReplyKeyboard replayMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard
    public static final String PERFOMER_FIELD = "performer";
    private String performer; ///< Optional. Performer of sent audio
    public static final String TITLE_FIELD = "title";
    private String title; ///< Optional. Title of sent audio

    public SendAudio() {
        super();
    }
}
