package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.DirectionsService;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Handler for updates to Directions Bot
 */
@Slf4j
public class DirectionsHandlers implements LongPollingSingleThreadUpdateConsumer {
    private static final int WATING_ORIGIN_STATUS = 0;
    private static final int WATING_DESTINY_STATUS = 1;
    private final ConcurrentLinkedQueue<Long> languageMessages = new ConcurrentLinkedQueue<>();
    private final TelegramClient telegramClient;

    public DirectionsHandlers(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        try {
            handleDirections(update);
        } catch (Exception e) {
            log.error("Error processing update in directions bot", e);
        }
    }

    private void handleDirections(Update update) throws InvalidObjectException {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (languageMessages.contains(message.getFrom().getId())) {
                onLanguageSelected(message);
            } else {
                String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
                if (message.getText().startsWith(Commands.setLanguageCommand)) {
                    onSetLanguageCommand(message, language);
                } else if (message.getText().startsWith(Commands.startDirectionCommand)) {
                    onStartdirectionsCommand(message, language);
                } else if ((message.getText().startsWith(Commands.help) ||
                        (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) &&
                        DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == -1) {
                    sendHelpMessage(message, language);
                } else if (!message.getText().startsWith("/")) {
                    if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == WATING_ORIGIN_STATUS &&
                            message.isReply() &&
                            DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                        onOriginReceived(message, language);

                    } else if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == WATING_DESTINY_STATUS &&
                            message.isReply() &&
                            DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                        onDestinationReceived(message, language);
                    } else if (!message.isReply()) {
                        if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == -1) {
                            sendHelpMessage(message, language);
                        } else {
                            SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("youNeedReplyDirections", language));
                            try {
                                telegramClient.execute(sendMessageRequest);
                            } catch (TelegramApiException e) {
                                log.error("Error handling directions", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void onDestinationReceived(Message message, String language) {
        String origin = DatabaseManager.getInstance().getUserOrigin(message.getFrom().getId());
        String destiny = message.getText();
        List<String> directions = DirectionsService.getInstance().getDirections(origin, destiny, language);
        SendMessage.SendMessageBuilder<?, ?> sendMessageRequestBuilder = SendMessage.builder();
        sendMessageRequestBuilder.chatId(message.getChatId());
        ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder().selective(true).build();
        sendMessageRequestBuilder.replyMarkup(replyKeyboardRemove);
        sendMessageRequestBuilder.replyToMessageId(message.getMessageId());
        for (String direction : directions) {
            sendMessageRequestBuilder.text(direction);
            try {
                telegramClient.executeAsync(sendMessageRequestBuilder.build()).thenAccept(sentMessage -> {
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().deleteUserForDirections(message.getFrom().getId());
                    }
                }).thenAccept(unused -> log.info("Sent destination received"));
            } catch (TelegramApiException e) {
                log.error("Error on destination received", e);
            }
        }

    }

    private void onOriginReceived(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("sendDestination", language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplyMarkup(forceReplyKeyboard);

        try {
            telegramClient.executeAsync(sendMessageRequest).thenAccept(sentMessage -> {
                if (sentMessage != null) {
                    DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_DESTINY_STATUS,
                            sentMessage.getMessageId(), message.getText());
                }
            }).thenAccept(unused -> log.info("Sent origin received"));
        } catch (TelegramApiException e) {
            log.error("Error on origin received", e);
        }
    }

    private void sendHelpMessage(Message message, String language) throws InvalidObjectException {
        String helpDirectionsFormated = String.format(
                LocalisationService.getString("helpDirections", language),
                Commands.startDirectionCommand);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), helpDirectionsFormated);
        try {
            telegramClient.execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            log.error("Error sending help", e);
        }
    }

    private void onStartdirectionsCommand(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("initDirections", language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplyMarkup(forceReplyKeyboard);
        try {
            telegramClient.executeAsync(sendMessageRequest).thenAccept(sentMessage -> {
                if (sentMessage != null) {
                    DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_ORIGIN_STATUS,
                            sentMessage.getMessageId(), null);
                }
            }).thenAccept(unused -> log.info("Sent start directions"));
        } catch (TelegramApiException e) {
            log.error("Error on start directions", e);
        }
    }

    private void onSetLanguageCommand(Message message, String language) throws InvalidObjectException {
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("chooselanguage", language));
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        List<LocalisationService.Language> languages = LocalisationService.getSupportedLanguages();
        List<KeyboardRow> commands = new ArrayList<>();
        for (LocalisationService.Language languageItem : languages) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(languageItem.getCode() + " --> " + languageItem.getName());
            commands.add(commandRow);
        }
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(true);
        replyKeyboardMarkupBuilder.keyboard(commands);
        replyKeyboardMarkupBuilder.selective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkupBuilder.build());
        try {
            telegramClient.execute(sendMessageRequest);
            languageMessages.add(message.getFrom().getId());
        } catch (TelegramApiException e) {
            log.error("Error setting language", e);
        }
    }

    private void onLanguageSelected(Message message) throws InvalidObjectException {
        String[] parts = message.getText().split("-->", 2);
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
        try {
            telegramClient.execute(sendMessageRequestBuilder.build());
            languageMessages.remove(message.getFrom().getId());
        } catch (TelegramApiException e) {
            log.error("Error on lanaguage selected", e);
        }
    }
}
