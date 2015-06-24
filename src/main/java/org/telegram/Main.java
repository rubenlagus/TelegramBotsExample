package org.telegram;

import org.telegram.updateshandlers.*;
import org.telegram.updatesreceivers.UpdatesThread;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Main class to create all bots
 * @date 20 of June of 2015
 */
public class Main {
    public static void main(String[] args) {
        UpdatesCallback weatherBot = new WeatherHandlers();
        UpdatesCallback transifexBot = new TransifexHandlers();
        UpdatesCallback filesBot = new FilesHandlers();
        UpdatesCallback directionsBot = new DirectionsHandlers();
    }
}
