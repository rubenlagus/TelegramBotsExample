package org.telegram;

import org.telegram.services.BotLogger;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.updateshandlers.*;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Main class to create all bots
 * @date 20 of June of 2015
 */
public class Main {
    private static final String LOGTAG = "MAIN";

    public static void main(String[] args) {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new ChannelHandlers());
            telegramBotsApi.registerBot(new DirectionsHandlers());
            telegramBotsApi.registerBot(new RaeHandlers());
            telegramBotsApi.registerBot(new WeatherHandlers());
            telegramBotsApi.registerBot(new TransifexHandlers());
            telegramBotsApi.registerBot(new FilesHandlers());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }
}
