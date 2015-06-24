package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Upon receiving a message with this object, Telegram clients will display a reply interface to the user
 * (act as if the user has selected the bot‘s message and tapped ’Reply').
 * This can be extremely useful if you want to create user-friendly step-by-step
 * interfaces without having to sacrifice privacy mode.
 * @date 22 of June of 2015
 */
public class ForceReply implements ReplyKeyboard {

    public static final String FORCEREPLY_FIELD = "force_reply";
    /**
     * Shows reply interface to the user, as if they manually selected the bot‘s message and tapped ’Reply'
     */
    @JsonProperty(FORCEREPLY_FIELD)
    private Boolean forceReply;
    public static final String SELECTIVE_FIELD = "selective";
    /**
     * Use this parameter if you want to force reply from specific users only.
     * Targets:
     *      1) users that are @mentioned in the text of the Message object;
     *      2) if the bot's message is a reply (has reply_to_message_id), sender of the original message.
     */
    @JsonProperty(SELECTIVE_FIELD)
    private Boolean selective;

    public ForceReply() {
        super();
        this.forceReply = true;
    }

    public ForceReply(JSONObject jsonObject) {
        super();
        this.forceReply = jsonObject.optBoolean(FORCEREPLY_FIELD, true);
        this.selective = jsonObject.optBoolean(SELECTIVE_FIELD, false);
    }

    public Boolean getForceReply() {
        return forceReply;
    }

    public void setForceReply(Boolean forceReply) {
        this.forceReply = forceReply;
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

        jsonObject.put(FORCEREPLY_FIELD, this.forceReply);
        if (this.selective != null) {
            jsonObject.put(SELECTIVE_FIELD, this.selective);
        }

        return jsonObject;
    }
}
