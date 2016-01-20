package org.telegram.updateshandlers;

import org.json.JSONObject;
import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.BotLogger;
import org.telegram.services.DirectionsService;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.updateshandlers.SentCallback;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Directions Bot
 * @date 24 of June of 2015
 */
public class DirectionsHandlers extends TelegramLongPollingBot {
    private static final String LOGTAG = "DIRECTIONSHANDLERS";

    private static final int WATING_ORIGIN_STATUS = 0;
    private static final int WATING_DESTINY_STATUS = 1;
    private final ConcurrentLinkedQueue<Integer> languageMessages = new ConcurrentLinkedQueue<>();


    @Override
    public String getBotToken() {
        return BotConfig.TOKENDIRECTIONS;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            handleDirections(update);
        } catch (InvalidObjectException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.USERNAMEDIRECTIONS;
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
                            message.hasReplayMessage() &&
                            DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                        onOriginReceived(message, language);

                    } else if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == WATING_DESTINY_STATUS &&
                            message.hasReplayMessage() &&
                            DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                        onDestinationReceived(message, language);
                    } else if (!message.hasReplayMessage()) {
                        if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == -1) {
                            sendHelpMessage(message, language);
                        } else {
                            SendMessage sendMessageRequest = new SendMessage();
                            sendMessageRequest.setText(LocalisationService.getInstance().getString("youNeedReplyDirections", language));
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
        }
    }

    private void onDestinationReceived(Message message, String language) {
        String origin = DatabaseManager.getInstance().getUserOrigin(message.getFrom().getId());
        String destiny = message.getText();
        List<String> directions = DirectionsService.getInstance().getDirections(origin, destiny, language);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setSelective(true);
        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        for (String direction : directions) {
            sendMessageRequest.setText(direction);
            try {
                sendMessageAsync(sendMessageRequest, new SentCallback<Message>() {
                    @Override
                    public void onResult(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                        Message sentMessage = botApiMethod.deserializeResponse(jsonObject);
                        if (sentMessage != null) {
                            DatabaseManager.getInstance().deleteUserForDirections(message.getFrom().getId());
                        }
                    }

                    @Override
                    public void onError(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                    }

                    @Override
                    public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                    }
                });
            } catch (TelegramApiException e) {
                BotLogger.error(LOGTAG, e);
            }
        }

    }

    private void onOriginReceived(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplayMarkup(forceReplyKeyboard);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("sendDestination", language));

        try {
            sendMessageAsync(sendMessageRequest, new SentCallback<Message>() {
                @Override
                public void onResult(BotApiMethod<Message> method, JSONObject jsonObject) {
                    Message sentMessage = method.deserializeResponse(jsonObject);
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_DESTINY_STATUS,
                                sentMessage.getMessageId(), message.getText());
                    }
                }

                @Override
                public void onError(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                }

                @Override
                public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                }
            });
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }

    }

    private void sendHelpMessage(Message message, String language) throws InvalidObjectException {
        SendMessage sendMessageRequest = new SendMessage();
        String helpDirectionsFormated = String.format(
                LocalisationService.getInstance().getString("helpDirections", language),
                Commands.startDirectionCommand);
        sendMessageRequest.setText(helpDirectionsFormated);
        sendMessageRequest.setChatId(message.getChatId().toString());
        try {
            sendMessage(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onStartdirectionsCommand(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplayMarkup(forceReplyKeyboard);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("initDirections", language));

        try {
            sendMessageAsync(sendMessageRequest, new SentCallback<Message>() {
                @Override
                public void onResult(BotApiMethod<Message> method, JSONObject jsonObject) {
                    Message sentMessage = method.deserializeResponse(jsonObject);
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_ORIGIN_STATUS,
                                sentMessage.getMessageId(), null);
                    }
                }

                @Override
                public void onError(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                }

                @Override
                public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                }
            });
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
            commandRow.add(entry.getKey() + " --> " + entry.getValue());
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

    private void onLanguageSelected(Message message) throws InvalidObjectException {
        String[] parts = message.getText().split("-->", 2);
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
