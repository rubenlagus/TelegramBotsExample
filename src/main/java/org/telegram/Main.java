package org.telegram;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.logging.BotLogger;
import org.telegram.telegrambots.logging.BotsFileHandler;
import org.telegram.updateshandlers.ChannelHandlers;
import org.telegram.updateshandlers.DirectionsHandlers;
import org.telegram.updateshandlers.FilesHandlers;
import org.telegram.updateshandlers.RaeHandlers;
import org.telegram.updateshandlers.TransifexHandlers;
import org.telegram.updateshandlers.WeatherHandlers;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Main class to create all bots
 * @date 20 of June of 2015
 */
public class Main {
    private static final String LOGTAG = "MAIN";

    public static void main(String[] args) {
        BotLogger.setLevel(Level.ALL);
        BotLogger.registerLogger(new ConsoleHandler());
        try {
            BotLogger.registerLogger(new BotsFileHandler());
        } catch (IOException e) {
            BotLogger.severe("MAIN", e);
        }

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
