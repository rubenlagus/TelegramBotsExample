package org.telegram.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String BASEURL = "http://api.openweathermap.org/data/2.5/"; ///< Base url for REST
    private static final String FORECASTPATH = "forecast/daily";
    private static final String CURRENTPATH = "weather";
    private static final String APIIDEND = "&APPID=" + BuildVars.OPENWEATHERAPIKEY;
    private static final String FORECASTPARAMS = "&cnt=3&units=metric";
    private static final String CURRENTPARAMS = "&cnt=1&units=metric";
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
    public String fetchWeatherForecast(String city, Integer userId) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + FORECASTPATH + "?" + getCityQuery(city) + FORECASTPARAMS + APIIDEND;
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
                responseToUser = "The weather for " + cityFound + " will be:\n\n";
                responseToUser += convertListOfForecastToString(jsonObject);
                responseToUser += "Thank you for using our Weather Bot.\n\n" +
                        "Your Telegram Team";
            } else {
                log.warning(jsonObject.toString());
                responseToUser = "City not found";
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = "Error fetching weather info";
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherForecastByLocation(Double longitude, Double latitude, Integer userId) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + FORECASTPATH + "?lat=" + URLEncoder.encode(latitude + "", "UTF-8") + "&lon=" + URLEncoder.encode(longitude + "", "UTF-8") + FORECASTPARAMS + APIIDEND;;
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
                responseToUser = "The weather for " + cityFound + " will be:\n\n";
                responseToUser += convertListOfForecastToString(jsonObject);
                responseToUser += "Thank you for using our Weather Bot.\n\n" +
                        "Your Telegram Team";
            } else {
                log.warning(jsonObject.toString());
                responseToUser = "City not found";
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = "Error fetching weather info";
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
    public String fetchWeatherCurrent(String city, Integer userId) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + CURRENTPATH + "?" + getCityQuery(city) + CURRENTPARAMS + APIIDEND;
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
                responseToUser = "The weather for " + cityFound + " is:\n\n";
                responseToUser += convertCurrentWeatherToString(jsonObject);
                responseToUser += "Thank you for using our Weather Bot.\n\n" +
                        "Your Telegram Team";
            } else {
                log.warning(jsonObject.toString());
                responseToUser = "City not found";
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = "Error fetching weather info";
        }
        return responseToUser;
    }

    /**
     * Fetch the weather of a city
     *
     * @return userHash to be send to use
     * @note Forecast for the following 3 days
     */
    public String fetchWeatherCurrentByLocation(Double longitude, Double latitude, Integer userId) {
        String cityFound;
        String responseToUser;
        try {
            String completURL = BASEURL + CURRENTPATH + "?q=" + URLEncoder.encode("lat=" + latitude + "&lon=" + longitude, "UTF-8") + CURRENTPARAMS + APIIDEND;;
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
                responseToUser = "The weather for " + cityFound + " is:\n\n";
                responseToUser += convertCurrentWeatherToString(jsonObject);
                responseToUser += "Thank you for using our Weather Bot.\n\n" +
                        "Your Telegram Team";
            } else {
                log.warning(jsonObject.toString());
                responseToUser = "City not found";
            }
        } catch (Exception e) {
            log.error(e);
            responseToUser = "Error fetching weather info";
        }
        return responseToUser;
    }

    private String convertCurrentWeatherToString(JSONObject jsonObject) {
        String temp = jsonObject.getJSONObject("main").getDouble("temp")+"";
        String cloudiness = jsonObject.getJSONObject("clouds").getInt("all") + "%";
        String weatherDesc = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

        String responseToUser = "";
        responseToUser +=
                "  |-- Weather:  " + weatherDesc + "\n" +
                        "  |-- Cloudiness: " + cloudiness + "\n" +
                        "  |-- Temperature: " + temp + "ºC\n\n";

        return responseToUser;
    }

    /**
     * Convert a list of weather forcast to a list of strings to be sent
     *
     * @param jsonObject JSONObject contining the list
     * @return String to be sent to the user
     */
    private String convertListOfForecastToString(JSONObject jsonObject) {
        String responseToUser = "";
        for (int i = 0; i < jsonObject.getJSONArray("list").length(); i++) {
            JSONObject internalJSON = jsonObject.getJSONArray("list").getJSONObject(i);
            responseToUser += convertInternalInformationToString(internalJSON);
        }
        return responseToUser;
    }

    /**
     * Convert internal part of then answer to string
     *
     * @param internalJSON JSONObject containing the part to convert
     * @return String to be sent to the user
     */
    private String convertInternalInformationToString(JSONObject internalJSON) {
        String responseToUser = "";
        LocalDate date;
        String tempMax;
        String tempMin;
        String weatherDesc;
        date = Instant.ofEpochSecond(internalJSON.getLong("dt")).atZone(ZoneId.systemDefault()).toLocalDate();
        tempMax = internalJSON.getJSONObject("temp").getDouble("max") + "";
        tempMin = internalJSON.getJSONObject("temp").getDouble("min") + "";
        JSONObject weatherObject = internalJSON.getJSONArray("weather").getJSONObject(0);
        weatherDesc = weatherObject.getString("description");

        responseToUser += "*On " + dateFormaterFromDate.format(date) + "\n" +
                "  |--Forecast:  " + weatherDesc + "\n" +
                "  |--High temperature: " + tempMax + "ºC\n" +
                "  |--Low temperature: " + tempMin + "ºC\n\n";
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
}
