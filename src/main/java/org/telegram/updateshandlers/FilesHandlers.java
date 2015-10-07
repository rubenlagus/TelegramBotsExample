package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.Commands;
import org.telegram.SenderHelper;
import org.telegram.api.objects.Message;
import org.telegram.api.objects.ReplyKeyboardHide;
import org.telegram.api.objects.ReplyKeyboardMarkup;
import org.telegram.api.objects.Update;
import org.telegram.database.DatabaseManager;
import org.telegram.api.methods.BotApiMethod;
import org.telegram.api.methods.SendDocument;
import org.telegram.api.methods.SendMessage;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

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
public class FilesHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENFILES;
    private static final String BOTNAME = BotConfig.USERNAMEFILES;
    private static final boolean USEWEBHOOK = false;

    private static final int INITIAL_UPLOAD_STATUS = 0;
    private static final int DELETE_UPLOADED_STATUS = 1;
    private final ConcurrentLinkedQueue<Integer> languageMessages = new ConcurrentLinkedQueue<>();

    public FilesHandlers(Webhook webhook) {
        if (USEWEBHOOK) {
            webhook.registerWebhook(this, BOTNAME);
            SenderHelper.SendWebhook(Webhook.getExternalURL(BOTNAME), TOKEN);
        } else {
            SenderHelper.SendWebhook("", TOKEN);
            new UpdatesThread(TOKEN, this);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        handleFileUpdate(update);
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        // Webhook not supported in this example
        return null;
    }

    public void handleFileUpdate(Update update) {
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
            sendMessageRequest.setChatId(message.getChatId());
            SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
        }
    }

    private void onListCommand(Message message, String language) {
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
        sendMessageRequest.setChatId(message.getChatId());
        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setHideKeyboard(true);
        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
    }

    private void onDeleteCommand(Message message, String language, String[] parts) {
        if (DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == DELETE_UPLOADED_STATUS &&
                parts.length == 2) {
            onDeleteCommandWithParameters(message, language, parts[1]);
        } else {
            onDeleteCommandWithoutParameters(message, language);
        }
    }

    private void onDeleteCommandWithoutParameters(Message message, String language) {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), DELETE_UPLOADED_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("deleteUploadedFile", language));
        sendMessageRequest.setChatId(message.getChatId());
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
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
    }

    private void onDeleteCommandWithParameters(Message message, String language, String part) {
        String[] innerParts = part.split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        boolean removed = DatabaseManager.getInstance().deleteFile(innerParts[0].trim());
        SendMessage sendMessageRequest = new SendMessage();
        if (removed) {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("fileDeleted", language));
        } else {
            sendMessageRequest.setText(LocalisationService.getInstance().getString("wrongFileId", language));
        }
        sendMessageRequest.setChatId(message.getChatId());
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
    }

    private void onCancelCommand(Message message, String language) {
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("processFinished", language));
        sendMessageRequest.setChatId(message.getChatId());
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
    }

    private void onUploadCommand(Message message, String language) {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), INITIAL_UPLOAD_STATUS);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setText(LocalisationService.getInstance().getString("sendFileToUpload", language));
        sendMessageRequest.setChatId(message.getChatId());
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
    }

    private void sendHelpMessage(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        String formatedString = String.format(
                LocalisationService.getInstance().getString("helpFiles", language),
                Commands.startCommand, Commands.uploadCommand, Commands.deleteCommand,
                Commands.listCommand);
        sendMessageRequest.setText(formatedString);
        sendMessageRequest.setChatId(message.getChatId());
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
    }

    private void onStartWithParameters(Message message, String language, String part) {
        if (DatabaseManager.getInstance().doesFileExists(part.trim())) {
            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setDocument(part.trim());
            sendDocumentRequest.setChatId(message.getChatId());
            SenderHelper.SendDocument(sendDocumentRequest, TOKEN);
        } else {
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setText(LocalisationService.getInstance().getString("wrongFileId", language));
            sendMessageRequest.setChatId(message.getChatId());
            SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
        }
    }

    private void onSetLanguageCommand(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId());
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
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
        languageMessages.add(message.getFrom().getId());
    }

    private void onLanguageReceived(Message message) {
        String[] parts = message.getText().split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId());
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
        SenderHelper.SendApiMethod(sendMessageRequest, TOKEN);
        languageMessages.remove(message.getFrom().getId());
    }
}
