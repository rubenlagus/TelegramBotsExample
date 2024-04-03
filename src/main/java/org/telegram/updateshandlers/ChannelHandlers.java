package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InvalidObjectException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Handler for updates to channel updates bot
 * This is a use case that will send a message to a channel if it is added as an admin to it.
 */
@Slf4j
public class ChannelHandlers implements LongPollingSingleThreadUpdateConsumer {
    private static final int WAITINGCHANNEL = 1;

    private static final String HELP_TEXT = "Send me the channel username where you added me as admin.";
    private static final String CANCEL_COMMAND = "/stop";
    private static final String AFTER_CHANNEL_TEXT = "A message to provided channel will be sent if the bot was added to it as admin.";
    private static final String WRONG_CHANNEL_TEXT = "Wrong username, please remember to add *@* before the username and send only the username.";
    private static final String CHANNEL_MESSAGE_TEXT = "This message was sent by *@updateschannelbot*. Enjoy!";
    private static final String ERROR_MESSAGE_TEXT = "There was an error sending the message to channel *%s*, the error was: ```%s```";

    private final ConcurrentHashMap<Long, Integer> userState = new ConcurrentHashMap<>();
    private final TelegramClient telegramClient;

    public ChannelHandlers(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        try {
            Message message = update.getMessage();
            if (message != null && message.hasText()) {
                try {
                    handleIncomingMessage(message);
                } catch (InvalidObjectException e) {
                    log.error("Channel Handler Error", e);
                }
            }
        } catch (Exception e) {
            log.error("Error handling channel message", e);
        }
    }

    // region Incoming messages handlers

    private void handleIncomingMessage(Message message) throws InvalidObjectException {
        int state = userState.getOrDefault(message.getFrom().getId(), 0);
        switch(state) {
            case WAITINGCHANNEL:
                onWaitingChannelMessage(message);
                break;
            default:
                sendHelpMessage(message.getChatId(), message.getMessageId(), null);
                userState.put(message.getFrom().getId(), WAITINGCHANNEL);
                break;
        }
    }

    private void onWaitingChannelMessage(Message message) {
        try {
            if (message.getText().equals(CANCEL_COMMAND)) {
                userState.remove(message.getFrom().getId());
                sendHelpMessage(message.getChatId(), message.getMessageId(), null);
            } else {
                if (message.getText().startsWith("@") && !message.getText().trim().contains(" ")) {
                    telegramClient.execute(getMessageToChannelSent(message));
                    sendMessageToChannel(message.getText(), message);
                    userState.remove(message.getFrom().getId());
                } else {
                    telegramClient.execute(getWrongUsernameMessage(message));
                }
            }
        } catch (TelegramApiException e) {
            log.error("Error waiting channel message", e);
        }
    }

    private void sendMessageToChannel(String username, Message message) {
        SendMessage sendMessage = new SendMessage(username.trim(), CHANNEL_MESSAGE_TEXT);
        sendMessage.enableMarkdown(true);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            sendErrorMessage(message, e.getMessage());
        }
    }

    private void sendErrorMessage(Message message, String errorText) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), String.format(ERROR_MESSAGE_TEXT, message.getText().trim(), errorText.replace("\"", "\\\"")));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending error message", e);
        }
    }

    private static SendMessage getWrongUsernameMessage(Message message) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), WRONG_CHANNEL_TEXT);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());

        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    private static SendMessage getMessageToChannelSent(Message message) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), AFTER_CHANNEL_TEXT);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    private void sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), HELP_TEXT);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending help message", e);
        }
    }
}
