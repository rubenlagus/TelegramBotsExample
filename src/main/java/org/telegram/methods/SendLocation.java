package org.telegram.methods;

import org.telegram.api.ReplyKeyboard;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to send point on the map. On success, the sent Message is returned.
 * @date 20 of June of 2015
 */
public class SendLocation {
    public static final String PATH = "sendlocation";

    public static final String CHATID_FIELD = "chat_id";
    private Integer chatId; ///< Unique identifier for the message recepient — User or GroupChat id
    public static final String LATITUDE_FIELD = "latitude";
    private Float latitude; ///< Latitude of location
    public static final String LONGITUDE_FIELD = "longitude";
    private Float longitude; ///< Longitude of location
    public static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private Integer replayToMessageId; ///< Optional. If the message is a reply, ID of the original message
    public static final String REPLYMARKUP_FIELD = "reply_markup";
    private ReplyKeyboard replayMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard
}
