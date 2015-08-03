package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a sticker.
 * @date 20 of June of 2015
 */
public class Sticker {

    public static final String FILEID_FIELD = "file_id";
    @JsonProperty(FILEID_FIELD)
    private String fileId; ///< Unique identifier for this file
    public static final String WIDTH_FIELD = "width";
    @JsonProperty(WIDTH_FIELD)
    private Integer width; ///< Sticker width
    public static final String HEIGHT_FIELD = "height";
    @JsonProperty(HEIGHT_FIELD)
    private Integer height; ///< Sticker height
    public static final String THUMB_FIELD = "thumb";
    @JsonProperty(THUMB_FIELD)
    private PhotoSize thumb; ///< Sticker thumbnail in .webp or .jpg format
    public static final String FILESIZE_FIELD = "file_size";
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize; ///< Optional. File size

    public Sticker() {
        super();
    }

    public Sticker(JSONObject jsonObject) {
        super();
        this.fileId = jsonObject.getString(FILEID_FIELD);
        this.width = jsonObject.getInt(WIDTH_FIELD);
        this.height = jsonObject.getInt(HEIGHT_FIELD);
        if (jsonObject.has(THUMB_FIELD)) {
            this.thumb = new PhotoSize(jsonObject.getJSONObject(THUMB_FIELD));
        } else {
            this.thumb = null;
        }
        this.fileSize = jsonObject.optInt(FILESIZE_FIELD, 0);
    }
}
