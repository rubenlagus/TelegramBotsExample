package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Handler for updates to Files Bot
 * This bot is an example for the use of sendMessage asynchronously
 */
@Slf4j
public class FilesHandlers implements LongPollingSingleThreadUpdateConsumer {
    private static final int INITIAL_UPLOAD_STATUS = 0;
    private static final int DELETE_UPLOADED_STATUS = 1;
    private final ConcurrentLinkedQueue<Long> languageMessages = new ConcurrentLinkedQueue<>();
    private final TelegramClient telegramClient;

    public FilesHandlers(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
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
                    log.error("Error handling file update", e);
                }
            }
        } catch (Exception e) {
            log.error("Unknown exception", e);
        }
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
            SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("fileUploaded", language) +
                    LocalisationService.getString("uploadedFileURL", language) + message.getDocument().getFileId());
            telegramClient.execute(sendMessageRequest);
        }
    }

    private void onListCommand(Message message, String language) throws InvalidObjectException, TelegramApiException {
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        SendMessage.SendMessageBuilder<?, ?> sendMessageRequestBuilder = SendMessage.builder();
        if (!files.isEmpty()) {
            StringBuilder text = new StringBuilder(LocalisationService.getString("listOfFiles", language) + ":\n\n");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                text.append(LocalisationService.getString("uploadedFileURL", language)).append(entry.getKey()).append(" ").append(Emoji.LEFT_RIGHT_ARROW).append(" ").append(entry.getValue()).append("\n");
            }
            sendMessageRequestBuilder.text(text.toString());
        } else {
            sendMessageRequestBuilder.text(LocalisationService.getString("noFiles", language));
        }
        sendMessageRequestBuilder.chatId(message.getChatId());
        sendMessageRequestBuilder.replyMarkup(ReplyKeyboardRemove.builder().build());
        telegramClient.execute(sendMessageRequestBuilder.build());
    }

    private void onDeleteCommand(Message message, String language, String[] parts) throws InvalidObjectException, TelegramApiException {
        if (DatabaseManager.getInstance().getUserStatusForFile(message.getFrom().getId()) == DELETE_UPLOADED_STATUS &&
                parts.length == 2) {
            onDeleteCommandWithParameters(message, language, parts[1]);
        } else {
            onDeleteCommandWithoutParameters(message, language);
        }
    }

    private void onDeleteCommandWithoutParameters(Message message, String language) throws TelegramApiException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), DELETE_UPLOADED_STATUS);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("deleteUploadedFile", language));
        HashMap<String, String> files = DatabaseManager.getInstance().getFilesByUser(message.getFrom().getId());
        ReplyKeyboardMarkup replyKeyboardMarkup = null;
        if (!files.isEmpty()) {
            List<KeyboardRow> commands = new ArrayList<>();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add(Commands.deleteCommand + " " + entry.getKey() + " " + Emoji.LEFT_RIGHT_ARROW
                        + " " + entry.getValue());
                commands.add(commandRow);
            }
            replyKeyboardMarkup = new ReplyKeyboardMarkup(commands);
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
        }
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
        telegramClient.execute(sendMessageRequest);
    }

    private void onDeleteCommandWithParameters(Message message, String language, String part) throws TelegramApiException {
        String[] innerParts = part.split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        boolean removed = DatabaseManager.getInstance().deleteFile(innerParts[0].trim());
        SendMessage.SendMessageBuilder<?, ?> sendMessageRequestBuilder = SendMessage.builder();
        if (removed) {
            sendMessageRequestBuilder.text(LocalisationService.getString("fileDeleted", language));
        } else {
            sendMessageRequestBuilder.text(LocalisationService.getString("wrongFileId", language));
        }
        sendMessageRequestBuilder.chatId(message.getChatId());

        telegramClient.execute(sendMessageRequestBuilder.build());
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());

    }

    private void onCancelCommand(Message message, String language) throws TelegramApiException {
        DatabaseManager.getInstance().deleteUserForFile(message.getFrom().getId());
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("processFinished", language));
        telegramClient.execute(sendMessageRequest);
    }

    private void onUploadCommand(Message message, String language) throws TelegramApiException {
        DatabaseManager.getInstance().addUserForFile(message.getFrom().getId(), INITIAL_UPLOAD_STATUS);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("sendFileToUpload", language));
        telegramClient.execute(sendMessageRequest);
    }

    private void sendHelpMessage(Message message, String language) throws TelegramApiException {

        String formatedString = String.format(
                LocalisationService.getString("helpFiles", language),
                Commands.startCommand, Commands.uploadCommand, Commands.deleteCommand,
                Commands.listCommand);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), formatedString);
        telegramClient.execute(sendMessageRequest);
    }

    private void onStartWithParameters(Message message, String language, String part) throws TelegramApiException {
        if (DatabaseManager.getInstance().doesFileExists(part.trim())) {
            SendDocument sendDocumentRequest = new SendDocument(String.valueOf(message.getChatId()), new InputFile(part.trim()));
            telegramClient.execute(sendDocumentRequest);
        } else {
            SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("wrongFileId", language));
            telegramClient.execute(sendMessageRequest);
        }
    }

    private void onSetLanguageCommand(Message message, String language) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("chooselanguage", language));
        List<LocalisationService.Language> languages = LocalisationService.getSupportedLanguages();
        List<KeyboardRow> commands = new ArrayList<>();
        for (LocalisationService.Language languageItem : languages) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(languageItem.getCode() + " " + Emoji.LEFT_RIGHT_ARROW + " " + languageItem.getName());
            commands.add(commandRow);
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(commands);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
        telegramClient.execute(sendMessageRequest);
        languageMessages.add(message.getFrom().getId());
    }

    private void onLanguageReceived(Message message) throws TelegramApiException {
        String[] parts = message.getText().split(Emoji.LEFT_RIGHT_ARROW.toString(), 2);
        SendMessage.SendMessageBuilder<?, ?> sendMessageRequestBuilder = SendMessage.builder();
        sendMessageRequestBuilder.chatId(message.getChatId());
        if (LocalisationService.getLanguageByCode(parts[0].trim()) != null) {
            DatabaseManager.getInstance().putUserLanguage(message.getFrom().getId(), parts[0].trim());
            sendMessageRequestBuilder.text(LocalisationService.getString("languageModified", parts[0].trim()));
        } else {
            sendMessageRequestBuilder.text(LocalisationService.getString("errorLanguage"));
        }
        sendMessageRequestBuilder.replyToMessageId(message.getMessageId());
        ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder().selective(true).build();
        sendMessageRequestBuilder.replyMarkup(replyKeyboardRemove);
        telegramClient.execute(sendMessageRequestBuilder.build());
        languageMessages.remove(message.getFrom().getId());
    }
}
