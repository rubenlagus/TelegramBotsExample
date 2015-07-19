package org.telegram.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.telegram.BuildVars;
import org.telegram.database.DatabaseManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Weather service
 * @date 20 of June of 2015
 */
public class WeatherService {
    private static volatile BotLogger log = BotLogger.getLogger(WeatherService.class.getName());

    private static final String FILEIDSUNNY = "BQADBAADEgoAAnVbtwRqrxn89vjb1wI";
    private static final String FILEIDFEWCLOUDS = "BQADBAADEwoAAnVbtwT3NZvlhOXiKQI";
    private static final String FILEIDCLOUDS = "BQADBAADFAoAAnVbtwR8DOcw8SdYbwI";
    private static final String FILEIDSHOWERRAIN = "BQADBAADFQoAAnVbtwQL2CuuhfiIYgI";
    private static final String FILEIDRAIN = "BQADBAADFgoAAnVbtwSxZkQoAlHhJAI";
    private static final String FILEIDTHUNDERSTORM = "BQADBAADGAoAAnVbtwQp1kcPThwm7QI";
    private static final String FILEIDSNOW = "BQADBAADGAoAAnVbtwRtYBVrEJPQPQI";
    private static final String FILEIDFOGGY = "BQADBAADFwoAAnVbtwRwJotTvbcb0gI";

    private static final String BASEURL = "http://api.openweathermap.org/data/2.5/"; ///< Base url for REST
    private static final String FORECASTPATH = "forecast/daily";
    private static final String CURRENTPATH = "weather";
    private static final String APIIDEND = "&APPID=" + BuildVars.OPENWEATHERAPIKEY;
    private static final String FORECASTPARAMS = "&cnt=3&units=metric&lang=@language@";
    private static final String CURRENTPARAMS = "&cnt=1&units=metric&lang=@language@";
    private static final DateTimeFormatter dateFormaterFromDate = DateTimeFormatter.ofPattern("dd/MM/yyyy"); ///< Date to text formater
    private static volatile WeatherService instance; ///< Instance of this class

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
     * Fetch the weather of a city
     *
     * @param city City to get the weather
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherForecast(String city, Integer userId, String language) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + FORECASTPATH + "?" + getCityQuery(city) +
                    FORECASTPARAMS.replace("@language@", language) + APIIDEND;
            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completURL);

            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            JSONObject jsonObject = new JSONObject(responseString);
            log.warning(jsonObject.toString());
            if (jsonObject.getInt("cod") == 200) {
                cityFound = jsonObject.getJSONObject("city").getString("name") + " (" +
                        jsonObject.getJSONObject("city").getString("country") + ")";
                saveRecentWeather(userId, cityFound, jsonObject.getJSONObject("city").getInt("id"));
                responseToUser = String.format(LocalisationService.getInstance().getString("weatherForcast", language),
                        cityFound, convertListOfForecastToString(jsonObject, language));
            } else {
                log.warning(jsonObject.toString());
                responseToUser = LocalisationService.getInstance().getString("cityNotFound", language);
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = LocalisationService.getInstance().getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherForecastByLocation(Double longitude, Double latitude, Integer userId, String language) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + FORECASTPATH + "?lat=" + URLEncoder.encode(latitude + "", "UTF-8") + "&lon="
                    + URLEncoder.encode(longitude + "", "UTF-8") + FORECASTPARAMS.replace("@language@", language) + APIIDEND;;
            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completURL);
            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            JSONObject jsonObject = new JSONObject(responseString);
            if (jsonObject.getInt("cod") == 200) {
                cityFound = jsonObject.getJSONObject("city").getString("name") + " (" +
                        jsonObject.getJSONObject("city").getString("country") + ")";
                saveRecentWeather(userId, cityFound, jsonObject.getJSONObject("city").getInt("id"));
                responseToUser = String.format(LocalisationService.getInstance().getString("weatherForcast", language),
                        cityFound, convertListOfForecastToString(jsonObject, language));
            } else {
                log.warning(jsonObject.toString());
                responseToUser = LocalisationService.getInstance().getString("cityNotFound", language);
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = LocalisationService.getInstance().getString("errorFetchingWeather", language);
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
    public String fetchWeatherCurrent(String city, Integer userId, String language) {
        String cityFound;
        String responseToUser;
        Emoji emoji = null;
        try {
            String completURL = BASEURL + CURRENTPATH + "?" + getCityQuery(city) +
                    CURRENTPARAMS.replace("@language@", language) + APIIDEND;
            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completURL);
            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            JSONObject jsonObject = new JSONObject(responseString);
            if (jsonObject.getInt("cod") == 200) {
                cityFound = jsonObject.getString("name") + " (" +
                        jsonObject.getJSONObject("sys").getString("country") + ")";
                saveRecentWeather(userId, cityFound, jsonObject.getInt("id"));
                emoji = getEmojiForWeather(jsonObject.getJSONArray("weather").getJSONObject(0));
                responseToUser = String.format(LocalisationService.getInstance().getString("weatherCurrent", language),
                        cityFound, convertCurrentWeatherToString(jsonObject, language, emoji));
            } else {
                log.warning(jsonObject.toString());
                responseToUser = LocalisationService.getInstance().getString("cityNotFound", language);
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = LocalisationService.getInstance().getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherCurrentByLocation(Double longitude, Double latitude, Integer userId, String language) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + CURRENTPATH + "?q=" + URLEncoder.encode("lat=" + latitude + "&lon=" +
                    longitude, "UTF-8") + CURRENTPARAMS.replace("@language@", language) + APIIDEND;;
            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(completURL);
            CloseableHttpResponse response = client.execute(request);
            HttpEntity ht = response.getEntity();

            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            String responseString = EntityUtils.toString(buf, "UTF-8");

            JSONObject jsonObject = new JSONObject(responseString);
            if (jsonObject.getInt("cod") == 200) {
                cityFound = jsonObject.getString("name") + " (" +
                        jsonObject.getJSONObject("sys").getString("country") + ")";
                saveRecentWeather(userId, cityFound, jsonObject.getInt("id"));
                responseToUser = String.format(LocalisationService.getInstance().getString("weatherCurrent", language),
                        cityFound, convertCurrentWeatherToString(jsonObject, language, null));
            } else {
                log.warning(jsonObject.toString());
                responseToUser = LocalisationService.getInstance().getString("cityNotFound", language);
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = LocalisationService.getInstance().getString("errorFetchingWeather", language);
        }
        return responseToUser;
    }

    private String convertCurrentWeatherToString(JSONObject jsonObject, String language, Emoji emoji) {
        String temp = jsonObject.getJSONObject("main").getDouble("temp")+"";
        String cloudiness = jsonObject.getJSONObject("clouds").getInt("all") + "%";
        String weatherDesc = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

        String responseToUser = LocalisationService.getInstance().getString("currentWeatherPart", language);
        responseToUser = String.format(responseToUser, emoji == null ? weatherDesc : emoji.toString(), cloudiness, temp);

        return responseToUser;
    }

    /**
     * Convert a list of weather forcast to a list of strings to be sent
     *
     * @param jsonObject JSONObject contining the list
     * @return String to be sent to the user
     */
    private String convertListOfForecastToString(JSONObject jsonObject, String language) {
        String responseToUser = "";
        for (int i = 0; i < jsonObject.getJSONArray("list").length(); i++) {
            JSONObject internalJSON = jsonObject.getJSONArray("list").getJSONObject(i);
            responseToUser += convertInternalInformationToString(internalJSON, language);
        }
        return responseToUser;
    }

    /**
     * Convert internal part of then answer to string
     *
     * @param internalJSON JSONObject containing the part to convert
     * @return String to be sent to the user
     */
    private String convertInternalInformationToString(JSONObject internalJSON, String language) {
        String responseToUser = "";
        LocalDate date;
        String tempMax;
        String tempMin;
        String weatherDesc;
        date = Instant.ofEpochSecond(internalJSON.getLong("dt")).atZone(ZoneId.systemDefault()).toLocalDate();
        tempMax = internalJSON.getJSONObject("temp").getDouble("max") + "";
        tempMin = internalJSON.getJSONObject("temp").getDouble("min") + "";
        JSONObject weatherObject = internalJSON.getJSONArray("weather").getJSONObject(0);
        Emoji emoji = getEmojiForWeather(internalJSON.getJSONArray("weather").getJSONObject(0));
        weatherDesc = weatherObject.getString("description");

        responseToUser = LocalisationService.getInstance().getString("forecastWeatherPart", language);
        responseToUser = String.format(responseToUser, dateFormaterFromDate.format(date),
                emoji == null ? weatherDesc : emoji.toString(), tempMax, tempMin);

        return responseToUser;
    }

    private void saveRecentWeather(Integer userId, String cityName, int cityId) {
        DatabaseManager.getInstance().addRecentWeather(userId, cityId, cityName);
    }

    private String getCityQuery(String city) throws UnsupportedEncodingException {
        String cityQuery = "";
        try {
            cityQuery += "id=" + URLEncoder.encode(Integer.parseInt(city)+"", "UTF-8");
        } catch(NumberFormatException | NullPointerException  e) {
            cityQuery += "q=" + URLEncoder.encode(city, "UTF-8");
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
