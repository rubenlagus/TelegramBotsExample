package org.telegram.structure;

import lombok.Data;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Weather Alert representation
 */
@Data
public class WeatherAlert {
    private int id;
    private long userId;
    private int cityId;
}
