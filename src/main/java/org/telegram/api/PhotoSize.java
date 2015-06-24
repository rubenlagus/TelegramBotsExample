package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents one size of a photo or a file / sticker thumbnail.
 * @date 20 of June of 2015
 */
public class PhotoSize {

    public static final String FILEID_FIELD = "file_id";
    @JsonProperty(FILEID_FIELD)
    private String fileId; ///< Unique identifier for this file
    public static final String WIDTH_FIELD = "width";
    @JsonProperty(WIDTH_FIELD)
    private Integer width; ///< Photo width
    public static final String HEIGHT_FIELD = "height";
    @JsonProperty(HEIGHT_FIELD)
    private Integer height; ///< Photo height
    public static final String FILESIZE_FIELD = "file_size";
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize; ///< Optional. File size

    public PhotoSize() {
        super();
    }

    public PhotoSize(JSONObject jsonObject) {
        super();
        this.fileId = jsonObject.optString(FILEID_FIELD, "");
        this.width = jsonObject.optInt(WIDTH_FIELD, 0);
        this.height = jsonObject.optInt(HEIGHT_FIELD, 0);
        this.fileSize = jsonObject.optInt(FILESIZE_FIELD, 0);
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
}
