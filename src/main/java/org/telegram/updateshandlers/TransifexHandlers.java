package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.Message;
import org.telegram.api.Update;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.SendDocument;
import org.telegram.methods.SendMessage;
import org.telegram.services.LocalisationService;
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
    private static final String webhookPath = "transifexBot";
    private final Webhook webhook;
    private final UpdatesThread updatesThread;

    public TransifexHandlers() {
        if (BuildVars.useWebHook) {
            webhook = new Webhook(this, webhookPath);
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

    public void sendTransifexFile(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
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
                    String helpFormated = String.format(
                            LocalisationService.getInstance().getString("helpTransifex", language),
                            Commands.transifexiOSCommand, Commands.transifexAndroidCommand, Commands.transifexWebogram,
                            Commands.transifexTDesktop, Commands.transifexOSX, Commands.transifexWP,
                            Commands.transifexAndroidSupportCommand);
                    sendMessageRequest.setText(helpFormated);
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
                String helpFormated = String.format(
                        LocalisationService.getInstance().getString("helpTransifex", language),
                        Commands.transifexiOSCommand, Commands.transifexAndroidCommand, Commands.transifexWebogram,
                        Commands.transifexTDesktop, Commands.transifexOSX, Commands.transifexWP,
                        Commands.transifexAndroidSupportCommand);
                sendMessageRequest.setText(helpFormated);
                sendMessageRequest.setChatId(message.getChatId());
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
            }
        }
    }
}
