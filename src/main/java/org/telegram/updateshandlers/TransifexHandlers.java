package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.Message;
import org.telegram.api.Update;
import org.telegram.methods.SendDocument;
import org.telegram.methods.SendMessage;
import org.telegram.services.TransifexService;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Transifex Bot
 * @date 24 of June of 2015
 */
public class TransifexHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENTRANSIFEX;
    private static final int webhookPort = 9991;
    private final Webhook webhook;
    private final UpdatesThread updatesThread;

    public TransifexHandlers() {
        if (BuildVars.useWebHook) {
            webhook = new Webhook(this, webhookPort);
            updatesThread = null;
            SenderHelper.SendWebhook(webhook.getURL(), TOKEN);
        } else {
            webhook = null;
            SenderHelper.SendWebhook("", TOKEN);
            updatesThread = new UpdatesThread(TOKEN, this);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        sendTransifexFile(update);
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        for (Update update: updates) {
            sendTransifexFile(update);
        }
    }

    public void sendTransifexFile(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            String[] parts = text.split(" ", 2);
            SendDocument sendDocument = null;
            if (parts.length == 2) {
                if (parts[0].startsWith(Commands.transifexiOSCommand)) {
                    sendDocument = TransifexService.getInstance().getiOSLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexAndroidCommand)) {
                    sendDocument = TransifexService.getInstance().getAndroidLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexTDesktop)) {
                    sendDocument = TransifexService.getInstance().getTdesktopLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexWebogram)) {
                    sendDocument = TransifexService.getInstance().getWebogramLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexWP)) {
                    sendDocument = TransifexService.getInstance().getWPLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexOSX)) {
                    sendDocument = TransifexService.getInstance().getOSXLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.transifexAndroidSupportCommand)) {
                    sendDocument = TransifexService.getInstance().getAndroidSupportLanguageFile(parts[1].trim());
                } else if (parts[0].startsWith(Commands.help)) {
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(CustomMessages.helpTransifex);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                }

                if (sendDocument != null) {
                    sendDocument.setChatId(message.getChatId());
                    SenderHelper.SendDocument(sendDocument, TOKEN);
                }
            } else if (parts[0].startsWith(Commands.help) ||
                    (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setText(CustomMessages.helpTransifex);
                sendMessageRequest.setChatId(message.getChatId());
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
            }
        }
    }
}
