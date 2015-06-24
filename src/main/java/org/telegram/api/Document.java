package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a general file (as opposed to photos and audio files).
 * Telegram users can send files of any type of up to 1.5 GB in size.
 * @date 20 of June of 2015
 */
public class Document {

    public static final String FILEID_FIELD = "file_id";
    @JsonProperty(FILEID_FIELD)
    private String fileId; ///< Unique identifier for this file
    public static final String THUMB_FIELD = "thumb";
    @JsonProperty(THUMB_FIELD)
    private PhotoSize thumb; ///< Document thumbnail as defined by sender
    public static final String FILENAME_FIELD = "file_name";
    @JsonProperty(FILENAME_FIELD)
    private String fileName; ///< Optional. Original filename as defined by sender
    public static final String MIMETYPE_FIELD = "mime_type";
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType; ///< Optional. Mime type of a file as defined by sender
    public static final String FILESIZE_FIELD = "file_size";
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize; ///< Optional. File size

    public Document() {
        super();
    }

    public Document(JSONObject jsonObject) {
        this.fileId = jsonObject.getString(FILEID_FIELD);
        this.thumb = new PhotoSize(jsonObject.getJSONObject(THUMB_FIELD));
        this.fileName = jsonObject.optString(FILENAME_FIELD, "");
        this.mimeType = jsonObject.optString(MIMETYPE_FIELD, "");
        this.fileSize = jsonObject.optInt(FILESIZE_FIELD, 0);
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public PhotoSize getThumb() {
        return thumb;
    }

    public void setThumb(PhotoSize thumb) {
        this.thumb = thumb;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
