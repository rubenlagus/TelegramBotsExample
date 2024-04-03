package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;

/**
 * @author pithera
 * @version 1.0
 * Simple Webhook example
 */
@Slf4j
public class WebHookExampleHandlers implements TelegramWebhookBot {
    private final TelegramClient telegramClient;

    public WebHookExampleHandlers(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public BotApiMethod<?> consumeUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Well, all information looks like noise until you break the code.")
                    .build();
            return sendMessage;
        }
        return null;
    }

    @Override
    public void runDeleteWebhook() {
        try {
            telegramClient.execute(new DeleteWebhook());
        } catch (TelegramApiException e) {
            log.info("Error deleting webhook");
        }
    }

    @Override
    public void runSetWebhook() {
        try {
            telegramClient.execute(SetWebhook
                    .builder()
                    .url(BuildVars.EXTERNALWEBHOOKURL + getBotPath())
                    .build());
        } catch (TelegramApiException e) {
            log.info("Error setting webhook");
        }
    }

    @Override
    public String getBotPath() {
        return "/" + BotConfig.WEBHOOK_USER; //arbitrary path to deliver updates on, username is an example.
    }
}
