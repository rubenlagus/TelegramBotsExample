package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.ForceReply;
import org.telegram.api.Message;
import org.telegram.api.ReplyKeyboardMarkup;
import org.telegram.api.Update;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.SendMessage;
import org.telegram.services.BotLogger;
import org.telegram.services.WeatherService;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Weather Bot
 * @date 24 of June of 2015
 */
public class WeatherHandlers implements UpdatesCallback {
    private static volatile BotLogger log = BotLogger.getLogger(WeatherHandlers.class.getName());

    private static final String TOKEN = BotConfig.TOKENWEATHER;
    private static final int CURRENTWEATHERID = 0;
    private static final int FORECASTWEATHERID = 1;

    private static final int webhookPort = 9990;
    private final Webhook webhook;
    private final UpdatesThread updatesThread;
    private ConcurrentHashMap<Integer, Integer> listOfSentMessages = new ConcurrentHashMap<>();

    public WeatherHandlers() {
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
        sendWeatherInformation(update);
    }

    public void sendWeatherInformation(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            String[] parts = text.split(" ", 2);
            if (parts[0].startsWith(Commands.WEATHERCOMMAND)) {
                if (parts.length == 2) {
                    String citywithoutdescription = parts[1].split("-->", 2)[0].trim();
                    String weather = WeatherService.getInstance().fetchWeatherForecast(citywithoutdescription, message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(weather);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else {
                    HashMap<Integer, String> recentWeather = DatabaseManager.getInstance().getRecentWeather(message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    if (recentWeather.size() > 0) {
                        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                        List<List<String>> commands = new ArrayList<>();
                        for (Map.Entry<Integer, String> entry : recentWeather.entrySet()) {
                            List<String> commandRow = new ArrayList<>();
                            commandRow.add(Commands.WEATHERCOMMAND + " " + entry.getKey() + " --> " + entry.getValue());
                            commands.add(commandRow);
                        }
                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboad(true);
                        replyKeyboardMarkup.setSelective(true);
                        replyKeyboardMarkup.setKeyboard(commands);
                        sendMessageRequest.setReplayMarkup(replyKeyboardMarkup);
                        sendMessageRequest.setText(CustomMessages.chooseFromRecentWeather);
                    } else {
                        sendMessageRequest.setText(CustomMessages.pleaseSendMeCityWeather);
                        ForceReply forceReply = new ForceReply();
                        forceReply.setForceReply(true);
                        forceReply.setSelective(true);
                        sendMessageRequest.setReplayMarkup(forceReply);
                    }
                    sendMessageRequest.setReplayToMessageId(message.getMessageId());
                    sendMessageRequest.setChatId(message.getChatId());
                    Message sentMessage = SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    try {
                        listOfSentMessages.put(sentMessage.getMessageId(), FORECASTWEATHERID);
                    } catch(NullPointerException e) {
                        log.error(e);
                    }
                }
            } else if (parts[0].startsWith(Commands.CURRENTWEATHERCOMMAND)) {
                if (parts.length == 2) {
                    String citywithoutdescription = parts[1].split("-->", 2)[0].trim();
                    String weather = WeatherService.getInstance().fetchWeatherCurrent(citywithoutdescription, message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setText(weather);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else {
                    HashMap<Integer, String> recentWeather = DatabaseManager.getInstance().getRecentWeather(message.getFrom().getId());
                    SendMessage sendMessageRequest = new SendMessage();
                    if (recentWeather.size() > 0) {
                        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                        List<List<String>> commands = new ArrayList<>();
                        for (Map.Entry<Integer, String> entry : recentWeather.entrySet()) {
                            List<String> commandRow = new ArrayList<>();
                            commandRow.add(Commands.CURRENTWEATHERCOMMAND + " " + entry.getKey() + " --> " + entry.getValue());
                            commands.add(commandRow);
                        }
                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboad(true);
                        replyKeyboardMarkup.setSelective(true);
                        replyKeyboardMarkup.setKeyboard(commands);
                        sendMessageRequest.setReplayMarkup(replyKeyboardMarkup);
                        sendMessageRequest.setText(CustomMessages.chooseFromRecentWeather);
                    } else {
                        sendMessageRequest.setText(CustomMessages.pleaseSendMeCityWeather);
                        ForceReply forceReply = new ForceReply();
                        forceReply.setForceReply(true);
                        forceReply.setSelective(true);
                        sendMessageRequest.setReplayMarkup(forceReply);
                    }
                    sendMessageRequest.setChatId(message.getChatId());
                    sendMessageRequest.setReplayToMessageId(message.getMessageId());
                    Message sentMessage = SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    try {
                        listOfSentMessages.put(sentMessage.getMessageId(), CURRENTWEATHERID);
                    } catch(NullPointerException e) {
                        log.error(e);
                    }
                }
            } else if (message.isReply() && listOfSentMessages.contains(message.getReplyToMessage().getMessageId())) {
                SendMessage sendMessageRequest = new SendMessage();
                if (listOfSentMessages.remove(message.getReplyToMessage().getMessageId()) == CURRENTWEATHERID) {
                    String weather = WeatherService.getInstance().fetchWeatherCurrent(message.getText(), message.getFrom().getId());
                    sendMessageRequest.setText(weather);
                } else {
                    String weather = WeatherService.getInstance().fetchWeatherForecast(message.getText(), message.getFrom().getId());
                    sendMessageRequest.setText(weather);
                }
                sendMessageRequest.setChatId(message.getChatId());
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
            } else if (parts[0].startsWith(Commands.help) ||
                    (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setText(CustomMessages.helpWeather);
                sendMessageRequest.setChatId(message.getChatId());
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
            }
        } else if (message != null && message.hasLocation()) {
            String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                    message.getLocation().getLatitude(), message.getFrom().getId());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(message.getChatId());
            SenderHelper.SendMessage(sendMessageRequest, TOKEN);
        }
    }
}
