/*
 * This is the source code of Telegram Bot v. 2.0
 * It is licensed under GNU GPL v. 3 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Ruben Bermudez, 3/12/14.
 */
package org.telegram.database;

import lombok.extern.slf4j.Slf4j;
import org.telegram.BuildVars;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * Connector to database
 */
@Slf4j
public class ConnectionDB {
    private final Connection currentConection;

    public ConnectionDB() {
        this.currentConection = openConexion();
    }

    private Connection openConexion() {
        Connection connection = null;
        try {
            Class.forName(BuildVars.controllerDB).getDeclaredConstructor().newInstance();
            connection = DriverManager.getConnection(BuildVars.linkDB, BuildVars.userDB, BuildVars.password);
        } catch (SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException |
                 NoSuchMethodException | InvocationTargetException e) {
            log.error("Error opening connection", e);
        }

        return connection;
    }

    public void closeConexion() {
        try {
            this.currentConection.close();
        } catch (SQLException e) {
            log.error("Error closing the connection", e);
        }

    }

    public ResultSet runSqlQuery(String query) throws SQLException {
        final Statement statement;
        statement = this.currentConection.createStatement();
        return statement.executeQuery(query);
    }

    public Boolean executeQuery(String query) throws SQLException {
        final Statement statement = this.currentConection.createStatement();
        return statement.execute(query);
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException {
        return this.currentConection.prepareStatement(query);
    }

    public PreparedStatement getPreparedStatement(String query, int flags) throws SQLException {
        return this.currentConection.prepareStatement(query, flags);
    }

    public int checkVersion() {
        int max = 0;
        try {
            final DatabaseMetaData metaData = this.currentConection.getMetaData();
            final ResultSet res = metaData.getTables(null, null, "",
                    new String[]{"TABLE"});
            while (res.next()) {
                if (res.getString("TABLE_NAME").compareTo("Versions") == 0) {
                    try(ResultSet result = runSqlQuery("SELECT Max(Version) FROM Versions")) {
                        while (result.next()) {
                            max = Math.max(max, result.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error checking version", e);
        }
        return max;
    }

    /**
     * Initilize a transaction in database
     * @throws SQLException If initialization fails
     */
    public void initTransaction() throws SQLException {
        this.currentConection.setAutoCommit(false);
    }

    /**
     * Finish a transaction in database and commit changes
     * @throws SQLException If a rollback fails
     */
    public void commitTransaction() throws SQLException {
        try {
            this.currentConection.commit();
        } catch (SQLException e) {
            if (this.currentConection != null) {
                this.currentConection.rollback();
            }
        } finally {
            this.currentConection.setAutoCommit(true);
        }
    }

    public void rollbackTransaction() throws SQLException {
        try {
            this.currentConection.rollback();
        } catch (SQLException e) {
            log.error("Error rolling back the transaction", e);
        } finally {
            this.currentConection.setAutoCommit(true);
        }
    }
}
