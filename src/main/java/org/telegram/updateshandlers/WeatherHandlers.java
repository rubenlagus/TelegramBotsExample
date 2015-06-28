package org.telegram.updateshandlers;

import org.telegram.*;
import org.telegram.api.*;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.ForwardMessage;
import org.telegram.methods.SendMessage;
import org.telegram.services.BotLogger;
import org.telegram.services.LocalisationService;
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

    private static final String webhookPath = "weatherBot";
    private final Webhook webhook;
    private final UpdatesThread updatesThread;
    private ConcurrentHashMap<Integer, Integer> listOfSentMessages = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> languageMessages = new ConcurrentLinkedQueue<>();

    public WeatherHandlers() {
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
        sendWeatherInformation(update);
    }

    public void sendWeatherInformation(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (languageMessages.contains(message.getFrom().getId())) {
                String[] parts = message.getText().split("-->", 2);
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(message.getChatId());
                if (LocalisationService.getInstance().supportedLanguages.containsKey(parts[0].trim())) {
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
                SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                languageMessages.remove(message.getFrom().getId());
            } else {
                String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
                String text = message.getText();
                String[] parts = text.split(" ", 2);
                if (message.getText().startsWith(Commands.setLanguageCommand)) {
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setChatId(message.getChatId());
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    HashMap<String, String> languages = LocalisationService.getInstance().supportedLanguages;
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
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                    languageMessages.add(message.getFrom().getId());
                } else if (parts[0].startsWith(Commands.WEATHERCOMMAND)) {
                    if (parts.length == 2) {
                        String citywithoutdescription = parts[1].split("-->", 2)[0].trim();
                        String weather = WeatherService.getInstance().fetchWeatherForecast(citywithoutdescription,
                                message.getFrom().getId(), language);
                        SendMessage sendMessageRequest = new SendMessage();
                        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
                        replyKeyboardHide.setSelective(true);
                        replyKeyboardHide.setHideKeyboard(true);
                        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
                        sendMessageRequest.setReplayToMessageId(update.getMessage().getMessageId());
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
                            sendMessageRequest.setText(LocalisationService.getInstance().getString("chooseFromRecentWeather", language));
                        } else {
                            sendMessageRequest.setText(LocalisationService.getInstance().getString("pleaseSendMeCityWeather", language));
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
                        } catch (NullPointerException e) {
                            log.error(e);
                        }
                    }
                } else if (parts[0].startsWith(Commands.CURRENTWEATHERCOMMAND)) {
                    if (parts.length == 2) {
                        String citywithoutdescription = parts[1].split("-->", 2)[0].trim();
                        String weather = WeatherService.getInstance().fetchWeatherCurrent(citywithoutdescription,
                                message.getFrom().getId(), language);
                        SendMessage sendMessageRequest = new SendMessage();
                        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
                        replyKeyboardHide.setSelective(true);
                        replyKeyboardHide.setHideKeyboard(true);
                        sendMessageRequest.setReplayMarkup(replyKeyboardHide);
                        sendMessageRequest.setReplayToMessageId(update.getMessage().getMessageId());
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
                            sendMessageRequest.setText(LocalisationService.getInstance().getString("chooseFromRecentWeather", language));
                        } else {
                            sendMessageRequest.setText(LocalisationService.getInstance().getString("pleaseSendMeCityWeather", language));
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
                        } catch (NullPointerException e) {
                            log.error(e);
                        }
                    }
                } else if (message.isReply() && listOfSentMessages.contains(message.getReplyToMessage().getMessageId())) {
                    SendMessage sendMessageRequest = new SendMessage();
                    if (listOfSentMessages.remove(message.getReplyToMessage().getMessageId()) == CURRENTWEATHERID) {
                        String weather = WeatherService.getInstance().fetchWeatherCurrent(message.getText(),
                                message.getFrom().getId(), language);
                        sendMessageRequest.setText(weather);
                    } else {
                        String weather = WeatherService.getInstance().fetchWeatherForecast(message.getText(),
                                message.getFrom().getId(), language);
                        sendMessageRequest.setText(weather);
                    }
                    ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
                    replyKeyboardHide.setSelective(true);
                    replyKeyboardHide.setHideKeyboard(true);
                    sendMessageRequest.setReplayMarkup(replyKeyboardHide);
                    sendMessageRequest.setReplayToMessageId(update.getMessage().getMessageId());
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                } else if (parts[0].startsWith(Commands.help) ||
                        (message.getText().startsWith(Commands.startCommand) || !message.isGroupMessage())) {
                    SendMessage sendMessageRequest = new SendMessage();
                    String formatedHelp = String.format(
                            LocalisationService.getInstance().getString("helpWeather", language),
                            Commands.WEATHERCOMMAND, Commands.CURRENTWEATHERCOMMAND);
                    sendMessageRequest.setText(formatedHelp);
                    sendMessageRequest.setChatId(message.getChatId());
                    SenderHelper.SendMessage(sendMessageRequest, TOKEN);
                }
            }
        } else if (message != null && message.hasLocation()) {
            String language = DatabaseManager.getInstance().getUserLanguage(update.getMessage().getFrom().getId());
            String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                    message.getLocation().getLatitude(), message.getFrom().getId(), language);
            SendMessage sendMessageRequest = new SendMessage();
            ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
            replyKeyboardHide.setSelective(true);
            replyKeyboardHide.setHideKeyboard(true);
            sendMessageRequest.setReplayMarkup(replyKeyboardHide);
            sendMessageRequest.setReplayToMessageId(update.getMessage().getMessageId());
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(message.getChatId());
            SenderHelper.SendMessage(sendMessageRequest, TOKEN);
        }
    }
}
