package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.logging.BotLogger;

/**
 * Created by pithera on 5/31/16.
 * Yes this is an ugly example, feel free to supply something nice.
 */
public class webHookExampleHandlers extends TelegramWebhookBot {
    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        BotLogger.severe("UPDATE", update.toString());
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(update.getMessage().getChatId().toString());
            sendMessage.setText("Your webhook works!, this is your callback:\n" + BuildVars.EXTERNALWEBHOOKURL  + "/"
            + "callback/" + getBotPath());
            return sendMessage;
        }
        return null;
    }


    @Override
    public String getBotUsername() {
        return BotConfig.USERNAMEWEBHOOK;
    }

    @Override
    public String getBotToken() {
        return BotConfig.TOKENWEBHOOK;
    }

    @Override
    public String getBotPath() {
        return BotConfig.USERNAMEWEBHOOK;
    } //arbitrary path to deliver updates on, username is an example.


}

