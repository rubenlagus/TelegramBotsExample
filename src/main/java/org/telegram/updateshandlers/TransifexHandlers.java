package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.LocalisationService;
import org.telegram.services.TransifexService;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.InvalidObjectException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Transifex Bot
 * @date 24 of June of 2015
 */
public class TransifexHandlers extends TelegramLongPollingBot {
    private static final String LOGTAG = "TRANSIFEXHANDLERS";

    @Override
    public String getBotToken() {
        return BotConfig.TRANSIFEX_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            sendTransifexFile(update);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.TRANSIFEX_USER;
    }

    private void sendTransifexFile(Update update) throws InvalidObjectException {
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
                    sendMessageRequest.setChatId(message.getChatId().toString());
                    try {
                        sendMessage(sendMessageRequest);
                    } catch (TelegramApiException e) {
                        BotLogger.error(LOGTAG, e);
                    }
                }

                if (sendDocument != null) {
                    sendDocument.setChatId(message.getChatId().toString());
                    try {
                        sendDocument(sendDocument);
                    } catch (TelegramApiException e) {
                        BotLogger.error(LOGTAG, e);
                    }
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
                sendMessageRequest.setChatId(message.getChatId().toString());
                try {
                    sendMessage(sendMessageRequest);
                } catch (TelegramApiException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        }
    }
}
