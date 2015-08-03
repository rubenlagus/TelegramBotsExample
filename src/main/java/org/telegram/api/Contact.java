package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a phone contact.
 * @date 20 of June of 2015
 */
public class Contact {

    public static final String PHONENUMBER_FIELD = "phone_number";
    @JsonProperty(PHONENUMBER_FIELD)
    private String phoneNumber; ///< Contact's phone number
    public static final String FIRSTNAME_FIELD = "first_name";
    @JsonProperty(FIRSTNAME_FIELD)
    private String firstName; ///< Contact's first name
    public static final String LASTNAME_FIELD = "last_name";
    @JsonProperty(LASTNAME_FIELD)
    private String lastName; ///< Optional. Contact's last name
    public static final String USERID_FIELD = "user_id";
    @JsonProperty(USERID_FIELD)
    private Integer userID; ///< Optional. Contact's user identifier in Telegram

    public Contact() {
        super();
    }

    public Contact(JSONObject jsonObject) {
        super();
        this.phoneNumber = jsonObject.getString(PHONENUMBER_FIELD);
        this.firstName = jsonObject.getString(FIRSTNAME_FIELD);
        this.lastName = jsonObject.optString(LASTNAME_FIELD, "");
        this.userID = jsonObject.optInt(USERID_FIELD, 0);
    }
}
