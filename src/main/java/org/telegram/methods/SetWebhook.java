package org.telegram.methods;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to specify a url and receive incoming updates via an outgoing webhook.
 * Whenever there is an update for the bot, we will send an HTTPS POST request to the specified url,
 * containing a JSON-serialized Update. In case of an unsuccessful request,
 * we will give up after a reasonable amount of attempts.
 * @date 20 of June of 2015
 */
public class SetWebhook {
    public static final String PATH = "setwebhook";

    public static final String URL_FIELD = "url";
    private String url; ///< HTTPS url to send updates to. Use an empty string to remove webhook integration
}
