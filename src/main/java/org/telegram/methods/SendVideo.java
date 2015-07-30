package org.telegram.methods;

import org.telegram.api.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to send video files,
 * Telegram clients support mp4 videos (other formats may be sent as Document).
 * On success, the sent Message is returned.
 * @date 20 of June of 2015
 */
public class SendVideo {
    public static final String PATH = "sendvideo";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String VIDEO_FIELD = "video";
    private String video; ///< Video to send. file_id as String to resend a video that is already on the Telegram servers
    public static final String DURATION_FIELD = "duration";
    private String duration; ///< Optional. Duration of sent video in seconds
    public static final String CAPTION_FIELD = "caption";
    private String caption; ///< OptionaL. Video caption (may also be used when resending videos by file_id).
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private Integer replayToMessageId; ///< Optional. If the message is a reply, ID of the original message
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    private ReplyKeyboard replayMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard
}
