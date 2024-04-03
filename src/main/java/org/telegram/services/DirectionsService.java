package org.telegram.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.telegram.BuildVars;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Weather service
 */
@Slf4j
public class DirectionsService {
    private static final String BASEURL = "https://maps.googleapis.com/maps/api/directions/json"; ///< Base url for REST
    private static final String APIIDEND = "&key=" + BuildVars.DirectionsApiKey;
    private static final String PARAMS = "&language=@language@&units=metric";
    private static final DateTimeFormatter dateFormaterFromDate = DateTimeFormatter.ofPattern("dd/MM/yyyy"); ///< Date to text formater
    private static volatile DirectionsService instance; ///< Instance of this class

    /**
     * Constructor (private due to singleton pattern)
     */
    private DirectionsService() {
    }

    /**
     * Singleton
     *
     * @return Return the instance of this class
     */
    public static DirectionsService getInstance() {
        DirectionsService currentInstance;
        if (instance == null) {
            synchronized (DirectionsService.class) {
                if (instance == null) {
                    instance = new DirectionsService();
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    /**
     * Fetch the directions
     *
     * @param origin Origin address
     * @param destination Destination address
     * @return Destinations
     */
    public List<String> getDirections(String origin, String destination, String language) {
        final List<String> responseToUser = new ArrayList<>();
        try {
            String completURL = BASEURL + "?origin=" + getQuery(origin) + "&destination=" +
                    getQuery(destination) + PARAMS.replace("@language@", language) + APIIDEND;
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
            Request request = new Request.Builder()
                    .url(completURL)
                    .header("charset", StandardCharsets.UTF_8.name())
                    .header("content-type", "application/json")
                    .get()
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            JSONObject jsonObject = new JSONObject(body.string());
                            if (jsonObject.getString("status").equals("OK")) {
                                JSONObject route = jsonObject.getJSONArray("routes").getJSONObject(0);
                                String startOfAddress = LocalisationService.getString("directionsInit", language);
                                String partialResponseToUser = String.format(startOfAddress,
                                        route.getJSONArray("legs").getJSONObject(0).getString("start_address"),
                                        route.getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getString("text"),
                                        route.getJSONArray("legs").getJSONObject(0).getString("end_address"),
                                        route.getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text")
                                );
                                responseToUser.add(partialResponseToUser);
                                responseToUser.addAll(getDirectionsSteps(
                                        route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps"), language));
                            } else {
                                responseToUser.add(LocalisationService.getString("directionsNotFound", language));
                            }
                        } else {
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error getting directions", e);
            responseToUser.add(LocalisationService.getString("errorFetchingDirections", language));
        }
        return responseToUser;
    }

    private String getQuery(String address) throws UnsupportedEncodingException {
        return URLEncoder.encode(address, "UTF-8");
    }

    private List<String> getDirectionsSteps(JSONArray steps, String language) {
        List<String> stepsStringify = new ArrayList<>();
        String partialStepsStringify = "";
        for (int i = 0; i < steps.length(); i++) {
            String step = getDirectionForStep(steps.getJSONObject(i), language);
            if (partialStepsStringify.length() > 1000) {
                stepsStringify.add(partialStepsStringify);
                partialStepsStringify = "";
            }
            partialStepsStringify += i + ".\t" + step + "\n\n";
        }
        if (!partialStepsStringify.isEmpty()) {
            stepsStringify.add(partialStepsStringify);
        }
        return stepsStringify;
    }

    private String getDirectionForStep(JSONObject jsonObject, String language) {
        String direction = LocalisationService.getString("directionsStep", language);
        String htmlIntructions = Jsoup.parse(jsonObject.getString("html_instructions")).text();
        String duration = jsonObject.getJSONObject("duration").getString("text");
        String distance = jsonObject.getJSONObject("distance").getString("text");

        direction = String.format(direction, htmlIntructions, duration, distance);

        return direction;
    }
}
