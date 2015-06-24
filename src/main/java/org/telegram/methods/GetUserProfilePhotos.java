package org.telegram.methods;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Use this method to get a list of profile pictures for a user. Returns a UserProfilePhotos object.
 * @date 20 of June of 2015
 */
public class GetUserProfilePhotos {
    public static final String PATH = "getuserprofilephotos";
/*
user_id	Integer	Yes
offset	Integer
limit	Integer	Optional
 */
    public static final String USERID_FIELD = "user_id";
    private Integer userId; ///< Unique identifier of the target user
    public static final String OFFSET_FIELD = "offset";
    /**
     * Sequential number of the first photo to be returned. By default, all photos are returned.
     */
    private Integer offset;
    public static final String LIMIT_FIELD = "limit";
    /**
     * Limits the number of photos to be retrieved. Values between 1—100 are accepted. Defaults to 100.
     */
    private Integer limit;

    public GetUserProfilePhotos() {
        super();
    }


    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
