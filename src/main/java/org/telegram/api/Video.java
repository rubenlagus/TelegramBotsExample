package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a video file.
 * @date 20 of June of 2015
 */
public class Video {

    public static final String FILEID_FIELD = "file_id";
    @JsonProperty(FILEID_FIELD)
    private String fileId; ///< Unique identifier for this file
    public static final String WIDTH_FIELD = "width";
    @JsonProperty(WIDTH_FIELD)
    private Integer width; ///< Video width as defined by sender
    public static final String HEIGHT_FIELD = "height";
    @JsonProperty(HEIGHT_FIELD)
    private Integer height; ///< Video height as defined by sender
    public static final String DURATION_FIELD = "duration";
    @JsonProperty(DURATION_FIELD)
    private Integer duration; ///< Duration of the video in seconds as defined by sender
    public static final String THUMB_FIELD = "thumb";
    @JsonProperty(THUMB_FIELD)
    private PhotoSize thumb; ///< Video thumbnail
    public static final String MIMETYPE_FIELD = "mime_type";
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType; ///< Optional. Mime type of a file as defined by sender
    public static final String FILESIZE_FIELD = "file_size";
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize; ///< Optional. File size

    public Video() {
        super();
    }

    public Video(JSONObject jsonObject) {
        this.fileId = jsonObject.getString(FILEID_FIELD);
        this.width = jsonObject.getInt(WIDTH_FIELD);
        this.height = jsonObject.getInt(HEIGHT_FIELD);
        this.duration = jsonObject.getInt(DURATION_FIELD);
        if (jsonObject.has(THUMB_FIELD)) {
            this.thumb = new PhotoSize(jsonObject.getJSONObject(THUMB_FIELD));
        } else {
            this.thumb = null;
        }
        this.mimeType = jsonObject.optString(MIMETYPE_FIELD, "");
        this.fileSize = jsonObject.optInt(FILESIZE_FIELD, 0);
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public PhotoSize getThumb() {
        return thumb;
    }

    public void setThumb(PhotoSize thumb) {
        this.thumb = thumb;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
}
