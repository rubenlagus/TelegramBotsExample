package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a Telegram chat with an user or a group
 * @date 24 of June of 2015
 */
public class Chat {

    public static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private Integer id; ///< Unique identifier for this chat
    public static final String FIRSTNAME_FIELD = "first_name";
    @JsonProperty(FIRSTNAME_FIELD)
    private String firstName; ///< User‘s or bot’s first name
    public static final String LASTNAME_FIELD = "last_name";
    @JsonProperty(LASTNAME_FIELD)
    private String lastName; ///< Optional. User‘s or bot’s last name
    public static final String USERNAME_FIELD = "username";
    @JsonProperty(USERNAME_FIELD)
    private String userName; ///< Optional. User‘s or bot’s username
    public static final String TITLE_FIELD = "title";
    @JsonProperty(TITLE_FIELD)
    private String title; ///< Group name

    public Chat() {
        super();
    }

    public Chat(JSONObject jsonObject) {
        super();
        this.id = jsonObject.getInt(ID_FIELD);
        if (this.id > 0) {
            this.firstName = jsonObject.getString(FIRSTNAME_FIELD);
            this.lastName = jsonObject.optString(LASTNAME_FIELD, "");
            this.userName = jsonObject.optString(USERNAME_FIELD, "");
        } else {
            this.title = jsonObject.getString(TITLE_FIELD);
        }
    }

    public Integer getId() {
        return id;
    }

    public Boolean isGroupChat() {
        if (id < 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }
}
