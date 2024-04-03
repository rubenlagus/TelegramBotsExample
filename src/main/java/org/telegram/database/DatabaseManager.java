/*
 * This is the source code of Telegram Bot v. 2.0
 * It is licensed under GNU GPL v. 3 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Ruben Bermudez, 3/12/14.
 */
package org.telegram.database;

import lombok.extern.slf4j.Slf4j;
import org.telegram.structure.WeatherAlert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * Database Manager to perform database operations
 */
@Slf4j
public class DatabaseManager {
    private static volatile DatabaseManager instance;
    private static volatile ConnectionDB connetion;

    /**
     * Private constructor (due to Singleton)
     */
    private DatabaseManager() {
        connetion = new ConnectionDB();
        final int currentVersion = connetion.checkVersion();
        log.info("Current db version: " + currentVersion);
        if (currentVersion < CreationStrings.version) {
            recreateTable(currentVersion);
        }
    }

    /**
     * Get Singleton instance
     *
     * @return instance of the class
     */
    public static DatabaseManager getInstance() {
        final DatabaseManager currentInstance;
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    /**
     * Recreates the DB
     */
    private void recreateTable(int currentVersion) {
        try {
            connetion.initTransaction();
            if (currentVersion == 0) {
                currentVersion = createNewTables();
            }
            if (currentVersion == 1) {
                currentVersion = updateToVersion2();
            }
            if (currentVersion == 2) {
                currentVersion = updateToVersion3();
            }
            if (currentVersion == 3) {
                currentVersion = updateToVersion4();
            }
            if (currentVersion == 4) {
                currentVersion = updateToVersion5();
            }
            if (currentVersion == 5) {
                currentVersion = updateToVersion6();
            }
            if (currentVersion == 6) {
                currentVersion = updateToVersion7();
            }
            if (currentVersion == 7) {
                currentVersion = updateToVersion8();
            }
            if (currentVersion == 8) {
                currentVersion = updateToVersion8();
            }
            connetion.commitTransaction();
        } catch (SQLException e) {
            log.error("Error updating DB", e);
            try {
                connetion.rollbackTransaction();
            } catch (SQLException ex) {
                log.error("Error rollingback the transaction", ex);
            }
        }
    }

    private int updateToVersion2() throws SQLException {
        connetion.executeQuery(CreationStrings.createRecentWeatherTable);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 2));
        return 2;
    }

    private int updateToVersion3() throws SQLException {
        connetion.executeQuery(CreationStrings.createDirectionsDatabase);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 3));
        return 3;
    }

    private int updateToVersion4() throws SQLException {
        connetion.executeQuery(CreationStrings.createLastUpdateDatabase);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 4));
        return 4;
    }

    private int updateToVersion5() throws SQLException {
        connetion.executeQuery(CreationStrings.createUserLanguageDatabase);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 5));
        return 5;
    }

    private int updateToVersion6() throws SQLException {
        connetion.executeQuery(CreationStrings.createWeatherStateTable);
        connetion.executeQuery(CreationStrings.createUserWeatherOptionDatabase);
        connetion.executeQuery(CreationStrings.createWeatherAlertTable);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 6));
        return 6;
    }

    private int updateToVersion7() throws SQLException {
        connetion.executeQuery("ALTER TABLE WeatherState MODIFY chatId BIGINT NOT NULL");
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 7));
        return 7;
    }

    private int updateToVersion8() throws SQLException {
        connetion.executeQuery(CreationStrings.CREATE_COMMANDS_TABLE);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 8));
        return 8;
    }

    private int updateToVersion9() throws SQLException {
        connetion.executeQuery("ALTER TABLE Files MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE FilesUsers MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE RecentWeather MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE Directions MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE UserLanguage MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE UserWeatherOptions MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE WeatherState MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE WeatherAlert MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery("ALTER TABLE CommandUsers MODIFY COLUMN userId BIGINT;");
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 9));
        return 8;
    }

    private int createNewTables() throws SQLException {
        connetion.executeQuery(CreationStrings.createVersionTable);
        connetion.executeQuery(CreationStrings.createFilesTable);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, CreationStrings.version));
        connetion.executeQuery(CreationStrings.createUsersForFilesTable);
        connetion.executeQuery(CreationStrings.createRecentWeatherTable);
        connetion.executeQuery(CreationStrings.createDirectionsDatabase);
        connetion.executeQuery(CreationStrings.createUserLanguageDatabase);
        connetion.executeQuery(CreationStrings.createWeatherStateTable);
        connetion.executeQuery(CreationStrings.createUserWeatherOptionDatabase);
        connetion.executeQuery(CreationStrings.createWeatherAlertTable);
        connetion.executeQuery(CreationStrings.CREATE_COMMANDS_TABLE);
        return CreationStrings.version;
    }

    public boolean setUserStateForCommandsBot(Long userId, boolean active) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("INSERT INTO CommandUsers (userId, status) VALUES(?, ?) ON DUPLICATE KEY UPDATE status=?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, active ? 1 : 0);
            preparedStatement.setInt(3, active ? 1 : 0);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting user state for commands bot", e);
        }
        return updatedRows > 0;
    }

    public boolean getUserStateForCommandsBot(Long userId) {
        int status = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("Select status FROM CommandUsers WHERE userId=?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                status = result.getInt("status");
            }
        } catch (SQLException e) {
            log.error("Error getting user state for command bot", e);
        }
        return status == 1;
    }

    public boolean addFile(String fileId, Long userId, String caption) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO Files (fileId, userId, caption) VALUES(?, ?, ?)");
            preparedStatement.setString(1, fileId);
            preparedStatement.setLong(2, userId);
            preparedStatement.setString(3, caption);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error getting file", e);
        }
        return updatedRows > 0;
    }

    public HashMap<String, String> getFilesByUser(Long userId) {
        HashMap<String, String> files = new HashMap<>();
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT * FROM Files WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                files.put(result.getString("fileId"), result.getString("caption"));
            }
            result.close();
        } catch (SQLException e) {
            log.error("Error getting files for user", e);
        }
        return files;
    }

    public boolean addUserForFile(Long userId, int status) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO FilesUsers (userId, status) VALUES(?, ?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, status);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error getting user for file", e);
        }
        return updatedRows > 0;
    }

    public boolean deleteUserForFile(Long userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM FilesUsers WHERE userId=?;");
            preparedStatement.setLong(1, userId);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting user file", e);
        }
        return updatedRows > 0;
    }

    public int getUserStatusForFile(Long userId) {
        int status = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("Select status FROM FilesUsers WHERE userId=?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                status = result.getInt("status");
            }
        } catch (SQLException e) {
            log.error("Error getting user status", e);
        }
        return status;
    }

    public boolean doesFileExists(String fileId) {
        boolean exists = false;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("Select fileID FROM Files WHERE fileId=?");
            preparedStatement.setString(1, fileId);
            final ResultSet result = preparedStatement.executeQuery();
            exists = result.next();
        } catch (SQLException e) {
            log.error("Error checking file", e);
        }
        return exists;
    }

    public boolean deleteFile(String fileId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM Files WHERE fileId=?;");
            preparedStatement.setString(1, fileId);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting files", e);
        }
        return updatedRows > 0;
    }

    public boolean addRecentWeather(Long userId, Integer cityId, String cityName) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO RecentWeather (userId, cityId, cityName) VALUES(?, ?, ?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, cityId);
            preparedStatement.setString(3, cityName);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding recent weather", e);
        }
        cleanUpRecent(userId);
        return updatedRows > 0;
    }

    public List<String> getRecentWeather(Long userId) {
        List<String> recentWeather = new ArrayList<>();
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("select * FROM RecentWeather WHERE userId=? order by date desc");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                recentWeather.add(result.getString("cityName"));
            }
        } catch (SQLException e) {
            log.error("Error getting recent weather", e);
        }

        return recentWeather;
    }

    private void cleanUpRecent(Long userId) {
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM RecentWeather WHERE userid = ? AND ID <= (SELECT ID FROM (SELECT id From RecentWeather where userId = ? ORDER BY id DESC LIMIT 1 OFFSET 4 ) AS T1 )");
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error cleaning up recent user", e);
        }
    }

    public boolean addUserForDirection(Long userId, int status, int messageId, String origin) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO Directions (userId, status, messageId, origin) VALUES(?, ?, ?, ?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, status);
            preparedStatement.setInt(3, messageId);
            if (origin == null || origin.isEmpty()) {
                preparedStatement.setNull(4, Types.VARCHAR);
            } else {
                preparedStatement.setString(4, origin);
            }
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding user for direction", e);
        }
        return updatedRows > 0;
    }

    public int getUserDestinationStatus(Long userId) {
        int status = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT status FROM Directions WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                status = result.getInt("status");
            }
        } catch (SQLException e) {
            log.error("Error getting user destination status", e);
        }
        return status;
    }

    public int getUserDestinationMessageId(Long userId) {
        int messageId = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT messageId FROM Directions WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                messageId = result.getInt("messageId");
            }
        } catch (SQLException e) {
            log.error("Error getting user destination message id", e);
        }
        return messageId;
    }

    public String getUserOrigin(Long userId) {
        String origin = "";
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT origin FROM Directions WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                origin = result.getString("origin");
            }
        } catch (SQLException e) {
            log.error("Error get user origin", e);
        }
        return origin;
    }

    public boolean deleteUserForDirections(Long userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM Directions WHERE userId=?;");
            preparedStatement.setLong(1, userId);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting user directions", e);
        }
        return updatedRows > 0;
    }

    public boolean putLastUpdate(String token, Integer updateId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO LastUpdate (token, updateId) VALUES(?, ?)");
            preparedStatement.setString(1, token);
            preparedStatement.setInt(2, updateId);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding last update", e);
        }
        return updatedRows > 0;
    }

    public Integer getLastUpdate(String token) {
        Integer updateId = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT updateId FROM LastUpdate WHERE token = ?");
            preparedStatement.setString(1, token);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                updateId = result.getInt("updateId");
            }
        } catch (SQLException e) {
            log.error("Error getting last update", e);
        }
        return updateId;
    }

    public String getUserLanguage(Long userId) {
        String languageCode = "en";
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT languageCode FROM UserLanguage WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                languageCode = result.getString("languageCode");
            }
        } catch (SQLException e) {
            log.error("Error getting user language", e);
        }
        return languageCode;
    }

    public boolean putUserLanguage(Long userId, String language) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO UserLanguage (userId, languageCode) VALUES(?, ?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, language);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating user language", e);
        }
        return updatedRows > 0;
    }

    public int getWeatherState(Long userId, Long chatId) {
        int state = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT state FROM WeatherState WHERE userId = ? AND chatId = ?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, chatId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                state = result.getInt("state");
            }
        } catch (SQLException e) {
            log.error("Error getting weather state", e);
        }
        return state;
    }

    public boolean insertWeatherState(Long userId, Long chatId, int state) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO WeatherState (userId, chatId, state) VALUES (?, ?, ?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, chatId);
            preparedStatement.setInt(3, state);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error insert weather state", e);
        }
        return updatedRows > 0;
    }

    public Integer getRecentWeatherIdByCity(Long userId, String city) {
        Integer cityId = null;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("select cityId FROM RecentWeather WHERE userId=? AND cityName=?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, city);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                cityId = result.getInt("cityId");
            }
        } catch (SQLException e) {
            log.error("Error getting recent weather by city", e);
        }

        return cityId;
    }

    public String[] getUserWeatherOptions(Long userId) {
        String[] options = new String[] {"en", "metric"};
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT * FROM UserWeatherOptions WHERE userId = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                options[0] = result.getString("languageCode");
                options[1] = result.getString("units");
            } else {
                addNewUserWeatherOptions(userId);
            }
        } catch (SQLException e) {
            log.error("Error getting wether options", e);
        }
        return options;
    }

    private boolean addNewUserWeatherOptions(Long userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("INSERT INTO UserWeatherOptions (userId) VALUES (?)");
            preparedStatement.setLong(1, userId);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding new user weather options", e);
        }
        return updatedRows > 0;
    }

    public boolean putUserWeatherLanguageOption(Long userId, String language) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("UPDATE UserWeatherOptions SET languageCode = ? WHERE userId = ?");
            preparedStatement.setString(1, language);
            preparedStatement.setLong(2, userId);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating weather language options", e);
        }
        return updatedRows > 0;
    }

    public boolean putUserWeatherUnitsOption(Long userId, String units) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("UPDATE UserWeatherOptions SET units = ? WHERE userId = ?");
            preparedStatement.setString(1, units);
            preparedStatement.setLong(2, userId);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding weather unit option", e);
        }
        return updatedRows > 0;
    }

    public boolean createNewWeatherAlert(long userId, Integer cityId, String cityName) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("INSERT INTO WeatherAlert (userId, cityId, cityName) VALUES (?,?,?)");
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, cityId);
            preparedStatement.setString(3, cityName);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error creating weather alert", e);
        }
        return updatedRows > 0;
    }

    public List<String> getAlertCitiesNameByUser(long userId) {
        List<String> alertCitiesNames = new ArrayList<>();
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("select cityName FROM WeatherAlert WHERE userId=?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                alertCitiesNames.add(result.getString("cityName"));
            }
        } catch (SQLException e) {
            log.error("Error getting alerts by user", e);
        }

        return alertCitiesNames;
    }

    public boolean deleteAlertCity(Long userId, String cityName) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM WeatherAlert WHERE userId=? AND cityName=?;");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, cityName);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error delete city alert", e);
        }
        return updatedRows > 0;
    }

    public boolean deleteAlertsForUser(Long userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM WeatherAlert WHERE userId=?");
            preparedStatement.setLong(1, userId);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting alerts for user", e);
        }
        return updatedRows > 0;
    }

    public List<WeatherAlert> getAllAlerts() {
        List<WeatherAlert> allAlerts = new ArrayList<>();

        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("select * FROM WeatherAlert");
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                WeatherAlert weatherAlert = new WeatherAlert();
                weatherAlert.setId(result.getInt("id"));
                weatherAlert.setUserId(result.getInt("userId"));
                weatherAlert.setCityId(result.getInt("cityId"));
                allAlerts.add(weatherAlert);
            }
        } catch (SQLException e) {
            log.error("Error getting all alerts", e);
        }

        return allAlerts;
    }
}
