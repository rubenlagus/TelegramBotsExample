package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Upon receiving a message with this object,
 * Telegram clients will hide the current custom keyboard and display the default letter-keyboard.
 * By default, custom keyboards are displayed until a new keyboard is sent by a bot.
 * An exception is made for one-time keyboards that are hidden immediately after the user presses a button
 * (@see ReplyKeyboardMarkup).
 * @date 20 of June of 2015
 */
public class ReplyKeyboardHide implements ReplyKeyboard{

    public static final String HIDEKEYBOARD_FIELD = "hide_keyboard";
    @JsonProperty(HIDEKEYBOARD_FIELD)
    private Boolean hideKeyboard; ///< Requests clients to hide the custom keyboard
    public static final String SELECTIVE_FIELD = "selective";
    /**
     * Optional. Use this parameter if you want to show the keyboard to specific users only.
     * Targets:
     *      1) users that are @mentioned in the text of the Message object;
     *      2) if the bot's message is a reply (has reply_to_message_id), sender of the original message.
     */
    @JsonProperty(SELECTIVE_FIELD)
    private Boolean selective;

    public ReplyKeyboardHide() {
        super();
        this.selective = true;
    }

    public ReplyKeyboardHide(JSONObject jsonObject) {
        super();
        this.hideKeyboard = jsonObject.optBoolean(HIDEKEYBOARD_FIELD, true);
        this.selective = jsonObject.optBoolean(SELECTIVE_FIELD, true);
    }

    public Boolean getHideKeyboard() {
        return hideKeyboard;
    }

    public void setHideKeyboard(Boolean hideKeyboard) {
        this.hideKeyboard = hideKeyboard;
    }

    public Boolean getSelective() {
        return selective;
    }

    public void setSelective(Boolean selective) {
        this.selective = selective;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(HIDEKEYBOARD_FIELD, this.hideKeyboard);
        jsonObject.put(SELECTIVE_FIELD, this.selective);
        return jsonObject;
    }

}
