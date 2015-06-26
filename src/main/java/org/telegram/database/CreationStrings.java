package org.telegram.database;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * @brief Strings to create database
 * @date 15 of May of 2015
 */
public class CreationStrings {
    public static final int version = 3;
    public static final String createVersionTable = "CREATE TABLE IF NOT EXISTS Versions(ID INTEGER PRIMARY KEY AUTO_INCREMENT, Version INTEGER);";
    public static final String insertCurrentVersion = "INSERT IGNORE INTO Versions (Version) VALUES(%d);";
    public static final String createFilesTable = "CREATE TABLE IF NOT EXISTS Files (fileId VARCHAR(100) PRIMARY KEY, userId INTEGER NOT NULL, caption TEXT NOT NULL)";
    public static final String createUsersForFilesTable = "CREATE TABLE IF NOT EXISTS FilesUsers (userId INTEGER PRIMARY KEY, status INTEGER NOT NULL DEFAULT 0)";
    public static final String createRecentWeatherTable = "CREATE TABLE IF NOT EXISTS RecentWeather (ID INTEGER PRIMARY KEY AUTO_INCREMENT, userId INTEGER NOT NULL, " +
            "date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, cityId INTEGER NOT NULL, cityName VARCHAR(60) NOT NULL," +
            "CONSTRAINT unique_cistyuser UNIQUE (userId,cityId))";
    public static final String createDirectionsDatabase = "CREATE TABLE IF NOT EXISTS Directions (userId INTEGER PRIMARY KEY, status INTEGER NOT NULL, " +
            "messageId INTEGER NOT NULL DEFAULT 0, origin VARCHAR(100));";
    public static final String createLastUpdateDatabase = "CREATE TABLE IF NOT EXISTS LastUpdate (token STRING PRIMARY KEY, updateId INTEGER NOT NULL DEFAULT -1);";


    /*public static final String createOperatingSystemsVersion = "CREATE TABLE IF NOT EXISTS OperatingSystems (operatingSystem VARCHAR(20) PRIMARY KEY);";
    public static final String createLanguagesTable = "CREATE TABLE IF NOT EXISTS Languages(languageCode VARCHAR(6) PRIMARY KEY, languageName VARCHAR(30) NOT NULL);";
    public static final String insertEnglishLanguage = "INSERT IGNORE INTO Languages (languageCode, languageName) VALUES('en', 'English');";
    public static final String insertPortugueseLanguage = "INSERT IGNORE INTO Languages (languageCode, languageName) VALUES('pt', 'Portuguese');";
    public static final String insertItalianLanguage = "INSERT IGNORE INTO Languages (languageCode, languageName) VALUES('it', 'Italian');";
    public static final String createCountriesTable = "CREATE TABLE IF NOT EXISTS Countries(countryCode VARCHAR(6) PRIMARY KEY, name VARCHAR(20) NOT NULL);";
    public static final String createLoginTable = "CREATE TABLE IF NOT EXISTS Logins(username VARCHAR(30) PRIMARY KEY, password VARCHAR(30) NOT NULL, " +
            "hash VARCHAR(130) NOT NULL, securityLevel INTEGER DEFAULT 0);";
    public static final String createLanguagesPerCountryTable = "CREATE TABLE IF NOT EXISTS LanguagesPerCountry( countryCode VARCHAR(6) PRIMARY KEY, firstLanguage VARCHAR(6) NOT NULL, " +
            "secondLanguage VARCHAR(6) DEFAULT NULL, FOREIGN KEY (firstLanguage) REFERENCES Languages (languageCode) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "FOREIGN KEY (secondLanguage) REFERENCES Languages (languageCode) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String createUsersTable = "CREATE TABLE IF NOT EXISTS Users(userId INTEGER NOT NULL, bot INTEGER NOT NULL, needVolunteer BOOLEAN NOT NULL, phone VARCHAR(15) DEFAULT NULL, firstName VARCHAR(100) NOT NULL, " +
            "lastName VARCHAR(100) NOT NULL, userName VARCHAR(100) DEFAULT NULL, isContact BOOLEAN NOT NULL, lastAnswer DATETIME, userHash VARCHAR(40) NOT NULL, lastScore FLOAT, hashtag VARCHAR(15), countryCode VARCHAR(6), denied INTEGER NOT NULL DEFAULT 0, " +
            "customCommand VARCHAR(60) DEFAULT NULL, deleted BOOLEAN NOT NULL DEFAULT FALSE, " +
            "CONSTRAINT `usersCountryCode` FOREIGN KEY (countryCode) REFERENCES Countries(countryCode) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `userPrimaryKey` PRIMARY KEY(userId,bot));";
    public static final String createAnswersTable = "CREATE TABLE IF NOT EXISTS AnswersXXXX (ID INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(250) NOT NULL," +
            "content VARCHAR(2000) NOT NULL, operatingSystem VARCHAR(20) NOT NULL, appVersion VARCHAR(10)," +
            "CONSTRAINT `answersOSXXXX` FOREIGN KEY (operatingSystem) REFERENCES OperatingSystems(operatingSystem) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String insertEmptyAnswer = "INSERT IGNORE INTO AnswersXXXX (name, content, operatingSystem, appVersion, ID) VALUES ('Empty Answer','','All','',100);";
    public static final String insertNotFoundAnswer = "INSERT IGNORE INTO AnswersXXXX (name, content, operatingSystem, appVersion, ID) VALUES ('Not found','','All','',99);";
    public static final String createHistoryTable = "CREATE TABLE IF NOT EXISTS History( ID INTEGER PRIMARY KEY AUTO_INCREMENT, userId INTEGER NOT NULL, date DATETIME NOT NULL,  answerId INTEGER NOT NULL, " +
            "languageCode VARCHAR(6) NOT NULL, CONSTRAINT `historyUserId` FOREIGN KEY (userId) REFERENCES Users(userId) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `historyLanguageCode` FOREIGN KEY (languageCode) REFERENCES Languages(languageCode) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String createQuestionsTable = "CREATE TABLE IF NOT EXISTS QuestionsXXXX (ID INTEGER PRIMARY KEY AUTO_INCREMENT, content VARCHAR(300) NOT NULL, " +
            "languageCode VARCHAR(6) NOT NULL, answerAll INTEGER DEFAULT NULL, answerIos INTEGER DEFAULT NULL, answerWebogram INTEGER DEFAULT NULL, answerAndroid INTEGER DEFAULT NULL, " +
            "answerOsx INTEGER DEFAULT NULL, answerDesktop INTEGER DEFAULT NULL, answerWP INTEGER DEFAULT NULL, " +
            "CONSTRAINT `questionsAnswerAllXXXX` FOREIGN KEY (answerAll) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerIosXXXX` FOREIGN KEY (answerIos) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerWebogramXXXX` FOREIGN KEY (answerWebogram) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerAndroidXXXX` FOREIGN KEY (answerAndroid) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerOsxXXXX` FOREIGN KEY (answerOsx) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerDesktopXXXX` FOREIGN KEY (answerDesktop) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `questionsAnswerWPXXXX` FOREIGN KEY (answerWP) REFERENCES AnswersXXXX(ID) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String createTrelloTable = "CREATE TABLE IF NOT EXISTS trello (shortLink VARCHAR(10) PRIMARY KEY NOT NULL, listName VARCHAR(20) NOT NULL, listId VARCHAR(30), " +
            "closed BOOLEAN NOT NULL, creationDate DATETIME NOT NULL, notificationDate DATETIME NOT NULL, affectedUserCount INTEGER NOT NULL DEFAULT 0, tittle VARCHAR(100) NOT NULL, " +
            "description TEXT NOT NULL);";
    public static final String createTemplatesTable = "CREATE TABLE IF NOT EXISTS Templates (id INTEGER PRIMARY KEY AUTO_INCREMENT, hash VARCHAR(130) NOT NULL, " +
            "languageCode VARCHAR(8), content TEXT);";
    public static final String createTemplatesLogTable = "CREATE TABLE IF NOT EXISTS TemplatesLog (id INTEGER PRIMARY KEY AUTO_INCREMENT, hash VARCHAR(130) NOT NULL, type INTEGER, content TEXT);";
    public static final String createTemplatesFileTable = "CREATE TABLE IF NOT EXISTS TemplatesFile (id INTEGER PRIMARY KEY AUTO_INCREMENT, hash VARCHAR(130) NOT NULL, date DATETIME, languageCode VARCHAR(8)," +
            " CONSTRAINT unique_filelanguage UNIQUE (hash,languageCode));";
    public static final String createFiltersTable = "create table if not exists Filters (filterId INTEGER PRIMARY KEY AUTO_INCREMENT," +
            " words VARCHAR(300) NOT NULL, languageCode VARCHAR(6) NOT NULL," +
            " CONSTRAINT `filtersLanguageCode` FOREIGN KEY (languageCode) REFERENCES Languages(languageCode) ON UPDATE CASCADE ON DELETE CASCADE" +
            ");";
    public static final String createMessagesDiscardedTable = "create table if not exists MessagesDiscarded (id INTEGER PRIMARY KEY AUTO_INCREMENT," +
            " userId INTEGER NOT NULL, filterId INTEGER NOT NULL, message TEXT NOT NULL, date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            " partial BOOLEAN NOT NULL DEFAULT false, CONSTRAINT `messagesDiscardedUserId` FOREIGN KEY (userId) REFERENCES Users(userId) ON UPDATE CASCADE ON DELETE CASCADE," +
            " CONSTRAINT `messageDiscardedFilterId` FOREIGN KEY (filterId) REFERENCES Filters(filterId) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String createMessagesAnsweredTable = "create table if not exists MessagesAnswered (id INTEGER PRIMARY KEY AUTO_INCREMENT," +
            " userId INTEGER NOT NULL, answer INTEGER NOT NULL, message TEXT NOT NULL, matchValue FLOAT NOT NULL, date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            " partial BOOLEAN NOT NULL DEFAULT false, CONSTRAINT `messagesAnswerdUserId` FOREIGN KEY (userId) REFERENCES Users(userId) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static final String createChatListTable = "create table if not exists Chats (chatId INTEGER PRIMARY KEY NOT NULL, " +
            "title VARCHAR(50) NOT NULL);";
    public static final String createChatsStatsTable = "create table if not exists ChatStats (id INTEGER PRIMARY KEY AUTO_INCREMENT," +
            " userId INTEGER NOT NULL, chatId INTEGER NOT NULL, date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            " forwarded BOOLEAN NOT NULL DEFAULT false, type INTEGER NOT NULL," +
            "CONSTRAINT `chatsStatsUserId` FOREIGN KEY (userId) REFERENCES Users(userId) ON UPDATE CASCADE ON DELETE CASCADE, " +
            "CONSTRAINT `chatsStatsChatId` FOREIGN KEY (chatId) REFERENCES Chats(chatId) ON UPDATE CASCADE ON DELETE CASCADE);";
    public static volatile List<String> operatingSystems = new ArrayList<>();

    static {
        operatingSystems.add("All");
        operatingSystems.add("iOS");
        operatingSystems.add("Webogram");
        operatingSystems.add("Android");
        operatingSystems.add("OSX");
        operatingSystems.add("Desktop");
        operatingSystems.add("WP");
    }*/
}
