package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.Commands;
import org.telegram.SenderHelper;
import org.telegram.api.objects.*;
import org.telegram.database.DatabaseManager;
import org.telegram.api.methods.BotApiMethod;
import org.telegram.api.methods.SendMessage;
import org.telegram.services.*;
import org.telegram.structure.WeatherAlert;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Weather Bot
 * This is a use case that works with both Webhooks and GetUpdates methods
 * @date 24 of June of 2015
 */
public class WeatherHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENWEATHER;
    private static final String BOTNAME = BotConfig.USERNAMEWEATHER;
    private static final boolean USEWEBHOOK = true;

    private static final int STARTSTATE = 0;
    private static final int MAINMENU = 1;
    private static final int CURRENTWEATHER = 2;
    private static final int CURRENTNEWWEATHER = 3;
    private static final int CURRENTLOCATIONWEATHER = 4;
    private static final int FORECASTWEATHER = 5;
    private static final int FORECASTNEWWEATHER = 6;
    private static final int FORECASTLOCATIONWEATHER = 7;
    private static final int ALERT = 8;
    private static final int ALERTNEW = 9;
    private static final int ALERTDELETE = 10;
    private static final int SETTINGS = 11;
    private static final int LANGUAGE = 12;
    private static final int UNITS = 13;

    private final Object webhookLock = new Object();

    public WeatherHandlers(Webhook webhook) {
        if (USEWEBHOOK) {
            webhook.registerWebhook(this, BOTNAME);
            SenderHelper.SendWebhook(Webhook.getExternalURL(BOTNAME), TOKEN);
        } else {
            SenderHelper.SendWebhook("", TOKEN);
            new UpdatesThread(TOKEN, this);
        }
        startAlertTimers();
    }

    private static void startAlertTimers() {
        TimerExecutor.getInstance().startExecutionEveryDayAt(new CustomTimerTask("First day alert", -1) {
            @Override
            public void execute() {
                sendAlerts();
            }
        }, 0, 0, 0);

        TimerExecutor.getInstance().startExecutionEveryDayAt(new CustomTimerTask("Second day alert", -1) {
            @Override
            public void execute() {
                sendAlerts();
            }
        }, 12, 0, 0);
    }

    private static void sendAlerts() {
        List<WeatherAlert> allAlerts = DatabaseManager.getInstance().getAllAlerts();
        for (WeatherAlert weatherAlert : allAlerts) {
            String[] userOptions = DatabaseManager.getInstance().getUserWeatherOptions(weatherAlert.getUserId());
            String weather = WeatherService.getInstance().fetchWeatherAlert(weatherAlert.getCityId(),
                    weatherAlert.getUserId(), userOptions[0], userOptions[1]);
            SendMessage sendMessage = new SendMessage();
            sendMessage.enableMarkdown(true);
            sendMessage.setChatId(weatherAlert.getUserId());
            sendMessage.setText(weather);
            sendBuiltMessage(sendMessage);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null) {
            BotApiMethod botApiMethod = handleIncomingMessage(message);
            SenderHelper.SendApiMethod(botApiMethod, TOKEN);
        }
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null) {
            synchronized (webhookLock) {
                return handleIncomingMessage(message);
            }
        }
        return null;
    }

    private static BotApiMethod onCancelCommand(Integer chatId, Integer userId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("backToMainMenu", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessage;
    }

    // region Incoming messages handlers

    private static BotApiMethod handleIncomingMessage(Message message) {
        final int state = DatabaseManager.getInstance().getWeatherState(message.getFrom().getId(), message.getChatId());
        final String language = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[0];
        if (message.isGroupMessage() && message.hasText()) {
            if (isCommandForOther(message.getText())) {
                return null;
            } else if (message.getText().startsWith(Commands.STOPCOMMAND)){
                sendHideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                return null;
            }
        }
        BotApiMethod botApiMethod;
        switch(state) {
            case MAINMENU:
                botApiMethod = messageOnMainMenu(message, language);
                break;
            case CURRENTWEATHER:
            case CURRENTNEWWEATHER:
            case CURRENTLOCATIONWEATHER:
                botApiMethod = messageOnCurrentWeather(message, language, state);
                break;
            case FORECASTWEATHER:
            case FORECASTNEWWEATHER:
            case FORECASTLOCATIONWEATHER:
                botApiMethod = messageOnForecastWeather(message, language, state);
                break;
            case ALERT:
            case ALERTNEW:
            case ALERTDELETE:
                botApiMethod = messageOnAlert(message, language, state);
                break;
            case SETTINGS:
                botApiMethod = messageOnSetting(message, language);
                break;
            case LANGUAGE:
                botApiMethod = messageOnLanguage(message, language);
                break;
            case UNITS:
                botApiMethod = messageOnUnits(message, language);
                break;
            default:
                botApiMethod = sendMessageDefault(message, language);
                break;
        }
        return botApiMethod;
    }

    private static void sendHideKeyboard(Integer userId, Integer chatId, Integer messageId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setText(Emoji.WAVING_HAND_SIGN.toString());

        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setSelective(true);
        replyKeyboardHide.setHideKeyboard(true);
        sendMessage.setReplayMarkup(replyKeyboardHide);

        SenderHelper.SendApiMethod(sendMessage, TOKEN);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, STARTSTATE);

    }

    private static boolean isCommandForOther(String text) {
        boolean isSimpleCommand = text.equals("/start") || text.equals("/help") || text.equals("/stop");
        boolean isCommandForMe = text.equals("/start@weatherbot") || text.equals("/help@weatherbot") || text.equals("/stop@weatherbot");
        return text.startsWith("/") && !isSimpleCommand && !isCommandForMe;
    }

    // endregion Incoming messages handlers

    // region Alerts Menu Option selected

    private static BotApiMethod messageOnAlert(Message message, String language, int state) {
        BotApiMethod botApiMethod = null;
        switch(state) {
            case ALERT:
                botApiMethod = onAlertOptionSelected(message, language);
                break;
            case ALERTNEW:
                botApiMethod = onAlertNewOptionSelected(message, language);
                break;
            case ALERTDELETE:
                botApiMethod = onAlertDeleteOptionSelected(message, language);
                break;
        }
        return botApiMethod;
    }

    private static BotApiMethod onAlertDeleteOptionSelected(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                botApiMethod = onAlertDeleteBackOptionSelected(message, language);
            } else if (DatabaseManager.getInstance().getAlertCitiesNameByUser(message.getFrom().getId()).contains(message.getText())) {
                botApiMethod = onAlertDeleteCityOptionSelected(message, language);
            } else {
                botApiMethod = sendChooseOptionMessage(message.getChatId(), message.getMessageId(), getAlertsListKeyboard(message.getFrom().getId(), language), language);
            }
        }

        return botApiMethod;
    }

    private static BotApiMethod onAlertDeleteCityOptionSelected(Message message, String language) {
        DatabaseManager.getInstance().deleteAlertCity(message.getFrom().getId(), message.getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertDeleted", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static BotApiMethod onAlertDeleteBackOptionSelected(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static BotApiMethod onAlertNewOptionSelected(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.enableMarkdown(true);
                sendMessage.setChatId(message.getChatId());
                sendMessage.setReplayToMessageId(message.getMessageId());
                sendMessage.setReplayMarkup(getAlertsKeyboard(language));
                sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));
                DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
                botApiMethod = sendMessage;
            } else {
                botApiMethod = onNewAlertCityReceived(message, language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onNewAlertCityReceived(Message message, String language) {
        int userId = message.getFrom().getId();
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, message.getText());
        if (cityId != null) {
            DatabaseManager.getInstance().createNewWeatherAlert(userId, cityId, message.getText());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplayMarkup(getAlertsKeyboard(language));
            sendMessageRequest.setReplayToMessageId(message.getMessageId());
            sendMessageRequest.setText(getChooseNewAlertSetMessage(message.getText(), language));
            sendMessageRequest.setChatId(message.getChatId());

            DatabaseManager.getInstance().insertWeatherState(userId, message.getChatId(), ALERT);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getRecentsKeyboard(message.getFrom().getId(), language, false), language);
        }
    }

    private static BotApiMethod onAlertOptionSelected(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().equals(getNewCommand(language))) {
                botApiMethod = onNewAlertCommand(message, language);
            } else if (message.getText().equals(getDeleteCommand(language))) {
                botApiMethod = onDeleteAlertCommand(message, language);
            } else if (message.getText().equals(getListCommand(language))) {
                botApiMethod = onListAlertCommand(message, language);
            } else if (message.getText().equals(getBackCommand(language))) {
                botApiMethod = onBackAlertCommand(message, language);
            } else {
                botApiMethod = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getAlertsKeyboard(language), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onBackAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static BotApiMethod onListAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(getAlertListMessage(message.getFrom().getId(), language));
        sendMessage.setReplayToMessageId(message.getMessageId());

        return sendMessage;
    }

    private static BotApiMethod onDeleteAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());

        ReplyKeyboardMarkup replyKeyboardMarkup = getAlertsListKeyboard(message.getFrom().getId(), language);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
            sendMessage.setText(LocalisationService.getInstance().getString("chooseNewAlertCity", language));
            DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTDELETE);
        } else {
            sendMessage.setReplayMarkup(getAlertsKeyboard(language));
            sendMessage.setText(LocalisationService.getInstance().getString("noAlertList", language));
        }

        sendMessage.setReplayToMessageId(message.getMessageId());
        return sendMessage;
    }

    private static SendMessage onNewAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getRecentsKeyboard(message.getFrom().getId(), language, false));
        sendMessage.setText(LocalisationService.getInstance().getString("chooseNewAlertCity", language));
        sendMessage.setReplayToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTNEW);
        return sendMessage;
    }

    // endregion Alerts Menu Option selected

    // region Settings Menu Option selected

    private static BotApiMethod messageOnSetting(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getLanguagesCommand(language))) {
                botApiMethod = onLanguageCommand(message, language);
            } else if (message.getText().startsWith(getUnitsCommand(language))) {
                botApiMethod = onUnitsCommand(message, language);
            } else if (message.getText().startsWith(getAlertsCommand(language))) {
                botApiMethod = onAlertsCommand(message, language);
            } else if (message.getText().startsWith(getBackCommand(language))) {
                botApiMethod = sendMessageDefault(message, language);
            } else {
                botApiMethod = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getSettingsKeyboard(language), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onAlertsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static BotApiMethod onUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getUnitsKeyboard(language));
        sendMessage.setText(getUnitsMessage(message.getFrom().getId(), language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), UNITS);
        return sendMessage;
    }

    private static BotApiMethod onLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getLanguagesKeyboard(language));
        sendMessage.setText(getLanguageMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), LANGUAGE);
        return sendMessage;
    }

    // endregion Settings Menu Option selected

    // region Units Menu Option selected

    private static BotApiMethod messageOnUnits(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                botApiMethod = onBackUnitsCommand(message, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("metricSystem", language))) {
                botApiMethod = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.METRICSYSTEM, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("imperialSystem", language))) {
                botApiMethod = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.IMPERIALSYSTEM, language);
            } else {
                botApiMethod = onUnitsError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onBackUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static BotApiMethod onUnitsError(Integer chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setReplayMarkup(getUnitsKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorUnitsNotFound", language));
        sendMessageRequest.setReplayToMessageId(messageId);

        return sendMessageRequest;
    }

    private static BotApiMethod onUnitsChosen(Integer userId, Integer chatId, Integer messageId, String units, String language) {
        DatabaseManager.getInstance().putUserWeatherUnitsOption(userId, units);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("unitsUpdated", language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Units Menu Option selected

    // region Language Menu Option selected

    private static BotApiMethod messageOnLanguage(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                botApiMethod = onBackLanguageCommand(message, language);
            } else if (LocalisationService.getInstance().getSupportedLanguages().values().contains(message.getText().trim())) {
                botApiMethod = onLanguageChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), message.getText().trim());
            } else {
                botApiMethod = onLanguageError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onBackLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static BotApiMethod onLanguageError(Integer chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setReplayMarkup(getLanguagesKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorLanguageNotFound", language));
        sendMessageRequest.setReplayToMessageId(messageId);

        return sendMessageRequest;
    }

    private static BotApiMethod onLanguageChosen(Integer userId, Integer chatId, Integer messageId, String language) {
        String languageCode = LocalisationService.getInstance().getLanguageCodeByName(language);
        DatabaseManager.getInstance().putUserWeatherLanguageOption(userId, languageCode);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("languageUpdated", languageCode));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(languageCode));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Language Menu Option selected

    // region Forecast Weather Menu Option selected

    private static BotApiMethod messageOnForecastWeather(Message message, String language, int state) {
        BotApiMethod botApiMethod = null;
        switch(state) {
            case FORECASTWEATHER:
                botApiMethod = onForecastWeather(message, language);
                break;
            case FORECASTNEWWEATHER:
                botApiMethod = onForecastNewWeather(message, language);
                break;
            case FORECASTLOCATIONWEATHER:
                botApiMethod = onForecastWeatherLocation(message, language);
                break;
        }
        return botApiMethod;
    }

    private static BotApiMethod onForecastWeather(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                botApiMethod = onNewForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                botApiMethod = onLocationForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                botApiMethod = onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                botApiMethod = onForecastWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onForecastNewWeather(Message message, String language) {
        if (message.isReply()) {
            return onForecastWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    private static BotApiMethod onForecastWeatherCityReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherForecast(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplayToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId);

            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static BotApiMethod onLocationForecastWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTLOCATIONWEATHER);
        return sendMessage;
    }

    private static BotApiMethod onNewForecastWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTNEWWEATHER);
        return sendMessage;
    }

    private static BotApiMethod onForecastWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            return onForecastWeatherLocationReceived(message, language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    // endregion Forecast Weather Menu Option selected

    // region Current Weather Menu Option selected

    private static BotApiMethod messageOnCurrentWeather(Message message, String language, int state) {
        BotApiMethod botApiMethod = null;
        switch(state) {
            case CURRENTWEATHER:
                botApiMethod = onCurrentWeather(message, language);
                break;
            case CURRENTNEWWEATHER:
                botApiMethod = onCurrentNewWeather(message, language);
                break;
            case CURRENTLOCATIONWEATHER:
                botApiMethod = onCurrentWeatherLocation(message, language);
                break;
        }

        return botApiMethod;
    }

    private static BotApiMethod onCurrentWeather(Message message, String language) {
        BotApiMethod botApiMethod = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                botApiMethod = onNewCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                botApiMethod = onLocationCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                botApiMethod = onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                botApiMethod = onCurrentWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
        return botApiMethod;
    }

    private static BotApiMethod onCurrentNewWeather(Message message, String language) {
        if (message.isReply()) {
            return onCurrentWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    private static BotApiMethod onCurrentWeatherCityReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherCurrent(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplayToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId);
            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static BotApiMethod onLocationCurrentWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTLOCATIONWEATHER);
        return sendMessage;
    }

    private static BotApiMethod onNewCurrentWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTNEWWEATHER);
        return sendMessage;
    }

    private static BotApiMethod onCurrentWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            return onCurrentWeatherLocationReceived(message, language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    // endregion Current Weather Menu Option selected

    // region Main menu options selected

    private static BotApiMethod messageOnMainMenu(Message message, String language) {
        BotApiMethod botApiMethod;
        if (message.hasText()) {
            if (message.getText().equals(getCurrentCommand(language))) {
                botApiMethod = onCurrentChoosen(message, language);
            } else if (message.getText().equals(getForecastCommand(language))) {
                botApiMethod = onForecastChoosen(message, language);
            } else if (message.getText().equals(getSettingsCommand(language))) {
                botApiMethod = onSettingsChoosen(message, language);
            } else if (message.getText().equals(getRateCommand(language))) {
                botApiMethod = sendRateMessage(message.getChatId(), message.getMessageId(), null, language);
            } else {
                botApiMethod = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            }
        } else {
            botApiMethod = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getMainMenuKeyboard(language), language);
        }

        return botApiMethod;
    }

    private static BotApiMethod onSettingsChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static BotApiMethod onForecastChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandFromHistory", language));
        } else {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandWithoutHistory", language));
        }

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), FORECASTWEATHER);
        return sendMessage;
    }

    private static BotApiMethod onCurrentChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessage.setText(LocalisationService.getInstance().getString("onCurrentCommandFromHistory", language));
        } else {
            sendMessage.setText(LocalisationService.getInstance().getString("onCurrentCommandWithoutHistory", language));
        }

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), CURRENTWEATHER);
        return sendMessage;
    }

    // endregion Main menu options selected

    // region Get Messages

    private static String getSettingsMessage(String language) {
        String baseString = LocalisationService.getInstance().getString("onSettingsCommand", language);
        return String.format(baseString, Emoji.GLOBE_WITH_MERIDIANS.toString(),
                Emoji.STRAIGHT_RULER.toString(), Emoji.ALARM_CLOCK.toString(),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getHelpMessage(String language) {
        String baseString = LocalisationService.getInstance().getString("helpWeatherMessage", language);
        return String.format(baseString, Emoji.BLACK_RIGHT_POINTING_TRIANGLE.toString(),
                Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString(), Emoji.ALARM_CLOCK.toString(),
                Emoji.EARTH_GLOBE_EUROPE_AFRICA.toString(), Emoji.STRAIGHT_RULER.toString());
    }

    private static String getLanguageMessage(String language) {
        String baseString = LocalisationService.getInstance().getString("selectLanguage", language);
        return String.format(baseString, language);
    }

    private static String getUnitsMessage(Integer userId, String language) {
        String baseString = LocalisationService.getInstance().getString("selectUnits", language);
        return String.format(baseString, DatabaseManager.getInstance().getUserWeatherOptions(userId)[1]);
    }

    private static String getChooseNewAlertSetMessage(String city, String language) {
        String baseString = LocalisationService.getInstance().getString("newAlertSaved", language);
        return String.format(baseString, Emoji.THUMBS_UP_SIGN.toString(), city);
    }

    private static String getAlertListMessage(int userId, String language) {
        String alertListMessage;

        List<String> alertCities = DatabaseManager.getInstance().getAlertCitiesNameByUser(userId);
        if (alertCities.size() > 0) {
            String baseAlertListString = LocalisationService.getInstance().getString("initialAlertList", language);
            String partialAlertListString = LocalisationService.getInstance().getString("partialAlertList", language);
            String fullListOfAlerts = "";
            for (String alertCity : alertCities) {
                fullListOfAlerts += String.format(partialAlertListString, Emoji.ALARM_CLOCK.toString(), alertCity);
            }
            alertListMessage = String.format(baseAlertListString, alertCities.size(), fullListOfAlerts);
        } else {
            alertListMessage = LocalisationService.getInstance().getString("noAlertList", language);
        }

        return alertListMessage;
    }


    // endregion Get Messages

    // region ReplyKeyboards

    private static ReplyKeyboardMarkup getMainMenuKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<List<String>> keyboard = new ArrayList<>();
        List<String> keyboardFirstRow = new ArrayList<>();
        keyboardFirstRow.add(getCurrentCommand(language));
        keyboardFirstRow.add(getForecastCommand(language));
        List<String> keyboardSecondRow = new ArrayList<>();
        keyboardSecondRow.add(getSettingsCommand(language));
        keyboardSecondRow.add(getRateCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getLanguagesKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<List<String>> keyboard = new ArrayList<>();
        for (String languageName : LocalisationService.getInstance().getSupportedLanguages().values()) {
            List<String> row = new ArrayList<>();
            row.add(languageName);
            keyboard.add(row);
        }

        List<String> row = new ArrayList<>();
        row.add(getCancelCommand(language));
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getUnitsKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<List<String>> keyboard = new ArrayList<>();
        List<String> row = new ArrayList<>();
        row.add(LocalisationService.getInstance().getString("metricSystem", language));
        keyboard.add(row);
        row = new ArrayList<>();
        row.add(LocalisationService.getInstance().getString("imperialSystem", language));
        keyboard.add(row);
        row = new ArrayList<>();
        row.add(getCancelCommand(language));
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getSettingsKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<List<String>> keyboard = new ArrayList<>();
        List<String> keyboardFirstRow = new ArrayList<>();
        keyboardFirstRow.add(getLanguagesCommand(language));
        keyboardFirstRow.add(getUnitsCommand(language));
        List<String> keyboardSecondRow = new ArrayList<>();
        keyboardSecondRow.add(getAlertsCommand(language));
        keyboardSecondRow.add(getBackCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getRecentsKeyboard(Integer userId, String language) {
        return getRecentsKeyboard(userId, language, true);
    }

    private static ReplyKeyboardMarkup getRecentsKeyboard(Integer userId, String language, boolean allowNew) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(true);

        List<List<String>> keyboard = new ArrayList<>();
        for (String recentWeather : DatabaseManager.getInstance().getRecentWeather(userId)) {
            List<String> row = new ArrayList<>();
            row.add(recentWeather);
            keyboard.add(row);
        }

        List<String> row = new ArrayList<>();
        if (allowNew) {
            row.add(getLocationCommand(language));
            keyboard.add(row);

            row = new ArrayList<>();
            row.add(getNewCommand(language));
            keyboard.add(row);

            row = new ArrayList<>();
        }
        row.add(getCancelCommand(language));
        keyboard.add(row);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAlertsKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(true);

        List<List<String>> keyboard = new ArrayList<>();

        List<String> row = new ArrayList<>();
        row.add(getNewCommand(language));
        row.add(getDeleteCommand(language));
        keyboard.add(row);

        row = new ArrayList<>();
        row.add(getListCommand(language));
        row.add(getBackCommand(language));
        keyboard.add(row);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAlertsListKeyboard(Integer userId, String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = null;

        List<String> alertCitiesNames = DatabaseManager.getInstance().getAlertCitiesNameByUser(userId);
        if (alertCitiesNames.size() > 0) {
            replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setSelective(true);
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboad(true);

            List<List<String>> keyboard = new ArrayList<>();
            for (String alertCityName: alertCitiesNames) {
                List<String> row = new ArrayList<>();
                row.add(alertCityName);
                keyboard.add(row);
            }
            List<String> row = new ArrayList<>();
            row.add(getCancelCommand(language));
            keyboard.add(row);

            replyKeyboardMarkup.setKeyboard(keyboard);
        }

        return replyKeyboardMarkup;
    }

    private static ForceReplyKeyboard getForceReply() {
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setForceReply(true);
        forceReplyKeyboard.setSelective(true);
        return forceReplyKeyboard;
    }

    // endregion ReplyKeyboards

    // region getCommnads

    private static String getRateCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("rateMe", language),
                Emoji.HUNDRED_POINTS_SYMBOL.toString());
    }

    private static String getListCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("showList", language),
                Emoji.CLIPBOARD.toString());
    }

    private static String getDeleteCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("delete", language),
                Emoji.HEAVY_MINUS_SIGN.toString());
    }

    private static String getLanguagesCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("languages", language),
                Emoji.GLOBE_WITH_MERIDIANS.toString());
    }

    private static String getUnitsCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("units", language),
                Emoji.STRAIGHT_RULER.toString());
    }

    private static String getAlertsCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("alerts", language),
                Emoji.ALARM_CLOCK.toString());
    }

    private static String getBackCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("back", language),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getNewCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("new", language),
                Emoji.HEAVY_PLUS_SIGN.toString());
    }

    private static String getLocationCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("location", language),
                Emoji.ROUND_PUSHPIN.toString());
    }

    private static String getSettingsCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("settings", language),
                Emoji.WRENCH.toString());
    }

    private static String getCurrentCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("current", language),
                Emoji.BLACK_RIGHT_POINTING_TRIANGLE.toString());
    }

    private static String getForecastCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("forecast", language),
                Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString());
    }

    private static String getCancelCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("cancel", language),
                Emoji.CROSS_MARK.toString());
    }
    // endregion getCommnads

    // region Send common messages

    private static BotApiMethod sendMessageDefault(Message message, String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getMainMenuKeyboard(language);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendHelpMessage(message.getChatId(), message.getMessageId(), replyKeyboardMarkup, language);
    }

    private static BotApiMethod sendChooseOptionMessage(Integer chatId, Integer messageId,
                                                ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("chooseOption", language));

        return sendMessage;
    }

    private static BotApiMethod sendHelpMessage(Integer chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(getHelpMessage(language));
        return sendMessage;
    }

    private static BotApiMethod sendRateMessage(Integer chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(LocalisationService.getInstance().getString("rateMeMessage", language));

        return sendMessage;
    }

    // endregion Send common messages

    // region Send weather

    private static BotApiMethod onForecastWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static BotApiMethod onForecastWeatherReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherForecast(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    private static BotApiMethod onCurrentWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrentByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static BotApiMethod onCurrentWeatherReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrent(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Send weather

    // region Helper Methods

    private static void sendBuiltMessage(SendMessage sendMessage) {
        SenderHelper.SendApiMethod(sendMessage, TOKEN);
    }

    // endregion Helper Methods
}
