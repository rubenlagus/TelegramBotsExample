package org.telegram.methods;

import org.telegram.api.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method when you need to tell the user that something is happening on the bot's side.
 * The status is set for 5 seconds or less (when a message arrives from your bot,
 * Telegram clients clear its typing status).
 * @date 20 of June of 2015
 */
public class SendChatAction {

    public static final String PATH = "sendChatAction";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String ACTION_FIELD = "action";
    /**
     * Type of action to broadcast.
     * Choose one, depending on what the user is about to receive:
     *      'typing' for text messages
     *      'upload_photo' for photos
     *      'record_video' or 'upload_video' for videos
     *      'record_audio' or 'upload_audio' for audio files
     *      'upload_document' for general files,
     *      'find_location' for location data.
     */
    private Float action;
}
