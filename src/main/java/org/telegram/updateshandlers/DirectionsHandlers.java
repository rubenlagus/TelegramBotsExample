package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.ForceReply;
import org.telegram.api.Message;
import org.telegram.api.ReplyKeyboardHide;
import org.telegram.api.Update;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.SendMessage;
import org.telegram.services.DirectionsService;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Directions Bot
 * @date 24 of June of 2015
 */
public class DirectionsHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENDIRECTIONS;
    private final int webhookPort = 9993;
    private final Webhook webhook;
    private final UpdatesThread updatesThread;
    private static final int WATING_ORIGIN_STATUS = 0;
    private static final int WATING_DESTINY_STATUS = 1;

    public DirectionsHandlers() {
        if (BuildVars.useWebHook) {
            webhook = new Webhook(this, webhookPort);
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
        handleDirections(update);
    }

    public void handleDirections(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (message.getText().startsWith(Commands.startDirectionCommand)) {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(message.getChatId());
                sendMessageRequest.setReplayToMessageId(message.getMessageId());
                ForceReply forceReply = new ForceReply();
                forceReply.setSelective(true);
                sendMessageRequest.setReplayMarkup(forceReply);
                sendMessageRequest.setText(CustomMessages.initDirections);
                Message sentMessage = SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                if (sentMessage != null) {
                    DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_ORIGIN_STATUS,
                            sentMessage.getMessageId(),null);
                }
            } else if (message.getText().startsWith(Commands.help) ||
                    (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setText(CustomMessages.helpDirections);
                sendMessageRequest.setChatId(message.getChatId());
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
            }  else if (!message.getText().startsWith("/")){
                if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == WATING_ORIGIN_STATUS &&
                        message.hasReplayMessage() &&
                        DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setChatId(message.getChatId());
                    sendMessageRequest.setReplayToMessageId(message.getMessageId());
                    ForceReply forceReply = new ForceReply();
                    forceReply.setSelective(true);
                    sendMessageRequest.setReplayMarkup(forceReply);
                    sendMessageRequest.setText(CustomMessages.sendDestination);
                    Message sentMessage = SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().addUserForDirection(message.getFrom().getId(), WATING_DESTINY_STATUS,
                                sentMessage.getMessageId(), message.getText());
                    }

                } else if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == WATING_DESTINY_STATUS &&
                        message.hasReplayMessage() &&
                        DatabaseManager.getInstance().getUserDestinationMessageId(message.getFrom().getId()) == message.getReplyToMessage().getMessageId()) {
                    String origin = DatabaseManager.getInstance().getUserOrigin(message.getFrom().getId());
                    String destiny = message.getText();
                    String directions = DirectionsService.getInstance().getDirections(origin, destiny);
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setChatId(message.getChatId());
                    ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
                    replyKeyboardHide.setSelective(true);
                    sendMessageRequest.setReplayMarkup(replyKeyboardHide);
                    sendMessageRequest.setReplayToMessageId(message.getMessageId());
                    sendMessageRequest.setText(directions);
                    Message sentMessage = SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    if (sentMessage != null) {
                        DatabaseManager.getInstance().deleteUserForDirections(message.getFrom().getId());
                    }
                } else if (!message.hasReplayMessage()) {
                    if (DatabaseManager.getInstance().getUserDestinationStatus(message.getFrom().getId()) == -1) {
                        SendMessage sendMessageRequest = new SendMessage();
                        sendMessageRequest.setText(CustomMessages.helpDirections);
                        sendMessageRequest.setChatId(message.getChatId());
                        SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    } else {
                        SendMessage sendMessageRequest = new SendMessage();
                        sendMessageRequest.setText(CustomMessages.youNeedReplyDirections);
                        sendMessageRequest.setChatId(message.getChatId());
                        SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    }
                }
            }
        }
    }
}
