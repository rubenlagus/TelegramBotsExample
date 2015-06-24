package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a point on the map.
 * @date 20 of June of 2015
 */
public class Location {

    public static final String LONGITUDE_FIELD = "longitude";
    @JsonProperty(LONGITUDE_FIELD)
    private Double longitude; ///< Longitude as defined by sender
    public static final String LATITUDE_FIELD = "latitude";
    @JsonProperty(LATITUDE_FIELD)
    private Double latitude; ///< Latitude as defined by sender

    public Location() {
        super();
    }

    public Location(JSONObject jsonObject) {
        super();
        this.longitude = jsonObject.getDouble(LONGITUDE_FIELD);
        this.latitude = jsonObject.getDouble(LATITUDE_FIELD);
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
