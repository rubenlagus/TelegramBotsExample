package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a Telegram user or bot.
 * @date 20 of June of 2015
 */
public class User {

    public static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private Integer id; ///< Unique identifier for this user or bot
    public static final String FIRSTNAME_FIELD = "first_name";
    @JsonProperty(FIRSTNAME_FIELD)
    private String firstName; ///< User‘s or bot’s first name
    public static final String LASTNAME_FIELD = "last_name";
    @JsonProperty(LASTNAME_FIELD)
    private String lastName; ///< Optional. User‘s or bot’s last name
    public static final String USERNAME_FIELD = "username";
    @JsonProperty(USERNAME_FIELD)
    private String userName; ///< Optional. User‘s or bot’s username

    public User() {
        super();
    }

    public User(JSONObject jsonObject) {
        super();
        this.id = jsonObject.getInt(ID_FIELD);
        this.firstName = jsonObject.getString(FIRSTNAME_FIELD);
        this.lastName = jsonObject.optString(LASTNAME_FIELD, "");
        this.userName = jsonObject.optString(USERNAME_FIELD, "");
    }

    public Integer getId() {
        return id;
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
