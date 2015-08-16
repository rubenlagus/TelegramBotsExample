package org.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief This object represents a message.
 * @date 20 of June of 2015
 */
public class Message {
    public static final String MESSAGEID_FIELD = "message_id";
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId; ///< Integer	Unique message identifier
    public static final String FROM_FIELD = "from";
    @JsonProperty(FROM_FIELD)
    private User from; ///< Sender
    public static final String DATE_FIELD = "date";
    @JsonProperty(DATE_FIELD)
    private Integer date; ///< Date the message was sent in Unix time
    public static final String CHAT_FIELD = "chat";
    /**
     * Conversation the message belongs to in case of a private message (@see User)or
     * Conversation the message belongs to in case of a group (@see GroupChat)
     */
    @JsonProperty(CHAT_FIELD)
    private Chat chat; ///< Conversation the message belongs to in case of a private message
    public static final String FORWARDFROM_FIELD = "forward_from";
    @JsonProperty(FORWARDFROM_FIELD)
    private User forwardFrom; ///< Optional. For forwarded messages, sender of the original message
    public static final String FORWARDDATE_FIELD = "forward_date";
    @JsonProperty(FORWARDDATE_FIELD)
    private Integer forwardDate; ///< Optional. For forwarded messages, date the original message was sent
    public static final String TEXT_FIELD = "text";
    @JsonProperty(TEXT_FIELD)
    private String text; ///< Optional. For text messages, the actual UTF-8 text of the message
    public static final String AUDIO_FIELD = "audio";
    @JsonProperty(AUDIO_FIELD)
    private Audio audio; ///< Optional. Message is an audio file, information about the file
    public static final String DOCUMENT_FIELD = "document";
    @JsonProperty(DOCUMENT_FIELD)
    private Document document; ///< Optional. Message is a general file, information about the file
    public static final String PHOTO_FIELD = "photo";
    @JsonProperty(PHOTO_FIELD)
    private List<PhotoSize> photo; ///< Optional. Message is a photo, available sizes of the photo
    public static final String STICKER_FIELD = "sticker";
    @JsonProperty(STICKER_FIELD)
    private Sticker sticker; ///< Optional. Message is a sticker, information about the sticker
    public static final String VIDEO_FIELD = "video";
    @JsonProperty(VIDEO_FIELD)
    private Video video; ///< Optional. Message is a video, information about the video
    public static final String CONTACT_FIELD = "contact";
    @JsonProperty(CONTACT_FIELD)
    private Contact contact; ///< Optional. Message is a shared contact, information about the contact
    public static final String LOCATION_FIELD = "location";
    @JsonProperty(LOCATION_FIELD)
    private Location location; ///< Optional. Message is a shared location, information about the location
    public static final String NEWCHATPARTICIPANT_FIELD = "new_chat_participant";
    @JsonProperty(NEWCHATPARTICIPANT_FIELD)
    private User newChatParticipant; ///< Optional. A new member was added to the group, information about them (this member may be bot itself)
    public static final String LEFTCHATPARTICIPANT_FIELD = "left_chat_participant";
    @JsonProperty(LEFTCHATPARTICIPANT_FIELD)
    private User leftChatParticipant; ///< Optional. A member was removed from the group, information about them (this member may be bot itself)
    public static final String NEWCHATTITLE_FIELD = "new_chat_title";
    @JsonProperty(NEWCHATTITLE_FIELD)
    private String newChatTitle; ///< Optional. A group title was changed to this value
    public static final String NEWCHATPHOTO_FIELD = "new_chat_photo";
    @JsonProperty(NEWCHATPHOTO_FIELD)
    private List<PhotoSize> newChatPhoto; ///< Optional. A group photo was change to this value
    public static final String DELETECHATPHOTO_FIELD = "delete_chat_photo";
    @JsonProperty(DELETECHATPHOTO_FIELD)
    private Boolean deleteChatPhoto; ///< Optional. Informs that the group photo was deleted
    public static final String GROUPCHATCREATED_FIELD = "group_chat_created";
    @JsonProperty(GROUPCHATCREATED_FIELD)
    private Boolean groupchatCreated; ///< Optional. Informs that the group has been created
    public static final String REPLYTOMESSAGE_FIELD = "reply_to_message";
    @JsonProperty(REPLYTOMESSAGE_FIELD)
    private Message replyToMessage;
    public static final String VOICE_FIELD = "voice";
    @JsonProperty(VOICE_FIELD)
    private Voice voice; ///< Optional. Message is a voice message, information about the file

    public Message() {
        super();
    }

    public Message(JSONObject jsonObject) {
        super();
        this.messageId = jsonObject.getInt(MESSAGEID_FIELD);
        this.from = new User(jsonObject.getJSONObject(FROM_FIELD));
        this.date = jsonObject.getInt(DATE_FIELD);
        this.chat = new Chat(jsonObject.getJSONObject(CHAT_FIELD));
        if (jsonObject.has(FORWARDFROM_FIELD)) {
            this.forwardFrom = new User(jsonObject.getJSONObject(FORWARDFROM_FIELD));
        }
        if (jsonObject.has(FORWARDDATE_FIELD)) {
            this.forwardDate = jsonObject.getInt(FORWARDDATE_FIELD);
        }
        if (jsonObject.has(TEXT_FIELD)) {
            this.text = jsonObject.getString(TEXT_FIELD);
        }
        if (jsonObject.has(AUDIO_FIELD)) {
            this.audio = new Audio(jsonObject.getJSONObject(AUDIO_FIELD));
        }
        if (jsonObject.has(DOCUMENT_FIELD)) {
            this.document = new Document(jsonObject.getJSONObject(DOCUMENT_FIELD));
        }
        this.photo = new ArrayList<>();
        if (jsonObject.has(PHOTO_FIELD)) {
            JSONArray photos = jsonObject.getJSONArray(PHOTO_FIELD);
            for (int i = 0; i < photos.length(); i++) {
                this.photo.add(new PhotoSize(photos.getJSONObject(i)));
            }
        }
        if (jsonObject.has(STICKER_FIELD)) {
            this.sticker = new Sticker(jsonObject.getJSONObject(STICKER_FIELD));
        }
        if (jsonObject.has(VIDEO_FIELD)) {
            this.video = new Video(jsonObject.getJSONObject(VIDEO_FIELD));
        }
        if (jsonObject.has(CONTACT_FIELD)) {
            this.contact = new Contact(jsonObject.getJSONObject(CONTACT_FIELD));
        }
        if (jsonObject.has(LOCATION_FIELD)) {
            this.location = new Location(jsonObject.getJSONObject(LOCATION_FIELD));
        }
        if (jsonObject.has(VOICE_FIELD)) {
            this.voice = new Voice(jsonObject.getJSONObject(VOICE_FIELD));
        }
        if (jsonObject.has(NEWCHATPARTICIPANT_FIELD)) {
            this.newChatParticipant = new User(jsonObject.getJSONObject(NEWCHATPARTICIPANT_FIELD));
        }
        if (jsonObject.has(LEFTCHATPARTICIPANT_FIELD)) {
            this.leftChatParticipant = new User(jsonObject.getJSONObject(LEFTCHATPARTICIPANT_FIELD));
        }
        if (jsonObject.has(REPLYTOMESSAGE_FIELD)) {
            this.replyToMessage = new Message(jsonObject.getJSONObject(REPLYTOMESSAGE_FIELD));
        }
        this.newChatTitle = jsonObject.optString(NEWCHATTITLE_FIELD, "");
        if (jsonObject.has(NEWCHATPHOTO_FIELD)) {
            JSONArray photoArray = jsonObject.getJSONArray(NEWCHATPHOTO_FIELD);
            this.newChatPhoto = new ArrayList<>();
            for (int i = 0; i < photoArray.length(); i++) {
                this.newChatPhoto.add(new PhotoSize(photoArray.getJSONObject(i)));
            }
        }
        this.deleteChatPhoto = jsonObject.optBoolean(DELETECHATPHOTO_FIELD, false);
        this.groupchatCreated = jsonObject.optBoolean(GROUPCHATCREATED_FIELD, false);
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public Integer getDate() {
        return date;
    }

    public void setDate(Integer date) {
        this.date = date;
    }

    public boolean isGroupMessage() {
        return chat.isGroupChat();
    }

    public Integer getChatId() {
        return chat.getId();
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public User getForwardFrom() {
        return forwardFrom;
    }

    public void setForwardFrom(User forwardFrom) {
        this.forwardFrom = forwardFrom;
    }

    public Integer getForwardDate() {
        return forwardDate;
    }

    public void setForwardDate(Integer forwardDate) {
        this.forwardDate = forwardDate;
    }

    public boolean hasText() {
        return text != null;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Audio getAudio() {
        return audio;
    }

    public void setAudio(Audio audio) {
        this.audio = audio;
    }

    public boolean hasDocument() {
        return this.document != null;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public List<PhotoSize> getPhoto() {
        return photo;
    }

    public void setPhoto(List<PhotoSize> photo) {
        this.photo = photo;
    }

    public Sticker getSticker() {
        return sticker;
    }

    public void setSticker(Sticker sticker) {
        this.sticker = sticker;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public User getNewChatParticipant() {
        return newChatParticipant;
    }

    public void setNewChatParticipant(User newChatParticipant) {
        this.newChatParticipant = newChatParticipant;
    }

    public User getLeftChatParticipant() {
        return leftChatParticipant;
    }

    public void setLeftChatParticipant(User leftChatParticipant) {
        this.leftChatParticipant = leftChatParticipant;
    }

    public String getNewChatTitle() {
        return newChatTitle;
    }

    public void setNewChatTitle(String newChatTitle) {
        this.newChatTitle = newChatTitle;
    }

    public List<PhotoSize> getNewChatPhoto() {
        return newChatPhoto;
    }

    public void setNewChatPhoto(List<PhotoSize> newChatPhoto) {
        this.newChatPhoto = newChatPhoto;
    }

    public Boolean getDeleteChatPhoto() {
        return deleteChatPhoto;
    }

    public void setDeleteChatPhoto(Boolean deleteChatPhoto) {
        this.deleteChatPhoto = deleteChatPhoto;
    }

    public Boolean getGroupchatCreated() {
        return groupchatCreated;
    }

    public void setGroupchatCreated(Boolean groupchatCreated) {
        this.groupchatCreated = groupchatCreated;
    }

    public boolean hasReplayMessage() {
        return replyToMessage != null;
    }

    public Message getReplyToMessage() {
        return replyToMessage;
    }

    public void setReplyToMessage(Message replyToMessage) {
        this.replyToMessage = replyToMessage;
    }

    public boolean isReply() {
        return this.replyToMessage != null;
    }

    public boolean hasLocation() {
        return location != null;
    }

    public Voice getVoice() {
        return voice;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }
}
