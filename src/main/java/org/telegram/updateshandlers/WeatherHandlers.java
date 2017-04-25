package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.CustomTimerTask;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.services.TimerExecutor;
import org.telegram.services.WeatherService;
import org.telegram.structure.WeatherAlert;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for updates to Weather Bot
 * @date 24 of June of 2015
 */
public class WeatherHandlers extends TelegramLongPollingBot {
    private static final String LOGTAG = "WEATHERHANDLERS";

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

    public WeatherHandlers() {
        super();
        startAlertTimers();
    }

    @Override
    public String getBotToken() {
        return BotConfig.WEATHER_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    handleIncomingMessage(message);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.WEATHER_USER;
    }

    private void startAlertTimers() {
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

    private void sendAlerts() {
        List<WeatherAlert> allAlerts = DatabaseManager.getInstance().getAllAlerts();
        for (WeatherAlert weatherAlert : allAlerts) {
            synchronized (Thread.currentThread()) {
                try {
                    Thread.currentThread().wait(35);
                } catch (InterruptedException e) {
                    BotLogger.severe(LOGTAG, e);
                }
            }
            String[] userOptions = DatabaseManager.getInstance().getUserWeatherOptions(weatherAlert.getUserId());
            String weather = WeatherService.getInstance().fetchWeatherAlert(weatherAlert.getCityId(),
                    weatherAlert.getUserId(), userOptions[0], userOptions[1]);
            SendMessage sendMessage = new SendMessage();
            sendMessage.enableMarkdown(true);
            sendMessage.setChatId(String.valueOf(weatherAlert.getUserId()));
            sendMessage.setText(weather);
            try {
                sendMessage(sendMessage);
            } catch (TelegramApiRequestException e) {
                BotLogger.warn(LOGTAG, e);
                if (e.getApiResponse().contains("Can't access the chat") || e.getApiResponse().contains("Bot was blocked by the user")) {
                    DatabaseManager.getInstance().deleteAlertsForUser(weatherAlert.getUserId());
                }
            } catch (Exception e) {
                BotLogger.severe(LOGTAG, e);
            }
        }
    }

    private static SendMessage onCancelCommand(Long chatId, Integer userId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("backToMainMenu", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessage;
    }

    // region Incoming messages handlers

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        final int state = DatabaseManager.getInstance().getWeatherState(message.getFrom().getId(), message.getChatId());
        final String language = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[0];
        if (!message.isUserMessage() && message.hasText()) {
            if (isCommandForOther(message.getText())) {
                return;
            } else if (message.getText().startsWith(Commands.STOPCOMMAND)){
                sendHideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                return;
            }
        }
        SendMessage sendMessageRequest;
        switch(state) {
            case MAINMENU:
                sendMessageRequest = messageOnMainMenu(message, language);
                break;
            case CURRENTWEATHER:
            case CURRENTNEWWEATHER:
            case CURRENTLOCATIONWEATHER:
                sendMessageRequest = messageOnCurrentWeather(message, language, state);
                break;
            case FORECASTWEATHER:
            case FORECASTNEWWEATHER:
            case FORECASTLOCATIONWEATHER:
                sendMessageRequest = messageOnForecastWeather(message, language, state);
                break;
            case ALERT:
            case ALERTNEW:
            case ALERTDELETE:
                sendMessageRequest = messageOnAlert(message, language, state);
                break;
            case SETTINGS:
                sendMessageRequest = messageOnSetting(message, language);
                break;
            case LANGUAGE:
                sendMessageRequest = messageOnLanguage(message, language);
                break;
            case UNITS:
                sendMessageRequest = messageOnUnits(message, language);
                break;
            default:
                sendMessageRequest = sendMessageDefault(message, language);
                break;
        }

            sendMessage(sendMessageRequest);
    }

    private void sendHideKeyboard(Integer userId, Long chatId, Integer messageId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setText(Emoji.WAVING_HAND_SIGN.toString());

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        sendMessage.setReplyMarkup(replyKeyboardHide);

        sendMessage(sendMessage);
        DatabaseManager.getInstance().insertWeatherState(userId, chatId, STARTSTATE);
    }

    private static boolean isCommandForOther(String text) {
        boolean isSimpleCommand = text.equals("/start") || text.equals("/help") || text.equals("/stop");
        boolean isCommandForMe = text.equals("/start@weatherbot") || text.equals("/help@weatherbot") || text.equals("/stop@weatherbot");
        return text.startsWith("/") && !isSimpleCommand && !isCommandForMe;
    }

    // endregion Incoming messages handlers

    // region Alerts Menu Option selected

    private static SendMessage messageOnAlert(Message message, String language, int state) {
        SendMessage sendMessageRequest = null;
        switch(state) {
            case ALERT:
                sendMessageRequest = onAlertOptionSelected(message, language);
                break;
            case ALERTNEW:
                sendMessageRequest = onAlertNewOptionSelected(message, language);
                break;
            case ALERTDELETE:
                sendMessageRequest = onAlertDeleteOptionSelected(message, language);
                break;
        }
        return sendMessageRequest;
    }

    private static SendMessage onAlertDeleteOptionSelected(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                sendMessageRequest = onAlertDeleteBackOptionSelected(message, language);
            } else if (DatabaseManager.getInstance().getAlertCitiesNameByUser(message.getFrom().getId()).contains(message.getText())) {
                sendMessageRequest = onAlertDeleteCityOptionSelected(message, language);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(), getAlertsListKeyboard(message.getFrom().getId(), language), language);
            }
        }

        return sendMessageRequest;
    }

    private static SendMessage onAlertDeleteCityOptionSelected(Message message, String language) {
        DatabaseManager.getInstance().deleteAlertCity(message.getFrom().getId(), message.getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertDeleted", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onAlertDeleteBackOptionSelected(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onAlertNewOptionSelected(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.enableMarkdown(true);
                sendMessage.setChatId(message.getChatId());
                sendMessage.setReplyToMessageId(message.getMessageId());
                sendMessage.setReplyMarkup(getAlertsKeyboard(language));
                sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));
                DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
                sendMessageRequest = sendMessage;
            } else {
                sendMessageRequest = onNewAlertCityReceived(message, language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onNewAlertCityReceived(Message message, String language) {
        int userId = message.getFrom().getId();
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, message.getText());
        if (cityId != null) {
            DatabaseManager.getInstance().createNewWeatherAlert(userId, cityId, message.getText());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getAlertsKeyboard(language));
            sendMessageRequest.setReplyToMessageId(message.getMessageId());
            sendMessageRequest.setText(getChooseNewAlertSetMessage(message.getText(), language));
            sendMessageRequest.setChatId(message.getChatId());

            DatabaseManager.getInstance().insertWeatherState(userId, message.getChatId(), ALERT);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getRecentsKeyboard(message.getFrom().getId(), language, false), language);
        }
    }

    private static SendMessage onAlertOptionSelected(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().equals(getNewCommand(language))) {
                sendMessageRequest = onNewAlertCommand(message, language);
            } else if (message.getText().equals(getDeleteCommand(language))) {
                sendMessageRequest = onDeleteAlertCommand(message, language);
            } else if (message.getText().equals(getListCommand(language))) {
                sendMessageRequest = onListAlertCommand(message, language);
            } else if (message.getText().equals(getBackCommand(language))) {
                sendMessageRequest = onBackAlertCommand(message, language);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getAlertsKeyboard(language), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onBackAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onListAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));
        sendMessage.setText(getAlertListMessage(message.getFrom().getId(), language));
        sendMessage.setReplyToMessageId(message.getMessageId());

        return sendMessage;
    }

    private static SendMessage onDeleteAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());

        ReplyKeyboardMarkup replyKeyboardMarkup = getAlertsListKeyboard(message.getFrom().getId(), language);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
            sendMessage.setText(LocalisationService.getInstance().getString("chooseNewAlertCity", language));
            DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTDELETE);
        } else {
            sendMessage.setReplyMarkup(getAlertsKeyboard(language));
            sendMessage.setText(LocalisationService.getInstance().getString("noAlertList", language));
        }

        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    private static SendMessage onNewAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getRecentsKeyboard(message.getFrom().getId(), language, false));
        sendMessage.setText(LocalisationService.getInstance().getString("chooseNewAlertCity", language));
        sendMessage.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTNEW);
        return sendMessage;
    }

    // endregion Alerts Menu Option selected

    // region Settings Menu Option selected

    private static SendMessage messageOnSetting(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getLanguagesCommand(language))) {
                sendMessageRequest = onLanguageCommand(message, language);
            } else if (message.getText().startsWith(getUnitsCommand(language))) {
                sendMessageRequest = onUnitsCommand(message, language);
            } else if (message.getText().startsWith(getAlertsCommand(language))) {
                sendMessageRequest = onAlertsCommand(message, language);
            } else if (message.getText().startsWith(getBackCommand(language))) {
                sendMessageRequest = sendMessageDefault(message, language);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getSettingsKeyboard(language), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onAlertsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));
        sendMessage.setText(LocalisationService.getInstance().getString("alertsMenuMessage", language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getUnitsKeyboard(language));
        sendMessage.setText(getUnitsMessage(message.getFrom().getId(), language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), UNITS);
        return sendMessage;
    }

    private static SendMessage onLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getLanguagesKeyboard(language));
        sendMessage.setText(getLanguageMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), LANGUAGE);
        return sendMessage;
    }

    // endregion Settings Menu Option selected

    // region Units Menu Option selected

    private static SendMessage messageOnUnits(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                sendMessageRequest = onBackUnitsCommand(message, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("metricSystem", language))) {
                sendMessageRequest = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.METRICSYSTEM, language);
            } else if (message.getText().trim().equals(LocalisationService.getInstance().getString("imperialSystem", language))) {
                sendMessageRequest = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.IMPERIALSYSTEM, language);
            } else {
                sendMessageRequest = onUnitsError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onBackUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onUnitsError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setReplyMarkup(getUnitsKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorUnitsNotFound", language));
        sendMessageRequest.setReplyToMessageId(messageId);

        return sendMessageRequest;
    }

    private static SendMessage onUnitsChosen(Integer userId, Long chatId, Integer messageId, String units, String language) {
        DatabaseManager.getInstance().putUserWeatherUnitsOption(userId, units);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setText(LocalisationService.getInstance().getString("unitsUpdated", language));
        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Units Menu Option selected

    // region Language Menu Option selected

    private static SendMessage messageOnLanguage(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                sendMessageRequest = onBackLanguageCommand(message, language);
            } else if (LocalisationService.getInstance().getSupportedLanguages().values().contains(message.getText().trim())) {
                sendMessageRequest = onLanguageChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), message.getText().trim());
            } else {
                sendMessageRequest = onLanguageError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onBackLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onLanguageError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setReplyMarkup(getLanguagesKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getInstance().getString("errorLanguageNotFound", language));
        sendMessageRequest.setReplyToMessageId(messageId);

        return sendMessageRequest;
    }

    private static SendMessage onLanguageChosen(Integer userId, Long chatId, Integer messageId, String language) {
        String languageCode = LocalisationService.getInstance().getLanguageCodeByName(language);
        DatabaseManager.getInstance().putUserWeatherLanguageOption(userId, languageCode);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setText(LocalisationService.getInstance().getString("languageUpdated", languageCode));
        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(languageCode));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Language Menu Option selected

    // region Forecast Weather Menu Option selected

    private static SendMessage messageOnForecastWeather(Message message, String language, int state) {
        SendMessage sendMessageRequest = null;
        switch(state) {
            case FORECASTWEATHER:
                sendMessageRequest = onForecastWeather(message, language);
                break;
            case FORECASTNEWWEATHER:
                sendMessageRequest = onForecastNewWeather(message, language);
                break;
            case FORECASTLOCATIONWEATHER:
                sendMessageRequest = onForecastWeatherLocation(message, language);
                break;
        }
        return sendMessageRequest;
    }

    private static SendMessage onForecastWeather(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                sendMessageRequest = onNewForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                sendMessageRequest = onLocationForecastWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                sendMessageRequest = onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                sendMessageRequest = onForecastWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onForecastNewWeather(Message message, String language) {
        if (message.isReply()) {
            return onForecastWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    private static SendMessage onForecastWeatherCityReceived(Long chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherForecast(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplyToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId.toString());

            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static SendMessage onLocationForecastWeatherCommand(Long chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTLOCATIONWEATHER);
        return sendMessage;
    }

    private static SendMessage onNewForecastWeatherCommand(Long chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTNEWWEATHER);
        return sendMessage;
    }

    private static SendMessage onForecastWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            return onForecastWeatherLocationReceived(message, language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    // endregion Forecast Weather Menu Option selected

    // region Current Weather Menu Option selected

    private static SendMessage messageOnCurrentWeather(Message message, String language, int state) {
        SendMessage sendMessageRequest = null;
        switch(state) {
            case CURRENTWEATHER:
                sendMessageRequest = onCurrentWeather(message, language);
                break;
            case CURRENTNEWWEATHER:
                sendMessageRequest = onCurrentNewWeather(message, language);
                break;
            case CURRENTLOCATIONWEATHER:
                sendMessageRequest = onCurrentWeatherLocation(message, language);
                break;
        }

        return sendMessageRequest;
    }

    private static SendMessage onCurrentWeather(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getNewCommand(language))) {
                sendMessageRequest = onNewCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getLocationCommand(language))) {
                sendMessageRequest = onLocationCurrentWeatherCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(), language);
            } else if (message.getText().startsWith(getCancelCommand(language))) {
                sendMessageRequest = onCancelCommand(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            } else {
                sendMessageRequest = onCurrentWeatherCityReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(),
                        message.getText(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onCurrentNewWeather(Message message, String language) {
        if (message.isReply()) {
            return onCurrentWeatherReceived(message.getChatId(), message.getFrom().getId(), message.getMessageId(), message.getText(), language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    private static SendMessage onCurrentWeatherCityReceived(Long chatId, Integer userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherCurrent(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplyToMessageId(messageId);
            sendMessageRequest.setText(weather);
            sendMessageRequest.setChatId(chatId.toString());
            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static SendMessage onLocationCurrentWeatherCommand(Long chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherLocationCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTLOCATIONWEATHER);
        return sendMessage;
    }

    private static SendMessage onNewCurrentWeatherCommand(Long chatId, Integer userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("onWeatherNewCommand", language));

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTNEWWEATHER);
        return sendMessage;
    }

    private static SendMessage onCurrentWeatherLocation(Message message, String language) {
        if (message.isReply() && message.hasLocation()) {
            return onCurrentWeatherLocationReceived(message, language);
        } else {
            return sendMessageDefault(message, language);
        }
    }

    // endregion Current Weather Menu Option selected

    // region Main menu options selected

    private static SendMessage messageOnMainMenu(Message message, String language) {
        SendMessage sendMessageRequest;
        if (message.hasText()) {
            if (message.getText().equals(getCurrentCommand(language))) {
                sendMessageRequest = onCurrentChoosen(message, language);
            } else if (message.getText().equals(getForecastCommand(language))) {
                sendMessageRequest = onForecastChoosen(message, language);
            } else if (message.getText().equals(getSettingsCommand(language))) {
                sendMessageRequest = onSettingsChoosen(message, language);
            } else if (message.getText().equals(getRateCommand(language))) {
                sendMessageRequest = sendRateMessage(message.getChatId(), message.getMessageId(), null, language);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language), language);
            }
        } else {
            sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getMainMenuKeyboard(language), language);
        }

        return sendMessageRequest;
    }

    private static SendMessage onSettingsChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onForecastChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandFromHistory", language));
        } else {
            sendMessage.setText(LocalisationService.getInstance().getString("onForecastCommandWithoutHistory", language));
        }

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), FORECASTWEATHER);
        return sendMessage;
    }

    private static SendMessage onCurrentChoosen(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
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

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getCurrentCommand(language));
        keyboardFirstRow.add(getForecastCommand(language));
        KeyboardRow keyboardSecondRow = new KeyboardRow();
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

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String languageName : LocalisationService.getInstance().getSupportedLanguages().values()) {
            KeyboardRow row = new KeyboardRow();
            row.add(languageName);
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
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

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(LocalisationService.getInstance().getString("metricSystem", language));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(LocalisationService.getInstance().getString("imperialSystem", language));
        keyboard.add(row);
        row = new KeyboardRow();
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

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getLanguagesCommand(language));
        keyboardFirstRow.add(getUnitsCommand(language));
        KeyboardRow keyboardSecondRow = new KeyboardRow();
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

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String recentWeather : DatabaseManager.getInstance().getRecentWeather(userId)) {
            KeyboardRow row = new KeyboardRow();
            row.add(recentWeather);
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
        if (allowNew) {
            row.add(getLocationCommand(language));
            keyboard.add(row);

            row = new KeyboardRow();
            row.add(getNewCommand(language));
            keyboard.add(row);

            row = new KeyboardRow();
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

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(getNewCommand(language));
        row.add(getDeleteCommand(language));
        keyboard.add(row);

        row = new KeyboardRow();
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

            List<KeyboardRow> keyboard = new ArrayList<>();
            for (String alertCityName: alertCitiesNames) {
                KeyboardRow row = new KeyboardRow();
                row.add(alertCityName);
                keyboard.add(row);
            }
            KeyboardRow row = new KeyboardRow();
            row.add(getCancelCommand(language));
            keyboard.add(row);

            replyKeyboardMarkup.setKeyboard(keyboard);
        }

        return replyKeyboardMarkup;
    }

    private static ForceReplyKeyboard getForceReply() {
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
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

    private static SendMessage sendMessageDefault(Message message, String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getMainMenuKeyboard(language);
        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendHelpMessage(message.getChatId(), message.getMessageId(), replyKeyboardMarkup, language);
    }

    private static SendMessage sendChooseOptionMessage(Long chatId, Integer messageId,
                                                ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getInstance().getString("chooseOption", language));

        return sendMessage;
    }

    private static SendMessage sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(getHelpMessage(language));
        return sendMessage;
    }

    private static SendMessage sendRateMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(LocalisationService.getInstance().getString("rateMeMessage", language));

        return sendMessage;
    }

    // endregion Send common messages

    // region Send weather

    private static SendMessage onForecastWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onForecastWeatherReceived(Long chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherForecast(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId.toString());

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onCurrentWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrentByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(message.getChatId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onCurrentWeatherReceived(Long chatId, Integer userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrent(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setText(weather);
        sendMessageRequest.setChatId(chatId.toString());

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Send weather
}
