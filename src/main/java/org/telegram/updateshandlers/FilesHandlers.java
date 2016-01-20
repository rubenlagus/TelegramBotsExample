package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.BotLogger;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.SendDocument;
import org.telegram.telegrambots.api.methods.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.ReplyKeyboardHide;
import org.telegram.telegrambots.api.objects.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

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
        return BotConfig.TOKENFILES;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            handleFileUpdate(update);
        } catch (InvalidObjectException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.USERNAMEFILES;
    }

    private void handleFileUpdate(Update update) throws InvalidObjectException {
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
            sendMessageRequest.setText(LocalisationService.getInstance().getString("fileUploaded", language) +
                    LocalisationService.getInstance().getString("uploadedFileURL", language) + message.getDocument().getFileId());
            sendMessageRequest.setChatId(message.getChatId().toString());
            try {
                sendMessage(sendMessageRequest);
            } catch (TelegramApiException e) {
                BotLogger.error(LOGTAG, e);
            }
        }
    }

    private void onListCommand(Message message, String language) throws InvalidObjectException {
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage();
        if (files.size() > 0) {
            String text = LocalisationService.getInstance().getString("listOfFiles", language) + ":\n\n";
            for (Map.Entry<String, String> entry : files.entrySet()) {
                text += LocalisationService.getInstance().getString("uploadedFileURL", language)
                        + entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW.toString() + " " + entry.getValue() + "\n";
            }
            sendMessageRequest.setText(text);
        } else {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("noFiles", language));
        }
        sendMessageRequest.setChatId(message.getChatId().toString());
        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setHideKeyboard(true);
        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onDeleteCommand(Message message, String language, String[] parts) throws InvalidObjectException {
        if (DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == DELETE_UPLOADED_STATUS &&
                parts.length == 2) {
            onDeleteCommandWithParameters(message, language, parts[1]);
        } else {
            onDeleteCommandWithoutParameters(message, language);
        }
    }

    private void onDeleteCommandWithoutParameters(Message message, String language) throws InvalidObjectException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), DELETE_UPLOADED_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("deleteUploadedFile", language));
        sendMessageRequest.setChatId(message.getChatId().toString());
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        if (files.size() > 0) {
            List<List<String>> commands = new ArrayList<>();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                List<String> commandRow = new ArrayList<>();
                commandRow.add(Commands.deleteCommand + " " + entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW.toString()
                        + " " + entry.getValue());
                commands.add(commandRow);
            }
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboad(true);
            replyKeyboardMarkup.setKeyboard(commands);
        }
        sendMessageRequest.setReplayMarkup(replyKeyboardMarkup);
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onDeleteCommandWithParameters(Message message, String language, String part) throws InvalidObjectException {
        String[] innerParts = part.split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        boolean removed = DatabaseManager.getInstance().deleteFile(innerParts[0].trim());
        SendMessage sendMessageRequest = new SendMessage();
        if (removed) {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("fileDeleted", language));
        } else {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("wrongFileId", language));
        }
        sendMessageRequest.setChatId(message.getChatId().toString());
        try {
            sendMessage(sendMessageRequest);
            DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }

    }

    private void onCancelCommand(Message message, String language) throws InvalidObjectException {
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("processFinished", language));
        sendMessageRequest.setChatId(message.getChatId().toString());
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onUploadCommand(Message message, String language) throws InvalidObjectException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), INITIAL_UPLOAD_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("sendFileToUpload", language));
        sendMessageRequest.setChatId(message.getChatId().toString());
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void sendHelpMessage(Message message, String language) throws InvalidObjectException {
        SendMessage sendMessageRequest = new SendMessage();
        String formatedString = String.format(
                LocalisationService.getInstance().getString("helpFiles", language),
                Commands.startCommand, Commands.uploadCommand, Commands.deleteCommand,
                Commands.listCommand);
        sendMessageRequest.setText(formatedString);
        sendMessageRequest.setChatId(message.getChatId().toString());
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onStartWithParameters(Message message, String language, String part) throws InvalidObjectException {
        try {
            if (DatabaseManager.getInstance().doesFileExists(part.trim())) {
                SendDocument sendDocumentRequest = new SendDocument();
                sendDocumentRequest.setDocument(part.trim());
                sendDocumentRequest.setChatId(message.getChatId().toString());
                sendDocument(sendDocumentRequest);
            } else {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setText(LocalisationService.getInstance().getString("wrongFileId", language));
                sendMessageRequest.setChatId(message.getChatId().toString());
                sendMessage(sendMessageRequest);
            }
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onSetLanguageCommand(Message message, String language) throws InvalidObjectException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        HashMap<String, String> languages = LocalisationService.getInstance().getSupportedLanguages();
        List<List<String>> commands = new ArrayList<>();
        for (Map.Entry<String, String> entry : languages.entrySet()) {
            List<String> commandRow = new ArrayList<>();
            commandRow.add(entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW.toString() + " " + entry.getValue());
            commands.add(commandRow);
        }
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(true);
        replyKeyboardMarkup.setKeyboard(commands);
        replyKeyboardMarkup.setSelective(true);
        sendMessageRequest.setReplayMarkup(replyKeyboardMarkup);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("chooselanguage", language));
        try {
            sendMessage(sendMessageRequest);
            languageMessages.add(message.getFrom().getId());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onLanguageReceived(Message message) throws InvalidObjectException {
        String[] parts = message.getText().split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        if (LocalisationService.getInstance().getSupportedLanguages().containsKey(parts[0].trim())) {
            DatabaseManager.getInstance().putUserLanguage(message.getFrom().getId(), parts[0].trim());
            sendMessageRequest.setText(LocalisationService.getInstance().getString("languageModified", parts[0].trim()));
        } else {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("errorLanguage"));
        }
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setHideKeyboard(true);
        replyKeyboardHide.setSelective(true);
        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
        try {
            sendMessage(sendMessageRequest);
            languageMessages.remove(message.getFrom().getId());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }
}
