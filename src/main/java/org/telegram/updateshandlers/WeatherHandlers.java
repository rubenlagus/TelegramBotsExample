package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.SenderHelper;
import org.telegram.api.*;
import org.telegram.database.DatabaseManager;
import org.telegram.methods.SendMessage;
import org.telegram.services.*;
import org.telegram.structure.WeatherAlert;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Weather Bot
 * @date 24 of June of 2015
 */
public class WeatherHandlers implements UpdatesCallback {
    private static final String TOKEN = BotConfig.TOKENWEATHER;
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
    private static final String webhookPath = "weatherBot";

    public WeatherHandlers() {
        Webhook webhook;
        if (BuildVars.useWebHook) {
            webhook = new Webhook(this, webhookPath);
            SenderHelper.SendWebhook(webhook.getURL(), TOKEN);
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
            sendMessage.setChatId(weatherAlert.getUserId());
            sendMessage.setText(weather);
            sendBuiltMessage(sendMessage);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null) {
            handleIncomingMessage(message);
        }
    }

    private static void onCancelCommand(Integer chatId, Integer userId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("backToMainMenu", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
    }

    // region Incoming messages handlers

    private static void handleIncomingMessage(Message message) {
        final int state = DatabaseManager.getInstance().getWeatherState(message.getFrom().getId(), message.getChatId());
        final String language = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[0];
        if (message.isGroupMessage() && message.hasText() && isCommandForOther(message.getText())) {
            return;
        }
        switch(state) {
            case MAINMENU:
                messageOnMainMenu(message, language);
                break;
            case CURRENTWEATHER:
            case CURRENTNEWWEATHER:
            case CURRENTLOCATIONWEATHER:
                messageOnCurrentWeather(message, language, state);
                break;
            case FORECASTWEATHER:
            case FORECASTNEWWEATHER:
            case FORECASTLOCATIONWEATHER:
                messageOnForecastWeather(message, language, state);
                break;
            case ALERT:
            case ALERTNEW:
            case ALERTDELETE:
                messageOnAlert(message, language, state);
                break;
            case SETTINGS:
                messageOnSetting(message, language);
                break;
            case LANGUAGE:
                messageOnLanguage(message, language);
                break;
            case UNITS:
                messageOnUnits(message, language);
                break;
            default:
                sendMessageDefault(message, language);
                break;
        }
    }

    private static boolean isCommandForOther(String text) {
        boolean isSimpleCommand = text.equals("/start") || text.equals("/help");
        boolean isCommandForMe = text.equals("/start@weatherbot") || text.equals("/help@weatherbot");
        return !isSimpleCommand && !isCommandForMe;
    }

    // endregion Incoming messages handlers

    // region Alerts Menu Option selected

    private static void messageOnAlert(Message message, String language, int state) {
        switch(state) {
            case ALERT:
                onAlertOptionSelected(message, language);
                break;
            case ALERTNEW:
                onAlertNewOptionSelected(message, language);
                break;
            case ALERTDELETE:
                onAlertDeleteOptionSelected(message, language);
                break;
        }
    }

    private static void onAlertDeleteOptionSelected(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                onAlertDeleteBackOptionSelected(message, language);
            } else if (DatabaseManager.getInstance().getAlertCitiesNameByUser(message.getFrom().getId()).contains(message.getText())) {
                onAlertDeleteCityOptionSelected(message, language);
            } else {
                sendChooseOptionMessage(message.getChatId(), message.getMessageId(), getAlertsListKeyboard(message.getFrom().getId(), language), language);
            }
        }
    }

    private static void onAlertDeleteCityOptionSelected(Message message, String language) {
        DatabaseManager.getInstance().deleteAlertCity(message.getFrom().getId(), message.getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertDeleted", language));
        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
    }

    private static void onAlertDeleteBackOptionSelected(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));
        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
    }

    private static void onAlertNewOptionSelected(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                sendMessage.setReplayToMessageId(message.getMessageId());
                sendMessage.setReplayMarkup(getAlertsKeyboard(language));
                sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));
                sendBuiltMessage(sendMessage);
                DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
            } else {
                onNewAlertCityReceived(message, language);
            }
        }
    }

    private static void onNewAlertCityReceived(Message message, String language) {
        int userId = message.getFrom().getId();
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, message.getText());
        if (cityId != null) {
            DatabaseManager.getInstance().createNewWeatherAlert(userId, cityId, message.getText());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setReplayMarkup(getAlertsKeyboard(language));
            sendMessageRequest.setReplayToMessageId(message.getMessageId());
            sendMessageRequest.setText(getChooseNewAlertSetMessage(message.getText(), language));
            sendMessageRequest.setChatId(message.getChatId());
            sendBuiltMessage(sendMessageRequest);
            DatabaseManager.getInstance().insertWeatherState(userId, message.getChatId(), ALERT);
        } else {
            sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getRecentsKeyboard(message.getFrom().getId(), language, false), language);
        }
    }

    private static void onAlertOptionSelected(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().equals(getNewCommand(language))) {
                onNewAlertCommand(message, language);
            } else if (message.getText().equals(getDeleteCommand(language))) {
                onDeleteAlertCommand(message, language);
            } else if (message.getText().equals(getListCommand(language))) {
                onListAlertCommand(message, language);
            } else if (message.getText().equals(getBackCommand(language))) {
                onBackAlertCommand(message, language);
            } else {
                sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getAlertsKeyboard(language), language);
            }
        }
    }

    private static void onBackAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
    }

    private static void onListAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(getAlertListMessage(message.getFrom().getId(), language));
        sendMessage.setReplayToMessageId(message.getMessageId());

        sendBuiltMessage(sendMessage);
    }

    private static void onDeleteAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

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
        sendBuiltMessage(sendMessage);
    }

    private static void onNewAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getRecentsKeyboard(message.getFrom().getId(), language, false));
        sendMessage.setText(LocalisationService.getInstance().getString("chooseNewAlertCity", language));
        sendMessage.setReplayToMessageId(message.getMessageId());

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTNEW);
    }

    // endregion Alerts Menu Option selected

    // region Settings Menu Option selected

    private static void messageOnSetting(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().startsWith(getLanguagesCommand(language))) {
                onLanguageCommand(message, language);
            } else if (message.getText().startsWith(getUnitsCommand(language))) {
                onUnitsCommand(message, language);
            } else if (message.getText().startsWith(getAlertsCommand(language))) {
                onAlertsCommand(message, language);
            } else if (message.getText().startsWith(getBackCommand(language))) {
                sendMessageDefault(message, language);
            } else {
                sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getSettingsKeyboard(language), language);
            }
        }
    }

    private static void onAlertsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
    }

    private static void onUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getUnitsKeyboard(language));
        sendMessage.setText(getUnitsMessage(message.getFrom().getId(), language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), UNITS);
    }

    private static void onLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplayMarkup(getLanguagesKeyboard(language));
        sendMessage.setText(getLanguageMessage(language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), LANGUAGE);
    }

    // endregion Settings Menu Option selected

    // region Units Menu Option selected

    private static void messageOnUnits(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                onBackUnitsCommand(message, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("metricSystem", language))) {
                onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.METRICSYSTEM, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("imperialSystem", language))) {
                onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.IMPERIALSYSTEM, language);
            } else {
                onUnitsError(message.getChatId(), message.getMessageId(), language);
            }
        }
    }

    private static void onBackUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
    }

    private static void onUnitsError(Integer chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setReplayMarkup(getUnitsKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorUnitsNotFound", language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendBuiltMessage(sendMessageRequest);
    }

    private static void onUnitsChosen(Integer userId, Integer chatId, Integer messageId, String units, String language) {
        DatabaseManager.getInstance().putUserWeatherUnitsOption(userId, units);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("unitsUpdated", language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendBuiltMessage(sendMessageRequest);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
    }

    // endregion Units Menu Option selected

    // region Language Menu Option selected

    private static void messageOnLanguage(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                onBackLanguageCommand(message, language);
            } else if (LocalisationService.getInstance().getSupportedLanguages().values().contains(message.getText().trim())) {
                onLanguageChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), message.getText().trim());
            } else {
                onLanguageError(message.getChatId(), message.getMessageId(), language);
            }
        }
    }

    private static void onBackLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
    }

    private static void onLanguageError(Integer chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setReplayMarkup(getLanguagesKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorLanguageNotFound", language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendBuiltMessage(sendMessageRequest);
    }

    private static void onLanguageChosen(Integer userId, Integer chatId, Integer messageId, String language) {
        String languageCode = LocalisationService.getInstance().getLanguageCodeByName(language);
        DatabaseManager.getInstance().putUserWeatherLanguageOption(userId, languageCode);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(LocalisationService.getInstance().getString("languageUpdated", languageCode));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(languageCode));
        sendBuiltMessage(sendMessageRequest);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
    }

    // endregion Language Menu Option selected

    // region Forecast Weather Menu Option selected

    private static void messageOnForecastWeather(Message message, String language, int state) {
        switch(state) {
            case FORECASTWEATHER:
                onForecastWeather(message, language);
                break;
            case FORECASTNEWWEATHER:
                onForecastNewWeather(message, language);
                break;
            case FORECASTLOCATIONWEATHER:
                onForecastWeatherLocation(message, language);
                break;
        }
    }

    private static void onForecastWeather(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                onNewForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                onLocationForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                onForecastWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
    }

    private static void onForecastNewWeather(Message message, String language) {
        if (message.isReply()) {
            onForecastWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            sendMessageDefault(message, language);
        }
    }

    private static void onForecastWeatherCityReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherForecast(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplayToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId);
            sendBuiltMessage(sendMessageRequest);
            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        } else {
            sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static void onLocationForecastWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTLOCATIONWEATHER);
    }

    private static void onNewForecastWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTNEWWEATHER);
    }

    private static void onForecastWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            onForecastWeatherLocationReceived(message, language);
        } else {
            sendMessageDefault(message, language);
        }
    }

    // endregion Forecast Weather Menu Option selected

    // region Current Weather Menu Option selected

    private static void messageOnCurrentWeather(Message message, String language, int state) {
        switch(state) {
            case CURRENTWEATHER:
                onCurrentWeather(message, language);
                break;
            case CURRENTNEWWEATHER:
                onCurrentNewWeather(message, language);
                break;
            case CURRENTLOCATIONWEATHER:
                onCurrentWeatherLocation(message, language);
                break;
        }
    }

    private static void onCurrentWeather(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                onNewCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                onLocationCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                onCurrentWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
    }

    private static void onCurrentNewWeather(Message message, String language) {
        if (message.isReply()) {
            onCurrentWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            sendMessageDefault(message, language);
        }
    }

    private static void onCurrentWeatherCityReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherCurrent(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplayToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId);
            sendBuiltMessage(sendMessageRequest);
            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        } else {
            sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static void onLocationCurrentWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTLOCATIONWEATHER);
    }

    private static void onNewCurrentWeatherCommand(Integer chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTNEWWEATHER);
    }

    private static void onCurrentWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            onCurrentWeatherLocationReceived(message, language);
        } else {
            sendMessageDefault(message, language);
        }
    }

    // endregion Current Weather Menu Option selected

    // region Main menu options selected

    private static void messageOnMainMenu(Message message, String language) {
        if (message.hasText()) {
            if (message.getText().equals(getCurrentCommand(language))) {
                onCurrentChoosen(message, language);
            } else if (message.getText().equals(getForecastCommand(language))) {
                onForecastChoosen(message, language);
            } else if (message.getText().equals(getSettingsCommand(language))) {
                onSettingsChoosen(message, language);
            } else if (message.getText().equals(getRateCommand(language))) {
                sendRateMessage(message.getChatId(), message.getMessageId(), null, language);
            } else {
                sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            }
        } else {
            sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getMainMenuKeyboard(language), language);
        }
    }

    private static void onSettingsChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
    }

    private static void onForecastChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandFromHistory", language));
        } else {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandWithoutHistory", language));
        }

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), FORECASTWEATHER);
    }

    private static void onCurrentChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplayMarkup(replyKeyboardMarkup);
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessage.setText(LocalisationService.getInstance().getString("onCurrentCommandFromHistory", language));
        } else {
            sendMessage.setText(LocalisationService.getInstance().getString("onCurrentCommandWithoutHistory", language));
        }

        sendBuiltMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), CURRENTWEATHER);
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

    private static String getHelpCommand(String language) {
        return String.format(LocalisationService.getInstance().getString("help", language),
                Emoji.BLACK_QUESTION_MARK_ORNAMENT.toString());
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

    private static void sendMessageDefault(Message message, String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getMainMenuKeyboard(language);
        sendHelpMessage(message.getChatId(), message.getMessageId(), replyKeyboardMarkup, language);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
    }

    private static void sendChooseOptionMessage(Integer chatId, Integer messageId,
                                                ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        sendMessage.setReplayMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("chooseOption", language));
        sendBuiltMessage(sendMessage);
    }

    private static void sendHelpMessage(Integer chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(getHelpMessage(language));
        sendBuiltMessage(sendMessage);
    }

    private static void sendRateMessage(Integer chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplayToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplayMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(LocalisationService.getInstance().getString("rateMeMessage", language));
        sendBuiltMessage(sendMessage);
    }

    // endregion Send common messages

    // region Send weather

    private static void onForecastWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());
        sendBuiltMessage(sendMessageRequest);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
    }

    private static void onForecastWeatherReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherForecast(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId);
        sendBuiltMessage(sendMessageRequest);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
    }

    private static void onCurrentWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrentByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());
        sendBuiltMessage(sendMessageRequest);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
    }

    private static void onCurrentWeatherReceived(Integer chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrent(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setReplayMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplayToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId);
        sendBuiltMessage(sendMessageRequest);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
    }

    // endregion Send weather

    // region Helper Methods

    private static void sendBuiltMessage(SendMessage sendMessage) {
        SenderHelper.SendMessage(sendMessage, TOKEN);
    }

    // endregion Helper Methods
}
