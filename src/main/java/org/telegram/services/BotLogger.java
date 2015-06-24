package org.telegram.services;

import org.telegram.BuildVars;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * @brief Logger to file
 * @date 21/01/15
 */
public class BotLogger {
    private static final Object lockToWrite = new Object();
    private static volatile PrintWriter logginFile;
    private static volatile String currentFileName;
    private static volatile ConcurrentHashMap<String, BotLogger> instances = new ConcurrentHashMap<>();
    private final Logger logger;
    private LocalDateTime lastFileDate;

    private BotLogger(String classname) {
        this.logger = Logger.getLogger(classname);
        this.logger.setLevel(Level.ALL);
        this.lastFileDate = LocalDateTime.now();
        if (currentFileName == null || currentFileName.compareTo("") == 0) {
            currentFileName = BuildVars.pathToLogs + dateFormatterForFileName(this.lastFileDate) + ".log";
            try {
                File file = new File(currentFileName);
                if (file.exists()) {
                    logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
                } else {
                    boolean created = file.createNewFile();
                    if (created) {
                        logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
                    } else {
                        throw new NullPointerException("File for loggin error");
                    }
                }
            } catch (IOException ignored) {
            }

        }
    }

    public static BotLogger getLogger(@NotNull String className) {
        BotLogger currentInstance;
        if (instances.containsKey(className)) {
            currentInstance = instances.get(className);
        } else {
            synchronized (BotLogger.class) {
                if (instances.containsKey(className)) {
                    currentInstance = instances.get(className);
                } else {
                    BotLogger instance = new BotLogger(className);
                    instances.put(className, instance);
                    currentInstance = instance;
                }
            }
        }

        return currentInstance;
    }

    public void log(@NotNull Level level, String msg) {
        this.logger.log(level, msg);
        logToFile(level, msg);
    }


    public void severe(String msg) {
        this.logger.severe(msg);
        logToFile(Level.SEVERE, msg);
    }

    public void warn(String msg) {
        warning(msg);
    }

    public void debug(String msg) {
        fine(msg);
    }

    public void error(String msg) {
        severe(msg);
    }

    public void trace(String msg) {
        finer(msg);
    }

    public void warning(String msg) {
        this.logger.warning(msg);
        logToFile(Level.WARNING, msg);
    }


    public void info(String msg) {
        this.logger.info(msg);
        logToFile(Level.INFO, msg);
    }


    public void config(String msg) {
        this.logger.config(msg);
        logToFile(Level.CONFIG, msg);
    }


    public void fine(String msg) {
        this.logger.fine(msg);
        logToFile(Level.FINE, msg);
    }


    public void finer(String msg) {
        this.logger.finer(msg);
        logToFile(Level.FINER, msg);
    }


    public void finest(String msg) {
        this.logger.finest(msg);
        logToFile(Level.FINEST, msg);
    }


    public void log(@NotNull Level level, @NotNull Throwable throwable) {
        this.logger.log(level, "Exception", throwable);
        logToFile(level, throwable);
    }

    public void log(@NotNull Level level, String msg, Throwable thrown) {
        this.logger.log(level, msg, thrown);
        logToFile(level, msg, thrown);
    }

    public void severe(@NotNull Throwable throwable) {
        logToFile(Level.SEVERE, throwable);
    }

    public void warning(@NotNull Throwable throwable) {
        logToFile(Level.WARNING, throwable);
    }

    public void info(@NotNull Throwable throwable) {
        logToFile(Level.INFO, throwable);
    }

    public void config(@NotNull Throwable throwable) {
        logToFile(Level.CONFIG, throwable);
    }

    public void fine(@NotNull Throwable throwable) {
        logToFile(Level.FINE, throwable);
    }

    public void finer(@NotNull Throwable throwable) {
        logToFile(Level.FINER, throwable);
    }

    public void finest(@NotNull Throwable throwable) {
        logToFile(Level.FINEST, throwable);
    }

    public void warn(Throwable throwable) {
        warning(throwable);
    }

    public void debug(Throwable throwable) {
        fine(throwable);
    }

    public void error(Throwable throwable) {
        severe(throwable);
    }

    public void trace(Throwable throwable) {
        finer(throwable);
    }

    public void severe(String msg, @NotNull Throwable throwable) {
        log(Level.SEVERE, msg, throwable);
    }

    public void warning(String msg, @NotNull Throwable throwable) {
        log(Level.WARNING, msg, throwable);
    }

    public void info(String msg, @NotNull Throwable throwable) {
        log(Level.INFO, msg, throwable);
    }

    public void config(String msg, @NotNull Throwable throwable) {
        log(Level.CONFIG, msg, throwable);
    }

    public void fine(String msg, @NotNull Throwable throwable) {
        log(Level.FINE, msg, throwable);
    }

    public void finer(String msg, @NotNull Throwable throwable) {
        log(Level.FINER, msg, throwable);
    }

    public void finest(String msg, @NotNull Throwable throwable) {
        log(Level.FINEST, msg, throwable);
    }

    public void warn(String msg, @NotNull Throwable throwable) {
        log(Level.WARNING, msg, throwable);
    }

    public void debug(String msg, @NotNull Throwable throwable) {
        log(Level.FINE, msg, throwable);
    }

    public void error(String msg, @NotNull Throwable throwable) {
        log(Level.SEVERE, msg, throwable);
    }

    public void trace(String msg, @NotNull Throwable throwable) {
        log(Level.FINER, msg, throwable);
    }

    private boolean isCurrentDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate().isEqual(this.lastFileDate.toLocalDate());
    }

    private String dateFormatterForFileName(@NotNull LocalDateTime dateTime) {
        String dateString = "";
        dateString += dateTime.getDayOfMonth();
        dateString += dateTime.getMonthValue();
        dateString += dateTime.getYear();
        return dateString;
    }

    private String dateFormatterForLogs(@NotNull LocalDateTime dateTime) {
        String dateString = "[";
        dateString += dateTime.getDayOfMonth() + "_";
        dateString += dateTime.getMonthValue() + "_";
        dateString += dateTime.getYear() + "_";
        dateString += dateTime.getHour() + ":";
        dateString += dateTime.getMinute() + ":";
        dateString += dateTime.getSecond();
        dateString += "] ";
        return dateString;
    }

    private void updateAndCreateFile(LocalDateTime dateTime) {
        if (!isCurrentDate(dateTime)) {
            this.lastFileDate = LocalDateTime.now();
            currentFileName = BuildVars.pathToLogs + dateFormatterForFileName(this.lastFileDate) + ".log";
            try {
                logginFile.flush();
                logginFile.close();
                File file = new File(currentFileName);
                if (file.exists()) {
                    logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
                } else {
                    boolean created = file.createNewFile();
                    if (created) {
                        logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
                    } else {
                        throw new NullPointerException("Error updating log file");
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void logToFile(@NotNull Level level, Throwable throwable) {
        if (isLoggable(level)) {
            synchronized (lockToWrite) {
                LocalDateTime currentDate = LocalDateTime.now();
                String dateForLog = dateFormatterForLogs(currentDate);
                updateAndCreateFile(currentDate);
                logThrowableToFile(level, throwable, dateForLog);
            }
        }
    }


    private void logToFile(@NotNull Level level, String msg) {
        if (isLoggable(level)) {
            synchronized (lockToWrite) {
                LocalDateTime currentDate = LocalDateTime.now();
                updateAndCreateFile(currentDate);
                String dateForLog = dateFormatterForLogs(currentDate);
                logMsgToFile(level, msg, dateForLog);
            }
        }
    }

    private void logToFile(Level level, String msg, Throwable throwable) {
        if (!isLoggable(level)) {
            return;
        }
        synchronized (lockToWrite) {
            LocalDateTime currentDate = LocalDateTime.now();
            updateAndCreateFile(currentDate);
            String dateForLog = dateFormatterForLogs(currentDate);
            logMsgToFile(level, msg, dateForLog);
            logThrowableToFile(level, throwable, dateForLog);
        }
    }

    private void logMsgToFile(Level level, String msg, String dateForLog) {
        dateForLog += level.toString() + " - " + msg;
        logginFile.println(dateForLog);
        logginFile.flush();
    }

    private void logThrowableToFile(Level level, Throwable throwable, String dateForLog) {
        logginFile.println(dateForLog + level.getName() + " - " + throwable);
        for (StackTraceElement element : throwable.getStackTrace()) {
            logginFile.println("\tat " + element);
        }
        logginFile.flush();
    }

    private boolean isLoggable(Level level) {
        return this.logger.isLoggable(level) && BuildVars.debug;
    }

    @Override
    protected void finalize() throws Throwable {
        logginFile.flush();
        logginFile.close();
        super.finalize();
    }
}
