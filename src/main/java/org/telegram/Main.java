package org.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.WebhookOptions;
import org.telegram.updateshandlers.ChannelHandlers;
import org.telegram.updateshandlers.CommandsHandler;
import org.telegram.updateshandlers.DirectionsHandlers;
import org.telegram.updateshandlers.ElektrollArtFanHandler;
import org.telegram.updateshandlers.FilesHandlers;
import org.telegram.updateshandlers.RaeHandlers;
import org.telegram.updateshandlers.WeatherHandlers;
import org.telegram.updateshandlers.WebHookExampleHandlers;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Main class to create all bots
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        try (TelegramBotsWebhookApplication webhookApplication = new TelegramBotsWebhookApplication(WebhookOptions.builder().enableRequestLogging(true).build())) {
            webhookApplication.registerBot(new WebHookExampleHandlers(BotConfig.WEBHOOK_TOKEN));
            try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
                botsApplication.registerBot(BotConfig.WEATHER_TOKEN, new WeatherHandlers(BotConfig.WEATHER_TOKEN));
                botsApplication.registerBot(BotConfig.CHANNEL_TOKEN, new ChannelHandlers(BotConfig.CHANNEL_TOKEN));
                botsApplication.registerBot(BotConfig.COMMANDS_TOKEN, new CommandsHandler(BotConfig.COMMANDS_TOKEN, BotConfig.COMMANDS_USER));
                botsApplication.registerBot(BotConfig.DIRECTIONS_TOKEN, new DirectionsHandlers(BotConfig.DIRECTIONS_TOKEN));
                botsApplication.registerBot(BotConfig.ELEKTROLLART_TOKEN, new ElektrollArtFanHandler(BotConfig.ELEKTROLLART_TOKEN));
                botsApplication.registerBot(BotConfig.FILES_TOKEN, new FilesHandlers(BotConfig.FILES_TOKEN));
                botsApplication.registerBot(BotConfig.RAE_TOKEN, new RaeHandlers(BotConfig.RAE_TOKEN));
                Thread.currentThread().join();
            } catch (Exception e) {
                log.error("Error registering bot", e);
            }
        } catch (TelegramApiException e) {
            log.error("Error registering bot", e);
        }
    }
}
