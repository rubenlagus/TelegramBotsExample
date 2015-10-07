package org.telegram.api.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.json.JSONObject;
import org.telegram.api.interfaces.BotApiObject;

import java.io.IOException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief TODO
 * @date 20 of June of 2015
 */
public class Update implements BotApiObject {
    public static final String UPDATEID_FIELD = "update_id";
    /**
     * The update‘s unique identifier.
     * Update identifiers start from a certain positive number and increase sequentially.
     * This ID becomes especially handy if you’re using Webhooks,
     * since it allows you to ignore repeated updates or to restore the
     * correct update sequence, should they get out of order.
     */
    @JsonProperty(UPDATEID_FIELD)
    private Integer updateId;
    public static final String MESSAGE_FIELD = "message";
    @JsonProperty(MESSAGE_FIELD)
    private Message message; ///< Optional. New incoming message of any kind — text, photo, sticker, etc.

    public Update() {
        super();
    }

    public Update(JSONObject jsonObject) {
        super();
        this.updateId = jsonObject.getInt(UPDATEID_FIELD);
        if (jsonObject.has(MESSAGE_FIELD)) {
            this.message = new Message(jsonObject.getJSONObject(MESSAGE_FIELD));
        }
    }

    public Integer getUpdateId() {
        return updateId;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField(UPDATEID_FIELD, updateId);
        if (message != null) {
            gen.writeObjectField(MESSAGE_FIELD, message);
        }
        gen.writeEndObject();
        gen.flush();
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField(UPDATEID_FIELD, updateId);
        if (message != null) {
            gen.writeObjectField(MESSAGE_FIELD, message);
        }
        gen.writeEndObject();
        gen.flush();
    }
}
