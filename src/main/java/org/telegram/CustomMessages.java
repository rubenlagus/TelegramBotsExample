package org.telegram;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Custom messages to be sent to the user
 * @date 21 of June of 2015
 */
public class CustomMessages {
    public static final String helpWeather = "Curious about the weather?\nJust send me these commands and you'll know a lot better.\n\n" +
            "|-- " + Commands.WEATHERCOMMAND + " CITY,COUNTRY : Get a 3-day weather forecast for a city.\n" +
            "|-- " + Commands.CURRENTWEATHERCOMMAND + " CITY,COUNTRY : Get the current weather of a city.\n" +
            "|-- Send a location to get the forecast for it.";
    public static final String helpTransifex = "Tricks with words is the game that I play, give it a shot, I might make your day.\n\n" +
	    "To get the latest Telegram localization file for a language: \n" +
            "|-- " + Commands.transifexiOSCommand + " LANG_CODE : Get the latest iOS language.\n" +
            "|-- " + Commands.transifexAndroidCommand + " LANG_CODE : Get the latest android language.\n" +
            "|-- " + Commands.transifexWebogram + " LANG_CODE : Get the latest webogram language.\n" +
            "|-- " + Commands.transifexiOSCommand + " LANG_CODE : Get the latest iOS language.\n" +
            "|-- " + Commands.transifexAndroidCommand + " LANG_CODE : Get the latest android language.\n" +
            "|-- " + Commands.transifexWebogram + " LANG_CODE : Get the latest webogram language.\n" +
            "|-- " + Commands.transifexTDesktop + " LANG_CODE : Get the latest Tdesktop language.\n" +
            "|-- " + Commands.transifexOSX + " LANG_CODE : Get the latest OSX-App language.\n" +
            "|-- " + Commands.transifexWP + " LANG_CODE : Get the latest Windows Phone language.\n\n" +
            "2. To get an updated localization file for your Android beta-app: \n" +
            "|-- " + Commands.transifexAndroidSupportCommand + " LANG_CODE : Get the latest Android-beta language.\n\n";
    public static final String helpFiles = "Leaving a file for some others to find? Just dock your boat here and a bay comes to mind.\n\n"+
	    "Share files through a custom link: \n" +
            "|-- " + Commands.startCommand + " FILEID : Get a file by id.\n" +
            "|-- " + Commands.uploadCommand + " : Start your file upload.\n" +
            "|-- " + Commands.deleteCommand + " : Choose one of your files to delete it.\n" +
            "|-- " + Commands.listCommand + " : Show a list of your shared files.\n\n";
    public static final String helpDirections = "The road ahead, paved with good intentions, the right path ahead however is what I tend to mention.\n\n" +
	    "To get directions between two locations: \n" +
            "|-- " + Commands.startDirectionCommand + " : Start to get directions\n";

    public static final String sendFileToUpload = "Please send me a file you want to share. Make sure you attach it as file, not as an image or video.";
    public static final String fileUploaded = "Great, your file has been uploaded. Send this link to anyone you want and they will be able to download the file:\n\n";
    public static final String deleteUploadedFile = "Please select the file you want to delete:";
    public static final String fileDeleted = "The file was deleted";
    public static final String wrongFileId = "Sorry, we can't find a file with that ID. Either a typo was made or it was deleted already.";
    public static final String listOfFiles = "This your currently shared files list:";
    public static final String noFiles = "You haven't shared any file yet.";
    public static final String processFinished = "The current process was cancelled.";
    public static final String uploadedFileURL = "https://telegram.me/filesbot?start=";
    public static final String chooseFromRecentWeather = "Please choose an option from your recent requests:";

    public static final String initDirections = "Please reply with your departing location.";
    public static final String sendDestination = "Please reply with your destination.";
    public static final String youNeedReplyDirections = "I'm sorry, I can't help you unless you reply to the message I sent you.";
    public static final String pleaseSendMeCityWeather = "Send me the city and country you are interested in, use this format: CITY,COUNTRY";
}
