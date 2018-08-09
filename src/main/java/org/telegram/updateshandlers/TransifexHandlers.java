package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.LocalisationService;
import org.telegram.services.TransifexService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

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
            handleUpdate(update);
        } catch (Throwable e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void handleUpdate(Update update) throws InvalidObjectException, TelegramApiException {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            if (BuildVars.ADMINS.contains(message.getFrom().getId())) {
                sendTransifexFile(message);
            } else {
                sendMovedToMessage(message);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.TRANSIFEX_USER;
    }

    private void sendTransifexFile(Message message) throws InvalidObjectException {
        String language = DatabaseManager.getInstance().getUserLanguage(message.getFrom().getId());
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
                        LocalisationService.getString("helpTransifex", language),
                        Commands.transifexiOSCommand, Commands.transifexAndroidCommand, Commands.transifexWebogram,
                        Commands.transifexTDesktop, Commands.transifexOSX, Commands.transifexWP,
                        Commands.transifexAndroidSupportCommand);
                sendMessageRequest.setText(helpFormated);
                sendMessageRequest.setChatId(message.getChatId());
                try {
                    execute(sendMessageRequest);
                } catch (TelegramApiException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }

            if (sendDocument != null) {
                sendDocument.setChatId(message.getChatId());
                try {
                    execute(sendDocument);
                } catch (TelegramApiException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } else if (parts[0].startsWith(Commands.help) ||
                (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) {
            SendMessage sendMessageRequest = new SendMessage();
            String helpFormated = String.format(
                    LocalisationService.getString("helpTransifex", language),
                    Commands.transifexiOSCommand, Commands.transifexAndroidCommand, Commands.transifexWebogram,
                    Commands.transifexTDesktop, Commands.transifexOSX, Commands.transifexWP,
                    Commands.transifexAndroidSupportCommand);
            sendMessageRequest.setText(helpFormated);
            sendMessageRequest.setChatId(message.getChatId());
            try {
                execute(sendMessageRequest);
            } catch (TelegramApiException e) {
                BotLogger.error(LOGTAG, e);
            }
        }
    }

    private void sendMovedToMessage(Message message) throws InvalidObjectException, TelegramApiException {
        String language = DatabaseManager.getInstance().getUserLanguage(message.getFrom().getId());
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.setReplyToMessageId(message.getMessageId());
        answer.setText(LocalisationService.getString("movedToLangBot", language));
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(LocalisationService.getString("checkLangBot", language));
        button.setUrl("https://telegram.me/langbot");
        row.add(button);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);
        answer.setReplyMarkup(inlineKeyboardMarkup);
        execute(answer);
    }
}
