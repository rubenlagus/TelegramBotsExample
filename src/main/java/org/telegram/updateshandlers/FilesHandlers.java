package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Files Bot
 * This bot is an example for the use of sendMessage asynchronously
 * @date 24 of June of 2015
 */
public class FilesHandlers extends TelegramLongPollingBot {
    private static final String LOGTAG = "FILESHANDLERS";

    private static final int INITIAL_UPLOAD_STATUS = 0;
    private static final int DELETE_UPLOADED_STATUS = 1;
    private final ConcurrentLinkedQueue<Integer> languageMessages = new ConcurrentLinkedQueue<>();

    @Override
    public String getBotToken() {
        return BotConfig.FILES_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                try {
                    handleFileUpdate(update);
                } catch (TelegramApiRequestException e) {
                    if (e.getApiResponse().contains("Bot was blocked by the user")) {
                        if (update.getMessage().getFrom() != null) {
                            DatabaseManager.getInstance().deleteUserForFile(update.getMessage().getFrom().getId());
                        }
                    }
                } catch (Exception e) {
                    BotLogger.severe(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.FILES_USER;
    }

    private void handleFileUpdate(Update update) throws InvalidObjectException, TelegramApiException {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (languageMessages.contains(message.getFrom().getId())) {
                onLanguageReceived(message);
            } else {
                String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
                if (message.getText().startsWith(Commands.setLanguageCommand)) {
                    onSetLanguageCommand(message, language);
                } else {
                    String[] parts = message.getText().split(" ", 2);
                    if (parts[0].startsWith(Commands.startCommand)) {
                        if (parts.length == 2) {
                            onStartWithParameters(message, language, parts[1]);
                        } else {
                            sendHelpMessage(message, language);
                        }
                    } else if (!message.isGroupMessage()) {
                        if (parts[0].startsWith(Commands.uploadCommand)) { // Open upload for user
                            onUploadCommand(message, language);
                        } else if (parts[0].startsWith(Commands.cancelCommand)) {
                            onCancelCommand(message, language);
                        } else if (parts[0].startsWith(Commands.deleteCommand)) {
                            onDeleteCommand(message, language, parts);
                        } else if (parts[0].startsWith(Commands.listCommand)) {
                            onListCommand(message, language);
                        } else {
                            sendHelpMessage(message, language);
                        }
                    }
                }
            }
        } else if (message != null && message.hasDocument()
                && DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == INITIAL_UPLOAD_STATUS) {
            String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
            DatabaseManager.getInstance().addFile(message.getDocument().getFileId(), message.getFrom().getId(), message.getDocument().getFileName());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setText(LocalisationService.getString("fileUploaded", language) +
                    LocalisationService.getString("uploadedFileURL", language) + message.getDocument().getFileId());
            sendMessageRequest.setChatId(message.getChatId());
            execute(sendMessageRequest);
        }
    }

    private void onListCommand(Message message, String language) throws InvalidObjectException, TelegramApiException {
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage();
        if (files.size() > 0) {
            String text = LocalisationService.getString("listOfFiles", language) + ":\n\n";
            for (Map.Entry<String, String> entry : files.entrySet()) {
                text += LocalisationService.getString("uploadedFileURL", language)
                        + entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW.toString() + " " + entry.getValue() + "\n";
            }
            sendMessageRequest.setText(text);
        } else {
            sendMessageRequest.setText(LocalisationService.getString("noFiles", language));
        }
        sendMessageRequest.setChatId(message.getChatId());
        sendMessageRequest.setReplyMarkup(new ReplyKeyboardRemove());
        execute(sendMessageRequest);
    }

    private void onDeleteCommand(Message message, String language, String[] parts) throws InvalidObjectException, TelegramApiException {
        if (DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == DELETE_UPLOADED_STATUS &&
                parts.length == 2) {
            onDeleteCommandWithParameters(message, language, parts[1]);
        } else {
            onDeleteCommandWithoutParameters(message, language);
        }
    }

    private void onDeleteCommandWithoutParameters(Message message, String language) throws InvalidObjectException, TelegramApiException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), DELETE_UPLOADED_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getString("deleteUploadedFile", language));
        sendMessageRequest.setChatId(message.getChatId());
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        if (files.size() > 0) {
            List<KeyboardRow> commands = new ArrayList<>();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add(Commands.deleteCommand + " " + entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW.toString()
                        + " " + entry.getValue());
                commands.add(commandRow);
            }
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setKeyboard(commands);
        }
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
        execute(sendMessageRequest);
    }

    private void onDeleteCommandWithParameters(Message message, String language, String part) throws InvalidObjectException, TelegramApiException {
        String[] innerParts = part.split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        boolean removed = DatabaseManager.getInstance().deleteFile(innerParts[0].trim());
        SendMessage sendMessageRequest = new SendMessage();
        if (removed) {
            sendMessageRequest.setText(LocalisationService.getString("fileDeleted", language));
        } else {
            sendMessageRequest.setText(LocalisationService.getString("wrongFileId", language));
        }
        sendMessageRequest.setChatId(message.getChatId());

        execute(sendMessageRequest);
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());

    }

    private void onCancelCommand(Message message, String language) throws InvalidObjectException, TelegramApiException {
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getString("processFinished", language));
        sendMessageRequest.setChatId(message.getChatId());
        execute(sendMessageRequest);
    }

    private void onUploadCommand(Message message, String language) throws InvalidObjectException, TelegramApiException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), INITIAL_UPLOAD_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getString("sendFileToUpload", language));
        sendMessageRequest.setChatId(message.getChatId());
        execute(sendMessageRequest);
    }

    private void sendHelpMessage(Message message, String language) throws InvalidObjectException, TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        String formatedString = String.format(
                LocalisationService.getString("helpFiles", language),
                Commands.startCommand, Commands.uploadCommand, Commands.deleteCommand,
                Commands.listCommand);
        sendMessageRequest.setText(formatedString);
        sendMessageRequest.setChatId(message.getChatId());
        execute(sendMessageRequest);
    }

    private void onStartWithParameters(Message message, String language, String part) throws InvalidObjectException, TelegramApiException {
        if (DatabaseManager.getInstance().doesFileExists(part.trim())) {
            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setDocument(part.trim());
            sendDocumentRequest.setChatId(message.getChatId());
            execute(sendDocumentRequest);
        } else {
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setText(LocalisationService.getString("wrongFileId", language));
            sendMessageRequest.setChatId(message.getChatId());
            execute(sendMessageRequest);
        }
    }

    private void onSetLanguageCommand(Message message, String language) throws InvalidObjectException, TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<LocalisationService.Language> languages = LocalisationService.getSupportedLanguages();
        List<KeyboardRow> commands = new ArrayList<>();
        for (LocalisationService.Language languageItem : languages) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(languageItem.getCode() + " " + Emoji.LEFT_RIGHT_ARROW.toString() + " " + languageItem.getName());
            commands.add(commandRow);
        }
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(commands);
        replyKeyboardMarkup.setSelective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
        sendMessageRequest.setText(LocalisationService.getString("chooselanguage", language));
        execute(sendMessageRequest);
        languageMessages.add(message.getFrom().getId());
    }

    private void onLanguageReceived(Message message) throws InvalidObjectException, TelegramApiException {
        String[] parts = message.getText().split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId());
        if (LocalisationService.getLanguageByCode(parts[0].trim()) != null) {
            DatabaseManager.getInstance().putUserLanguage(message.getFrom().getId(), parts[0].trim());
            sendMessageRequest.setText(LocalisationService.getString("languageModified", parts[0].trim()));
        } else {
            sendMessageRequest.setText(LocalisationService.getString("errorLanguage"));
        }
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessageRequest);
        languageMessages.remove(message.getFrom().getId());
    }
}
