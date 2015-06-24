package org.telegram.updateshandlers;

import org.telegram.api.Update;

import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Callback to handle updates. Must support both, single update and List of updates
 * @date 20 of June of 2015
 */
public interface UpdatesCallback {
    void onUpdateReceived(Update update);
    void onUpdatesReceived(List<Update> updates);
}
