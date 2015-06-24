package org.telegram.api;

import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Reply keyboard abstract type
 * @date 20 of June of 2015
 */
public interface ReplyKeyboard {
    JSONObject toJson();
}
