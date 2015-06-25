package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.Message;
import org.telegram.api.ReplyKeyboardMarkup;
import org.telegram.api.Update;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.SendDocument;
import org.telegram.methods.SendMessage;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Files Bot
 * @date 24 of June of 2015
 */
public class FilesHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENFILES;
    private static final String webhookPath = "filesBot";

    private static final int INITIAL_UPLOAD_STATUS = 0;
    private static final int DELETE_UPLOADED_STATUS = 1;
    private final Webhook webhook;
    private final UpdatesThread updatesThread;

    public FilesHandlers() {
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
        handleFileUpdate(update);
    }

    public void handleFileUpdate(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String[] parts = message.getText().split(" ", 2);
            if (parts[0].startsWith(Commands.startCommand)) {
                if (parts.length == 2) {
                    if (DatabaseManager.getInstance().doesFileExists(parts[1].trim())) {
                        SendDocument sendDocumentRequest = new SendDocument();
                        sendDocumentRequest.setDocument(parts[1].trim());
                        sendDocumentRequest.setChatId(message.getChatId());
                        SenderHelper.SendDocument(sendDocumentRequest, TOKEN);
                    } else {
                        SendMessage sendMessageRequest = new SendMessage();
                        sendMessageRequest.setText(CustomMessages.wrongFileId);
                        sendMessageRequest.setChatId(message.getChatId());
                        SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    }
                } else {
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(CustomMessages.helpFiles);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                }
            } else if (!message.isGroupMessage()){
                if (parts[0].startsWith(Commands.uploadCommand)) { // Open upload for user
                    DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), INITIAL_UPLOAD_STATUS);
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(CustomMessages.sendFileToUpload);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else if (parts[0].startsWith(Commands.cancelCommand)) {
                    DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(CustomMessages.processFinished);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else if (parts[0].startsWith(Commands.deleteCommand)) {
                    if (DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == DELETE_UPLOADED_STATUS &&
                            parts.length == 2) {
                        String[] innerParts = parts[1].split("-->", 2);
                        boolean removed = DatabaseManager.getInstance().deleteFile(innerParts[0].trim());
                        SendMessage sendMessageRequest = new SendMessage();
                        if (removed) {
                            sendMessageRequest.setText(CustomMessages.fileDeleted);
                        } else {
                            sendMessageRequest.setText(CustomMessages.wrongFileId);
                        }
                        sendMessageRequest.setChatId(message.getChatId());
                        SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
                    } else {
                        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), DELETE_UPLOADED_STATUS);
                        SendMessage sendMessageRequest = new SendMessage();
                        sendMessageRequest.setText(CustomMessages.deleteUploadedFile);
                        sendMessageRequest.setChatId(message.getChatId());
                        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
                        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                        if (files.size() > 0) {
                            List<List<String>> commands = new ArrayList<>();
                            for (Map.Entry<String, String> entry : files.entrySet()) {
                                List<String> commandRow = new ArrayList<>();
                                commandRow.add(Commands.deleteCommand + " " + entry.getKey() + " --> " + entry.getValue());
                                commands.add(commandRow);
                            }
                            replyKeyboardMarkup.setResizeKeyboard(true);
                            replyKeyboardMarkup.setOneTimeKeyboad(true);
                            replyKeyboardMarkup.setKeyboard(commands);
                        }
                        sendMessageRequest.setReplayMarkup(replyKeyboardMarkup);
                        SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    }
                } else if (parts[0].startsWith(Commands.listCommand)) {
                    HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    if (files.size() > 0) {
                        String text = CustomMessages.listOfFiles + ":\n\n";
                        for (Map.Entry<String, String> entry : files.entrySet()) {
                            text += CustomMessages.uploadedFileURL + entry.getKey() + " --> " + entry.getValue() + "\n";
                        }
                        sendMessageRequest.setText(text);
                    } else {
                        sendMessageRequest.setText(CustomMessages.noFiles);
                    }
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else {
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(CustomMessages.helpFiles);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                }
            }
        } else if ( message != null && message.hasDocument()
                && DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == INITIAL_UPLOAD_STATUS) {
            DatabaseManager.getInstance().addFile(message.getDocument().getFileId(), message.getFrom().getId(), message.getDocument().getFileName());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setText(CustomMessages.fileUploaded + CustomMessages.uploadedFileURL + message.getDocument().getFileId());
            sendMessageRequest.setChatId(message.getChatId());
            SenderHelper.SendMessage(sendMessageRequest, TOKEN);
        }
    }
}
