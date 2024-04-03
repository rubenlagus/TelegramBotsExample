package org.telegram.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.telegram.BuildVars;
import org.telegram.database.DatabaseManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Weather service
 */
@Slf4j
public class WeatherService {
    public static final String METRICSYSTEM = "metric";
    public static final String IMPERIALSYSTEM = "imperial";

    private static final String BASEURL = "http://api.openweathermap.org/data/2.5/"; ///< Base url for REST
    private static final String FORECASTPATH = "forecast/daily";
    private static final String CURRENTPATH = "weather";
    private static final String APIIDEND = "&APPID=" + BuildVars.OPENWEATHERAPIKEY;
    private static final String FORECASTPARAMS = "&cnt=3&units=@units@&lang=@language@";
    private static final String ALERTPARAMS = "&cnt=1&units=@units@&lang=@language@";
    private static final String CURRENTPARAMS = "&cnt=1&units=@units@&lang=@language@";
    private static final DateTimeFormatter dateFormaterFromDate = DateTimeFormatter.ofPattern("dd/MM/yyyy"); ///< Date to text formater
    private static volatile WeatherService instance; ///< Instance of this class

    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();

    /**
     * Constructor (private due to singleton pattern)
     */
    private WeatherService() {
    }

    /**
     * Singleton
     *
     * @return Return the instance of this class
     */
    public static WeatherService getInstance() {
        WeatherService currentInstance;
        if (instance == null) {
            synchronized (WeatherService.class) {
                if (instance == null) {
                    instance = new WeatherService();
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    /**
     * Fetch the weather of a city for one day
     *
     * @param cityId City to get the weather
     * @return userHash to be send to use
     * @apiNote Forecast for the day
     */
    public String fetchWeatherAlert(int cityId, long userId, String language, String units) {
        String cityFound;
        String responseToUser;
        try {
            String completeURL = BASEURL + FORECASTPATH + "?" + getCityQuery(cityId + "") +
                    ALERTPARAMS.replace("@language@", language).replace("@units@", units) + APIIDEND;
            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            log.info("Alert fetched: {}", jsonObject);
                            if (jsonObject.getInt("cod") == 200) {
                                cityFound = jsonObject.getJSONObject("city").getString("name") + " (" +
                                        jsonObject.getJSONObject("city").getString("country") + ")";
                                saveRecentWeather(userId, cityFound, jsonObject.getJSONObject("city").getInt("id"));
                                responseToUser = String.format(LocalisationService.getString("weatherAlert", language),
                                        cityFound, convertListOfForecastToString(jsonObject, language, units, false));
                            } else {
                                log.warn("Unable to read alerts fetched {}", jsonObject);
                                responseToUser = LocalisationService.getString("cityNotFound", language);
                            }
                        } else {
                            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                        }
                    }
                } else {
                    responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching alerts", e);
            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @param city City to get the weather
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherForecast(String city, Long userId, String language, String units) {
        String cityFound;
        String responseToUser;
        try {
            String completeURL = BASEURL + FORECASTPATH + "?" + getCityQuery(city) +
                    FORECASTPARAMS.replace("@language@", language).replace("@units@", units) + APIIDEND;
            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            log.info("Fetched weather forecast {}", jsonObject);
                            if (jsonObject.getInt("cod") == 200) {
                                cityFound = jsonObject.getJSONObject("city").getString("name") + " (" +
                                        jsonObject.getJSONObject("city").getString("country") + ")";
                                saveRecentWeather(userId, cityFound, jsonObject.getJSONObject("city").getInt("id"));
                                responseToUser = String.format(LocalisationService.getString("weatherForcast", language),
                                        cityFound, convertListOfForecastToString(jsonObject, language, units, true));
                            } else {
                                log.warn("City forecast not found {}", jsonObject);
                                responseToUser = LocalisationService.getString("cityNotFound", language);
                            }
                        } else {
                            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                        }
                    }
                } else {
                    responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching city forecast", e);
            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherForecastByLocation(Double longitude, Double latitude, Long userId, String language, String units) {
        String cityFound;
        String responseToUser;
        try {
            String completeURL = BASEURL + FORECASTPATH + "?lat=" + URLEncoder.encode(latitude + "", StandardCharsets.UTF_8) + "&lon="
                    + URLEncoder.encode(longitude + "", StandardCharsets.UTF_8) +
                    FORECASTPARAMS.replace("@language@", language).replace("@units@", units) + APIIDEND;
            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            if (jsonObject.getInt("cod") == 200) {
                                cityFound = jsonObject.getJSONObject("city").getString("name") + " (" +
                                        jsonObject.getJSONObject("city").getString("country") + ")";
                                saveRecentWeather(userId, cityFound, jsonObject.getJSONObject("city").getInt("id"));
                                responseToUser = String.format(LocalisationService.getString("weatherForcast", language),
                                        cityFound, convertListOfForecastToString(jsonObject, language, units, true));
                            } else {
                                log.warn("No forecast for location found {}", jsonObject);
                                responseToUser = LocalisationService.getString("cityNotFound", language);
                            }
                        } else {
                            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                        }
                    }
                } else {
                    responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching location forecast", e);
            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @param city City to get the weather
     * @return userHash to be send to use
     * @apiNote Forecast for the following 3 days
     */
    public String fetchWeatherCurrent(String city, Long userId, String language, String units) {
        String cityFound;
        String responseToUser;
        Emoji emoji = null;
        try {
            String completeURL = BASEURL + CURRENTPATH + "?" + getCityQuery(city) +
                    CURRENTPARAMS.replace("@language@", language).replace("@units@", units) + APIIDEND;
            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            if (jsonObject.getInt("cod") == 200) {
                                cityFound = jsonObject.getString("name") + " (" +
                                        jsonObject.getJSONObject("sys").getString("country") + ")";
                                saveRecentWeather(userId, cityFound, jsonObject.getInt("id"));
                                emoji = getEmojiForWeather(jsonObject.getJSONArray("weather").getJSONObject(0));
                                responseToUser = String.format(LocalisationService.getString("weatherCurrent", language),
                                        cityFound, convertCurrentWeatherToString(jsonObject, language, units, emoji));
                            } else {
                                log.warn("No current weather found {}", jsonObject);
                                responseToUser = LocalisationService.getString("cityNotFound", language);
                            }
                        } else {
                            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                        }
                    }
                } else {
                    responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching current weather", e);
            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherCurrentByLocation(Double longitude, Double latitude, Long userId, String language, String units) {
        String cityFound;
        String responseToUser;
        try {
            String completeURL = BASEURL + CURRENTPATH + "?lat=" + URLEncoder.encode(latitude + "", StandardCharsets.UTF_8) + "&lon="
                    + URLEncoder.encode(longitude + "", StandardCharsets.UTF_8) +
                    CURRENTPARAMS.replace("@language@", language).replace("@units@", units) + APIIDEND;
            Request request = new Request.Builder()
                    .url(completeURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            if (jsonObject.getInt("cod") == 200) {
                                cityFound = jsonObject.getString("name") + " (" +
                                        jsonObject.getJSONObject("sys").getString("country") + ")";
                                saveRecentWeather(userId, cityFound, jsonObject.getInt("id"));
                                responseToUser = String.format(LocalisationService.getString("weatherCurrent", language),
                                        cityFound, convertCurrentWeatherToString(jsonObject, language, units, null));
                            } else {
                                log.warn("No weather found for location {}", jsonObject);
                                responseToUser = LocalisationService.getString("cityNotFound", language);
                            }
                        } else {
                            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                        }
                    }
                } else {
                    responseToUser = LocalisationService.getString("errorFetchingWeather", language);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching weather for location", e);
            responseToUser = LocalisationService.getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    private String convertCurrentWeatherToString(JSONObject jsonObject, String language, String units, Emoji emoji) {
        String temp = ((int)jsonObject.getJSONObject("main").getDouble("temp"))+"";
        String cloudiness = jsonObject.getJSONObject("clouds").getInt("all") + "%";
        String weatherDesc = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

        String responseToUser;
        if (units.equals(METRICSYSTEM)) {
            responseToUser = LocalisationService.getString("currentWeatherPartMetric", language);
        } else {
            responseToUser = LocalisationService.getString("currentWeatherPartImperial", language);
        }
        responseToUser = String.format(responseToUser, emoji == null ? weatherDesc : emoji.toString(), cloudiness, temp);

        return responseToUser;
    }

    /**
     * Convert a list of weather forcast to a list of strings to be sent
     *
     * @param jsonObject JSONObject contining the list
     * @return String to be sent to the user
     */
    private String convertListOfForecastToString(JSONObject jsonObject, String language, String units, boolean addDate) {
        String responseToUser = "";
        for (int i = 0; i < jsonObject.getJSONArray("list").length(); i++) {
            JSONObject internalJSON = jsonObject.getJSONArray("list").getJSONObject(i);
            responseToUser += convertInternalInformationToString(internalJSON, language, units, addDate);
        }
        return responseToUser;
    }

    /**
     * Convert internal part of then answer to string
     *
     * @param internalJSON JSONObject containing the part to convert
     * @return String to be sent to the user
     */
    private String convertInternalInformationToString(JSONObject internalJSON, String language, String units, boolean addDate) {
        String responseToUser = "";
        LocalDate date;
        String tempMax;
        String tempMin;
        String weatherDesc;
        date = Instant.ofEpochSecond(internalJSON.getLong("dt")).atZone(ZoneId.systemDefault()).toLocalDate();
        tempMax = ((int)internalJSON.getJSONObject("temp").getDouble("max")) + "";
        tempMin = ((int)internalJSON.getJSONObject("temp").getDouble("min")) + "";
        JSONObject weatherObject = internalJSON.getJSONArray("weather").getJSONObject(0);
        Emoji emoji = getEmojiForWeather(internalJSON.getJSONArray("weather").getJSONObject(0));
        weatherDesc = weatherObject.getString("description");

        if (units.equals(METRICSYSTEM)) {
            if (addDate) {
                responseToUser = LocalisationService.getString("forecastWeatherPartMetric", language);
            } else {
                responseToUser = LocalisationService.getString("alertWeatherPartMetric", language);
            }
        } else {
            if (addDate) {
                responseToUser = LocalisationService.getString("forecastWeatherPartImperial", language);
            } else {
                responseToUser = LocalisationService.getString("alertWeatherPartImperial", language);
            }
        }
        if (addDate) {
            responseToUser = String.format(responseToUser, Emoji.LARGE_ORANGE_DIAMOND.toString(),
                    dateFormaterFromDate.format(date), emoji == null ? weatherDesc : emoji.toString(), tempMax, tempMin);
        } else {
            responseToUser = String.format(responseToUser, emoji == null ? weatherDesc : emoji.toString(),
                    tempMax, tempMin);
        }

        return responseToUser;
    }

    private void saveRecentWeather(Long userId, String cityName, int cityId) {
        DatabaseManager.getInstance().addRecentWeather(userId, cityId, cityName);
    }

    private String getCityQuery(String city) throws UnsupportedEncodingException {
        String cityQuery = "";
        try {
            cityQuery += "id=" + URLEncoder.encode(Integer.parseInt(city)+"", StandardCharsets.UTF_8);
        } catch(NumberFormatException | NullPointerException  e) {
            cityQuery += "q=" + URLEncoder.encode(city, StandardCharsets.UTF_8);
        }
        return cityQuery;
    }

    private Emoji getEmojiForWeather(JSONObject weather) {
        Emoji emoji;

        switch(weather.getString("icon")) {
            case "01n":
            case "01d":
                emoji = Emoji.SUN_WITH_FACE;
                break;
            case "02n":
            case "02d":
                emoji = Emoji.SUN_BEHIND_CLOUD;
                break;
            case "03n":
            case "03d":
            case "04n":
            case "04d":
                emoji = Emoji.CLOUD;
                break;
            case "09n":
            case "09d":
            case "10n":
            case "10d":
                emoji = Emoji.UMBRELLA_WITH_RAIN_DROPS;
                break;
            case "11n":
            case "11d":
                emoji = Emoji.HIGH_VOLTAGE_SIGN;
                break;
            case "13n":
            case "13d":
                emoji = Emoji.SNOWFLAKE;
                break;
            case "50n":
            case "50d":
                emoji = Emoji.FOGGY;
                break;
            default:
                emoji = null;
        }

        return emoji;
    }
}
