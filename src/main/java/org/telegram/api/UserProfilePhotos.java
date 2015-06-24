package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represent a user's profile pictures.
 * @date 22 of June of 2015
 */
public class UserProfilePhotos {

    public static final String TOTALCOUNT_FIELD = "total_count";
    @JsonProperty(TOTALCOUNT_FIELD)
    private Integer totalCount; ///< Total number of profile pictures the target user has
    public static final String PHOTOS_FIELD = "photos";
    @JsonProperty(PHOTOS_FIELD)
    private List<List<PhotoSize>> photos; ///< Requested profile pictures (in up to 4 sizes each)

    public UserProfilePhotos() {
        super();
    }

    public UserProfilePhotos(JSONObject jsonObject) {
        super();
        this.totalCount = jsonObject.getInt(TOTALCOUNT_FIELD);
        this.photos = new ArrayList<>();
        JSONArray photos = jsonObject.getJSONArray(PHOTOS_FIELD);
        for (int i = 0; i < photos.length(); i++) {
            JSONArray innerArray = photos.getJSONArray(i);
            List<PhotoSize> innerPhotos = new ArrayList<>();
            for (int j = 0; j < innerArray.length(); j ++) {
                innerPhotos.add(new PhotoSize(innerArray.getJSONObject(j)));
            }
            this.photos.add(innerPhotos);
        }
    }
}
