package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.Commands;
import org.telegram.database.DatabaseManager;
import org.telegram.services.CustomTimerTask;
import org.telegram.services.Emoji;
import org.telegram.services.LocalisationService;
import org.telegram.services.TimerExecutor;
import org.telegram.services.WeatherService;
import org.telegram.structure.WeatherAlert;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Handler for updates to Weather Bot
 */
@Slf4j
public class WeatherHandlers implements LongPollingSingleThreadUpdateConsumer {
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

    private final TelegramClient telegramClient;

    public WeatherHandlers(String botToken) {
        startAlertTimers();
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    handleIncomingMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("Weather Handler error", e);
        }
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
                    log.error("Error sleeping for alerts", e);
                }
            }
            String[] userOptions = DatabaseManager.getInstance().getUserWeatherOptions(weatherAlert.getUserId());
            String weather = WeatherService.getInstance().fetchWeatherAlert(weatherAlert.getCityId(),
                    weatherAlert.getUserId(), userOptions[0], userOptions[1]);
            SendMessage sendMessage = new SendMessage(String.valueOf(weatherAlert.getUserId()), weather);
            sendMessage.enableMarkdown(true);
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiRequestException e) {
                log.warn("Error sending alerts", e);
                if (e.getApiResponse().contains("Can't access the chat") || e.getApiResponse().contains("Bot was blocked by the user")) {
                    DatabaseManager.getInstance().deleteAlertsForUser(weatherAlert.getUserId());
                }
            } catch (Exception e) {
                log.error("Unknown error sending alerts", e);
            }
        }
    }

    private static SendMessage onCancelCommand(Long chatId, Long userId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("backToMainMenu", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
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

        telegramClient.execute(sendMessageRequest);
    }

    private void sendHideKeyboard(Long userId, Long chatId, Integer messageId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(chatId.toString(), Emoji.WAVING_HAND_SIGN.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);

        ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder().selective(true).build();
        sendMessage.setReplyMarkup(replyKeyboardRemove);

        telegramClient.execute(sendMessage);
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
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("alertDeleted", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onAlertDeleteBackOptionSelected(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("alertsMenuMessage", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onAlertNewOptionSelected(Message message, String language) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().equals(getCancelCommand(language))) {
                SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("alertsMenuMessage", language));
                sendMessage.enableMarkdown(true);
                sendMessage.setReplyToMessageId(message.getMessageId());
                sendMessage.setReplyMarkup(getAlertsKeyboard(language));
                DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
                sendMessageRequest = sendMessage;
            } else {
                sendMessageRequest = onNewAlertCityReceived(message, language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onNewAlertCityReceived(Message message, String language) {
        long userId = message.getFrom().getId();
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, message.getText());
        if (cityId != null) {
            DatabaseManager.getInstance().createNewWeatherAlert(userId, cityId, message.getText());
            SendMessage sendMessageRequest = SendMessage.builder().chatId(message.getChatId()).text(getChooseNewAlertSetMessage(message.getText(), language)).build();
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getAlertsKeyboard(language));
            sendMessageRequest.setReplyToMessageId(message.getMessageId());

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
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getSettingsMessage(language));
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onListAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getAlertListMessage(message.getFrom().getId(), language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));
        sendMessage.setReplyToMessageId(message.getMessageId());

        return sendMessage;
    }

    private static SendMessage onDeleteAlertCommand(Message message, String language) {
        SendMessage.SendMessageBuilder<?, ?> sendMessageBuilder = SendMessage.builder();
        sendMessageBuilder.parseMode(ParseMode.MARKDOWN);
        sendMessageBuilder.chatId(message.getChatId());

        ReplyKeyboardMarkup replyKeyboardMarkup = getAlertsListKeyboard(message.getFrom().getId(), language);
        if (replyKeyboardMarkup != null) {
            sendMessageBuilder.replyMarkup(replyKeyboardMarkup);
            sendMessageBuilder.text(LocalisationService.getString("chooseNewAlertCity", language));
            DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERTDELETE);
        } else {
            sendMessageBuilder.replyMarkup(getAlertsKeyboard(language));
            sendMessageBuilder.text(LocalisationService.getString("noAlertList", language));
        }

        sendMessageBuilder.replyToMessageId(message.getMessageId());
        return sendMessageBuilder.build();
    }

    private static SendMessage onNewAlertCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("chooseNewAlertCity", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(getRecentsKeyboard(message.getFrom().getId(), language, false));
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
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), LocalisationService.getString("alertsMenuMessage", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getAlertsKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), ALERT);
        return sendMessage;
    }

    private static SendMessage onUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getUnitsMessage(message.getFrom().getId(), language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getUnitsKeyboard(language));

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), UNITS);
        return sendMessage;
    }

    private static SendMessage onLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getLanguageMessage(language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getLanguagesKeyboard(language));

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
            } else if (message.getText().trim().equals(LocalisationService.getString("metricSystem", language))) {
                sendMessageRequest = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.METRICSYSTEM, language);
            } else if (message.getText().trim().equals(LocalisationService.getString("imperialSystem", language))) {
                sendMessageRequest = onUnitsChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), WeatherService.IMPERIALSYSTEM, language);
            } else {
                sendMessageRequest = onUnitsError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onBackUnitsCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getSettingsMessage(language));
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onUnitsError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), LocalisationService.getString("errorUnitsNotFound", language));
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getUnitsKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);

        return sendMessageRequest;
    }

    private static SendMessage onUnitsChosen(Long userId, Long chatId, Integer messageId, String units, String language) {
        DatabaseManager.getInstance().putUserWeatherUnitsOption(userId, units);

        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), LocalisationService.getString("unitsUpdated", language));
        sendMessageRequest.enableMarkdown(true);
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
            } else if (LocalisationService.getLanguageByName(message.getText().trim()) != null) {
                sendMessageRequest = onLanguageChosen(message.getFrom().getId(), message.getChatId(),
                        message.getMessageId(), message.getText().trim());
            } else {
                sendMessageRequest = onLanguageError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return sendMessageRequest;
    }

    private static SendMessage onBackLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getSettingsMessage(language));
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onLanguageError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), LocalisationService.getString("errorLanguageNotFound", language));
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getLanguagesKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);

        return sendMessageRequest;
    }

    private static SendMessage onLanguageChosen(Long userId, Long chatId, Integer messageId, String language) {
        String languageCode = LocalisationService.getLanguageCodeByName(language);
        DatabaseManager.getInstance().putUserWeatherLanguageOption(userId, languageCode);

        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), LocalisationService.getString("languageUpdated", languageCode));
        sendMessageRequest.enableMarkdown(true);
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

    private static SendMessage onForecastWeatherCityReceived(Long chatId, Long userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherForecast(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage(chatId.toString(), weather);
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplyToMessageId(messageId);

            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static SendMessage onLocationForecastWeatherCommand(Long chatId, Long userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("onWeatherLocationCommand", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, FORECASTLOCATIONWEATHER);
        return sendMessage;
    }

    private static SendMessage onNewForecastWeatherCommand(Long chatId, Long userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("onWeatherNewCommand", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

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

    private static SendMessage onCurrentWeatherCityReceived(Long chatId, Long userId, Integer messageId, String text, String language) {
        Integer cityId = DatabaseManager.getInstance().getRecentWeatherIdByCity(userId, text);
        if (cityId != null) {
            String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
            String weather = WeatherService.getInstance().fetchWeatherCurrent(cityId.toString(), userId, language, unitsSystem);
            SendMessage sendMessageRequest = new SendMessage(chatId.toString(), weather);
            sendMessageRequest.enableMarkdown(true);
            sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
            sendMessageRequest.setReplyToMessageId(messageId);
            DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
            return sendMessageRequest;
        } else {
            return sendChooseOptionMessage(chatId, messageId, getRecentsKeyboard(userId, language), language);
        }
    }

    private static SendMessage onLocationCurrentWeatherCommand(Long chatId, Long userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("onWeatherLocationCommand", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, CURRENTLOCATIONWEATHER);
        return sendMessage;
    }

    private static SendMessage onNewCurrentWeatherCommand(Long chatId, Long userId, Integer messageId, String language) {
        ForceReplyKeyboard forceReplyKeyboard = getForceReply();

        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("onWeatherNewCommand", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

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
        SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), getSettingsMessage(language));
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), SETTINGS);
        return sendMessage;
    }

    private static SendMessage onForecastChoosen(Message message, String language) {
        SendMessage.SendMessageBuilder<?, ?> sendMessageBuilder = SendMessage.builder();
        sendMessageBuilder.parseMode(ParseMode.MARKDOWN);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessageBuilder.replyMarkup(replyKeyboardMarkup);
        sendMessageBuilder.replyToMessageId(message.getMessageId());
        sendMessageBuilder.chatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessageBuilder.text(LocalisationService.getString("onForecastCommandFromHistory", language));
        } else {
            sendMessageBuilder.text(LocalisationService.getString("onForecastCommandWithoutHistory", language));
        }

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), FORECASTWEATHER);
        return sendMessageBuilder.build();
    }

    private static SendMessage onCurrentChoosen(Message message, String language) {
        SendMessage.SendMessageBuilder<?, ?> sendMessageBuilder = SendMessage.builder();
        sendMessageBuilder.parseMode(ParseMode.MARKDOWN);

        ReplyKeyboardMarkup replyKeyboardMarkup = getRecentsKeyboard(message.getFrom().getId(), language);
        sendMessageBuilder.replyMarkup(replyKeyboardMarkup);
        sendMessageBuilder.replyToMessageId(message.getMessageId());
        sendMessageBuilder.chatId(message.getChatId());
        if (replyKeyboardMarkup.getKeyboard().size() > 3) {
            sendMessageBuilder.text(LocalisationService.getString("onCurrentCommandFromHistory", language));
        } else {
            sendMessageBuilder.text(LocalisationService.getString("onCurrentCommandWithoutHistory", language));
        }

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), CURRENTWEATHER);
        return sendMessageBuilder.build();
    }

    // endregion Main menu options selected

    // region Get Messages

    private static String getSettingsMessage(String language) {
        String baseString = LocalisationService.getString("onSettingsCommand", language);
        return String.format(baseString, Emoji.GLOBE_WITH_MERIDIANS.toString(),
                Emoji.STRAIGHT_RULER.toString(), Emoji.ALARM_CLOCK.toString(),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getHelpMessage(String language) {
        String baseString = LocalisationService.getString("helpWeatherMessage", language);
        return String.format(baseString, Emoji.BLACK_RIGHT_POINTING_TRIANGLE.toString(),
                Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString(), Emoji.ALARM_CLOCK.toString(),
                Emoji.EARTH_GLOBE_EUROPE_AFRICA.toString(), Emoji.STRAIGHT_RULER.toString());
    }

    private static String getLanguageMessage(String language) {
        String baseString = LocalisationService.getString("selectLanguage", language);
        return String.format(baseString, language);
    }

    private static String getUnitsMessage(Long userId, String language) {
        String baseString = LocalisationService.getString("selectUnits", language);
        return String.format(baseString, DatabaseManager.getInstance().getUserWeatherOptions(userId)[1]);
    }

    private static String getChooseNewAlertSetMessage(String city, String language) {
        String baseString = LocalisationService.getString("newAlertSaved", language);
        return String.format(baseString, Emoji.THUMBS_UP_SIGN.toString(), city);
    }

    private static String getAlertListMessage(long userId, String language) {
        String alertListMessage;

        List<String> alertCities = DatabaseManager.getInstance().getAlertCitiesNameByUser(userId);
        if (!alertCities.isEmpty()) {
            String baseAlertListString = LocalisationService.getString("initialAlertList", language);
            String partialAlertListString = LocalisationService.getString("partialAlertList", language);
            StringBuilder fullListOfAlerts = new StringBuilder();
            for (String alertCity : alertCities) {
                fullListOfAlerts.append(String.format(partialAlertListString, Emoji.ALARM_CLOCK, alertCity));
            }
            alertListMessage = String.format(baseAlertListString, alertCities.size(), fullListOfAlerts);
        } else {
            alertListMessage = LocalisationService.getString("noAlertList", language);
        }

        return alertListMessage;
    }


    // endregion Get Messages

    // region ReplyKeyboards

    private static ReplyKeyboardMarkup getMainMenuKeyboard(String language) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getCurrentCommand(language));
        keyboardFirstRow.add(getForecastCommand(language));
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(getSettingsCommand(language));
        keyboardSecondRow.add(getRateCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getLanguagesKeyboard(String language) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String languageName : LocalisationService.getSupportedLanguages().stream().map(
                LocalisationService.Language::getName).toList()) {
            KeyboardRow row = new KeyboardRow();
            row.add(languageName);
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
        row.add(getCancelCommand(language));
        keyboard.add(row);
        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getUnitsKeyboard(String language) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(LocalisationService.getString("metricSystem", language));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(LocalisationService.getString("imperialSystem", language));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(getCancelCommand(language));
        keyboard.add(row);
        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getSettingsKeyboard(String language) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getLanguagesCommand(language));
        keyboardFirstRow.add(getUnitsCommand(language));
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(getAlertsCommand(language));
        keyboardSecondRow.add(getBackCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getRecentsKeyboard(Long userId, String language) {
        return getRecentsKeyboard(userId, language, true);
    }

    private static ReplyKeyboardMarkup getRecentsKeyboard(Long userId, String language, boolean allowNew) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(true);

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

        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getAlertsKeyboard(String language) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
        replyKeyboardMarkupBuilder.selective(true);
        replyKeyboardMarkupBuilder.resizeKeyboard(true);
        replyKeyboardMarkupBuilder.oneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(getNewCommand(language));
        row.add(getDeleteCommand(language));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(getListCommand(language));
        row.add(getBackCommand(language));
        keyboard.add(row);

        replyKeyboardMarkupBuilder.keyboard(keyboard);

        return replyKeyboardMarkupBuilder.build();
    }

    private static ReplyKeyboardMarkup getAlertsListKeyboard(Long userId, String language) {
        List<String> alertCitiesNames = DatabaseManager.getInstance().getAlertCitiesNameByUser(userId);
        if (!alertCitiesNames.isEmpty()) {
            ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder<?, ?> replyKeyboardMarkupBuilder = ReplyKeyboardMarkup.builder();
            replyKeyboardMarkupBuilder.selective(true);
            replyKeyboardMarkupBuilder.resizeKeyboard(true);
            replyKeyboardMarkupBuilder.oneTimeKeyboard(true);

            List<KeyboardRow> keyboard = new ArrayList<>();
            for (String alertCityName: alertCitiesNames) {
                KeyboardRow row = new KeyboardRow();
                row.add(alertCityName);
                keyboard.add(row);
            }
            KeyboardRow row = new KeyboardRow();
            row.add(getCancelCommand(language));
            keyboard.add(row);

            replyKeyboardMarkupBuilder.keyboard(keyboard);
            return replyKeyboardMarkupBuilder.build();
        }

        return null;
    }

    private static ForceReplyKeyboard getForceReply() {
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        return forceReplyKeyboard;
    }

    // endregion ReplyKeyboards

    // region getCommnads

    private static String getRateCommand(String language) {
        return String.format(LocalisationService.getString("rateMe", language),
                Emoji.HUNDRED_POINTS_SYMBOL.toString());
    }

    private static String getListCommand(String language) {
        return String.format(LocalisationService.getString("showList", language),
                Emoji.CLIPBOARD.toString());
    }

    private static String getDeleteCommand(String language) {
        return String.format(LocalisationService.getString("delete", language),
                Emoji.HEAVY_MINUS_SIGN.toString());
    }

    private static String getLanguagesCommand(String language) {
        return String.format(LocalisationService.getString("languages", language),
                Emoji.GLOBE_WITH_MERIDIANS.toString());
    }

    private static String getUnitsCommand(String language) {
        return String.format(LocalisationService.getString("units", language),
                Emoji.STRAIGHT_RULER.toString());
    }

    private static String getAlertsCommand(String language) {
        return String.format(LocalisationService.getString("alerts", language),
                Emoji.ALARM_CLOCK.toString());
    }

    private static String getBackCommand(String language) {
        return String.format(LocalisationService.getString("back", language),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getNewCommand(String language) {
        return String.format(LocalisationService.getString("new", language),
                Emoji.HEAVY_PLUS_SIGN.toString());
    }

    private static String getLocationCommand(String language) {
        return String.format(LocalisationService.getString("location", language),
                Emoji.ROUND_PUSHPIN.toString());
    }

    private static String getSettingsCommand(String language) {
        return String.format(LocalisationService.getString("settings", language),
                Emoji.WRENCH.toString());
    }

    private static String getCurrentCommand(String language) {
        return String.format(LocalisationService.getString("current", language),
                Emoji.BLACK_RIGHT_POINTING_TRIANGLE.toString());
    }

    private static String getForecastCommand(String language) {
        return String.format(LocalisationService.getString("forecast", language),
                Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString());
    }

    private static String getCancelCommand(String language) {
        return String.format(LocalisationService.getString("cancel", language),
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
        SendMessage sendMessage = new SendMessage(chatId.toString(), LocalisationService.getString("chooseOption", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);

        return sendMessage;
    }

    private static SendMessage sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), getHelpMessage(language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        return sendMessage;
    }

    private static SendMessage sendRateMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), LocalisationService.getString("rateMeMessage", language));
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }

        return sendMessage;
    }

    // endregion Send common messages

    // region Send weather

    private static SendMessage onForecastWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherForecastByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), weather);
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onForecastWeatherReceived(Long chatId, Long userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherForecast(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), weather);
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onCurrentWeatherLocationReceived(Message message, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrentByLocation(message.getLocation().getLongitude(),
                message.getLocation().getLatitude(), message.getFrom().getId(), language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage(String.valueOf(message.getChatId()), weather);
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(message.getMessageId());

        DatabaseManager.getInstance().insertWeatherState(message.getFrom().getId(), message.getChatId(), MAINMENU);
        return sendMessageRequest;
    }

    private static SendMessage onCurrentWeatherReceived(Long chatId, Long userId, Integer messageId, String text, String language) {
        String unitsSystem = DatabaseManager.getInstance().getUserWeatherOptions(userId)[1];
        String weather = WeatherService.getInstance().fetchWeatherCurrent(text, userId, language, unitsSystem);
        SendMessage sendMessageRequest = new SendMessage(chatId.toString(), weather);
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language));
        sendMessageRequest.setReplyToMessageId(messageId);

        DatabaseManager.getInstance().insertWeatherState(userId, chatId, MAINMENU);
        return sendMessageRequest;
    }

    // endregion Send weather
}
