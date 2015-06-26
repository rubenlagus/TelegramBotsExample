/*
 * This is the source code of Telegram Bot v. 2.0
 * It is licensed under GNU GPL v. 3 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Ruben Bermudez, 3/12/14.
 */
package org.telegram.database;

import org.telegram.services.BotLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * @brief Database Manager to perform database operations
 * @date 3/12/14
 */
public class DatabaseManager {
    private static volatile BotLogger log = BotLogger.getLogger(DatabaseManager.class.getName()); ///< Logger
    private static volatile DatabaseManager instance;
    private static volatile ConectionDB connetion;

    /**
     * Private constructor (due to Singleton)
     */
    private DatabaseManager() {
        connetion = new ConectionDB();
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
            connetion.commitTransaction();
        } catch (SQLException e) {
            log.error(e);
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
        return 2;
    }

    private int updateToVersion4() throws SQLException {
        connetion.executeQuery(CreationStrings.createLastUpdateDatabase);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, 4));
        return 4;
    }

    private int createNewTables() throws SQLException {
        connetion.executeQuery(CreationStrings.createVersionTable);
        connetion.executeQuery(CreationStrings.createFilesTable);
        connetion.executeQuery(String.format(CreationStrings.insertCurrentVersion, CreationStrings.version));
        connetion.executeQuery(CreationStrings.createUsersForFilesTable);
        connetion.executeQuery(CreationStrings.createRecentWeatherTable);
        connetion.executeQuery(CreationStrings.createDirectionsDatabase);
        return CreationStrings.version;
    }

    public boolean addFile(String fileId, Integer userId, String caption) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO Files (fileId, userId, caption) VALUES(?, ?, ?)");
            preparedStatement.setString(1, fileId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setString(3, caption);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updatedRows > 0;
    }

    public HashMap<String, String> getFilesByUser(Integer userId) {
        HashMap<String, String> files = new HashMap<>();
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT * FROM Files WHERE userId = ?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                files.put(result.getString("fileId"), result.getString("caption"));
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return files;
    }

    public boolean addUserForFile(Integer userId, int status) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO FilesUsers (userId, status) VALUES(?, ?)");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, status);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updatedRows > 0;
    }

    public boolean deleteUserForFile(Integer userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM FilesUsers WHERE userId=?;");
            preparedStatement.setInt(1, userId);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updatedRows > 0;
    }

    public int getUserStatusForFile(Integer userId) {
        int status = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("Select status FROM FilesUsers WHERE userId=?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                status = result.getInt("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return updatedRows > 0;
    }

    public boolean addRecentWeather(Integer userId, Integer cityId, String cityName) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO RecentWeather (userId, cityId, cityName) VALUES(?, ?, ?)");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, cityId);
            preparedStatement.setString(3, cityName);
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cleanUpRecent(userId);
        return updatedRows > 0;
    }

    public HashMap<Integer,String> getRecentWeather(Integer userId) {
        HashMap<Integer,String> recentWeather = new HashMap<>();
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("select * FROM RecentWeather WHERE userId=?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                recentWeather.put(result.getInt("cityId"), result.getString("cityName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recentWeather;
    }

    private void cleanUpRecent(Integer userId) {
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM RecentWeather WHERE userid = ? AND ID <= (SELECT ID FROM (SELECT id From RecentWeather where userId = ? ORDER BY id DESC LIMIT 1 OFFSET 4 ) AS T1 )");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addUserForDirection(Integer userId, int status, int messageId, String origin) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("REPLACE INTO Directions (userId, status, messageId, origin) VALUES(?, ?, ?, ?)");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, status);
            preparedStatement.setInt(3, messageId);
            if (origin == null || origin.isEmpty()) {
                preparedStatement.setNull(4, Types.VARCHAR);
            } else {
                preparedStatement.setString(4, origin);
            }
            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updatedRows > 0;
    }

    public int getUserDestinationStatus(Integer userId) {
        int status = -1;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT status FROM Directions WHERE userId = ?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                status = result.getInt("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return status;
    }

    public int getUserDestinationMessageId(Integer userId) {
        int messageId = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT messageId FROM Directions WHERE userId = ?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                messageId = result.getInt("messageId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messageId;
    }

    public String getUserOrigin(Integer userId) {
        String origin = "";
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("SELECT origin FROM Directions WHERE userId = ?");
            preparedStatement.setInt(1, userId);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                origin = result.getString("origin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return origin;
    }

    public boolean deleteUserForDirections(Integer userId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connetion.getPreparedStatement("DELETE FROM Directions WHERE userId=?;");
            preparedStatement.setInt(1, userId);

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return updateId;
    }
}
