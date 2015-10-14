package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.SenderHelper;
import org.telegram.api.methods.BotApiMethod;
import org.telegram.api.methods.SendMessage;
import org.telegram.api.objects.*;
import org.telegram.services.BotLogger;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to channel updates bot
 * This is a use case that will send a message to a channel if it is added as an admin to it.
 * @date 24 of June of 2015
 */
public class ChannelHandlers implements UpdatesCallback {
    private static final String LOGTAG = "CHANNELHANDLERS";
    private static final String TOKEN = BotConfig.TOKENCHANNEL;
    private static final String BOTNAME = BotConfig.USERNAMECHANNEL;
    private static final boolean USEWEBHOOK = false;

    private static final int WAITINGCHANNEL = 1;

    private static final String HELP_TEXT = "Send me the channel username where you added me as admin.";
    private static final String CANCEL_COMMAND = "/stop";
    private static final String AFTER_CHANNEL_TEXT = "A message to provided channel will be sent if the bot was added to it as admin.";
    private static final String WRONG_CHANNEL_TEXT = "Wrong username, please remember to add *@* before the username and send only the username.";
    private static final String CHANNEL_MESSAGE_TEXT = "This message was sent by *@updateschannelbot*. Enjoy!";
    private static final String ERROR_MESSAGE_TEXT = "There was an error sending the message to channel *%s*, the error was: ```%s```";

    private final ConcurrentHashMap<Integer, Integer> userState = new ConcurrentHashMap<>();

    private final Object webhookLock = new Object();

    public ChannelHandlers(Webhook webhook) {
        if (USEWEBHOOK && BuildVars.useWebHook) {
            webhook.registerWebhook(this, BOTNAME);
            SenderHelper.SendWebhook(Webhook.getExternalURL(BOTNAME), TOKEN);
        } else {
            SenderHelper.SendWebhook("", TOKEN);
            new UpdatesThread(TOKEN, this);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            try {
                BotApiMethod botApiMethod = handleIncomingMessage(message);
                if (botApiMethod != null) {
                    SenderHelper.SendApiMethod(botApiMethod, TOKEN);
                }
            } catch (InvalidObjectException e) {
                BotLogger.severe(LOGTAG, e);
            }
        }
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        throw new NoSuchMethodError();
    }


    // region Incoming messages handlers

    private BotApiMethod handleIncomingMessage(Message message) throws InvalidObjectException {
        int state = userState.getOrDefault(message.getFrom().getId(), 0);
        BotApiMethod botApiMethod;
        switch(state) {
            case WAITINGCHANNEL:
                botApiMethod = onWaitingChannelMessage(message);
                break;
            default:
                botApiMethod = sendHelpMessage(message.getChatId().toString(), message.getMessageId(), null);
                userState.put(message.getFrom().getId(), WAITINGCHANNEL);
                break;
        }
        return botApiMethod;
    }

    private BotApiMethod onWaitingChannelMessage(Message message) throws InvalidObjectException {
        BotApiMethod sendMessage = null;
        if (message.getText().equals(CANCEL_COMMAND)) {
            userState.remove(message.getFrom().getId());
            sendMessage = sendHelpMessage(message.getChatId().toString(), message.getMessageId(), null);
        } else {
            if (message.getText().startsWith("@") && !message.getText().trim().contains(" ")) {
                sendBuiltMessage(getMessageToChannelSent(message));
                sendMessageToChannel(message.getText(), message);
                userState.remove(message.getFrom().getId());
            } else {
                sendMessage = getWrongUsernameMessage(message);
            }
        }

        return sendMessage;
    }

    private static void sendMessageToChannel(String username, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(username.trim());

        sendMessage.setText(CHANNEL_MESSAGE_TEXT);
        sendMessage.enableMarkdown(true);

        try {
            sendBuiltMessage(sendMessage);
        } catch (InvalidObjectException e) {
            sendErrorMessage(message, e.getMessage());
        }
    }

    private static void sendErrorMessage(Message message, String errorText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplayToMessageId(message.getMessageId());

        sendMessage.setText(String.format(ERROR_MESSAGE_TEXT, message.getText().trim(), errorText.replace("\"", "\\\"")));
        sendMessage.enableMarkdown(true);

        try {
            sendBuiltMessage(sendMessage);
        } catch (InvalidObjectException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private static BotApiMethod getWrongUsernameMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplayToMessageId(message.getMessageId());

        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        forceReplyKeyboard.setForceReply(true);
        sendMessage.setReplayMarkup(forceReplyKeyboard);

        sendMessage.setText(WRONG_CHANNEL_TEXT);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    private static SendMessage getMessageToChannelSent(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplayToMessageId(message.getMessageId());

        sendMessage.setText(AFTER_CHANNEL_TEXT);
        return sendMessage;
    }

    private static BotApiMethod sendHelpMessage(String chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
        }

        sendMessage.setText(HELP_TEXT);
        return sendMessage;
    }

    private static void sendBuiltMessage(SendMessage sendMessage) throws InvalidObjectException {
        SenderHelper.SendApiMethod(sendMessage, TOKEN);
    }
}
