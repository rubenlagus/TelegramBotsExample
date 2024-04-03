package org.telegram.database;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * Strings to create database
 */
public class CreationStrings {
    public static final int version = 8;
    public static final String createVersionTable = "CREATE TABLE IF NOT EXISTS Versions(ID INTEGER PRIMARY KEY AUTO_INCREMENT, Version INTEGER);";
    public static final String insertCurrentVersion = "INSERT IGNORE INTO Versions (Version) VALUES(%d);";
    public static final String createFilesTable = "CREATE TABLE IF NOT EXISTS Files (fileId VARCHAR(100) PRIMARY KEY, userId BIGINT NOT NULL, caption TEXT NOT NULL)";
    public static final String createUsersForFilesTable = "CREATE TABLE IF NOT EXISTS FilesUsers (userId BIGINT PRIMARY KEY, status INTEGER NOT NULL DEFAULT 0)";
    public static final String createRecentWeatherTable = "CREATE TABLE IF NOT EXISTS RecentWeather (ID INTEGER PRIMARY KEY AUTO_INCREMENT, userId BIGINT NOT NULL, " +
            "date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, cityId INTEGER NOT NULL, cityName VARCHAR(60) NOT NULL," +
            "CONSTRAINT unique_cistyuser UNIQUE (userId,cityId))";
    public static final String createDirectionsDatabase = "CREATE TABLE IF NOT EXISTS Directions (userId BIGINT PRIMARY KEY, status INTEGER NOT NULL, " +
            "messageId INTEGER NOT NULL DEFAULT 0, origin VARCHAR(100));";
    public static final String createLastUpdateDatabase = "CREATE TABLE IF NOT EXISTS LastUpdate (token VARCHAR(125) PRIMARY KEY, updateId INTEGER NOT NULL DEFAULT -1);";
    public static final String createUserLanguageDatabase = "CREATE TABLE IF NOT EXISTS UserLanguage (userId BIGINT PRIMARY KEY, languageCode VARCHAR(10) NOT NULL)";
    public static final String createUserWeatherOptionDatabase = "CREATE TABLE IF NOT EXISTS UserWeatherOptions (userId BIGINT PRIMARY KEY, languageCode VARCHAR(10) NOT NULL DEFAULT 'en', " +
            "units VARCHAR(10) NOT NULL DEFAULT 'metric')";
    public static final String createWeatherStateTable = "CREATE TABLE IF NOT EXISTS WeatherState (userId BIGINT NOT NULL, chatId BIGINT NOT NULL, state INTEGER NOT NULL DEFAULT 0, " +
            "languageCode VARCHAR(10) NOT NULL DEFAULT 'en', " +
            "CONSTRAINT `watherPrimaryKey` PRIMARY KEY(userId,chatId));";
    public static final String createWeatherAlertTable = "CREATE TABLE IF NOT EXISTS WeatherAlert (id INTEGER PRIMARY KEY AUTO_INCREMENT, userId BIGINT NOT NULL, cityId INTEGER NOT NULL, " +
            "cityName VARCHAR(60) NOT NULL, time INTEGER NOT NULL DEFAULT -1, CONSTRAINT unique_cityNameAlert UNIQUE (userId, cityName)," +
            "CONSTRAINT unique_cityIdAlert UNIQUE (userId, cityId));";

    public static final String CREATE_COMMANDS_TABLE = "CREATE TABLE IF NOT EXISTS CommandUsers (userId BIGINT PRIMARY KEY, status INTEGER NOT NULL);";
}
