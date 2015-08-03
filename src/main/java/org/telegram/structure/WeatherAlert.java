package org.telegram.structure;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Weather Alert representation
 * @date 23 of July of 2015
 */
public class WeatherAlert {
    private int id;
    private int userId;
    private int cityId;

    public WeatherAlert() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}
