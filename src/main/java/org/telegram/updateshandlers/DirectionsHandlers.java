package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.DirectionsService;
import org.telegram.services.LocalisationService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.logging.BotLogger;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
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
        return BotConfig.DIRECTIONS_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            handleDirections(update);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.DIRECTIONS_USER;
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
                            SendMessage sendMessageRequest = new SendMessage();
                            sendMessageRequest.setText(LocalisationService.getString("youNeedReplyDirections", language));
                            sendMessageRequest.setChatId(message.getChatId());
                            try {
                                execute(sendMessageRequest);
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
        sendMessageRequest.setChatId(message.getChatId());
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardRemove);
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        for (String direction : directions) {
            sendMessageRequest.setText(direction);
            try {
                executeAsync(sendMessageRequest, new SentCallback<Message>() {
                    @Override
                    public void onResult(BotApiMethod<Message> botApiMethod, Message sentMessage) {
                        if (sentMessage != null) {
                            DatabaseManager.getInstance().deleteUserForDirections(message.getFrom().getId());
                        }
                    }

                    @Override
                    public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
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
        sendMessageRequest.setChatId(message.getChatId());
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplyMarkup(forceReplyKeyboard);
        sendMessageRequest.setText(LocalisationService.getString("sendDestination", language));

        try {
            executeAsync(sendMessageRequest, new SentCallback<Message>() {
                @Override
                public void onResult(BotApiMethod<Message> method, Message sentMessage) {
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_DESTINY_STATUS,
                                sentMessage.getMessageId(), message.getText());
                    }
                }

                @Override
                public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
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
                LocalisationService.getString("helpDirections", language),
                Commands.startDirectionCommand);
        sendMessageRequest.setText(helpDirectionsFormated);
        sendMessageRequest.setChatId(message.getChatId());
        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onStartdirectionsCommand(Message message, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId());
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessageRequest.setReplyMarkup(forceReplyKeyboard);
        sendMessageRequest.setText(LocalisationService.getString("initDirections", language));

        try {
            executeAsync(sendMessageRequest, new SentCallback<Message>() {
                @Override
                public void onResult(BotApiMethod<Message> method, Message sentMessage) {
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_ORIGIN_STATUS,
                                sentMessage.getMessageId(), null);
                    }
                }

                @Override
                public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
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
        sendMessageRequest.setChatId(message.getChatId());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<LocalisationService.Language> languages = LocalisationService.getSupportedLanguages();
        List<KeyboardRow> commands = new ArrayList<>();
        for (LocalisationService.Language languageItem : languages) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(languageItem.getCode() + " --> " + languageItem.getName());
            commands.add(commandRow);
        }
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(commands);
        replyKeyboardMarkup.setSelective(true);
        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
        sendMessageRequest.setText(LocalisationService.getString("chooselanguage", language));
        try {
            execute(sendMessageRequest);
            languageMessages.add(message.getFrom().getId());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void onLanguageSelected(Message message) throws InvalidObjectException {
        String[] parts = message.getText().split("-->", 2);
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
        try {
            execute(sendMessageRequest);
            languageMessages.remove(message.getFrom().getId());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }
}
