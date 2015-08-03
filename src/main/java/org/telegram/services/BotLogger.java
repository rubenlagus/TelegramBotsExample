package org.telegram.services;

import org.telegram.BuildVars;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Logger
 * @date 21/01/15
 */
public class BotLogger {
    private volatile Object lockToWrite = new Object();

    private final Logger logger;
    private static volatile PrintWriter logginFile;
    private Calendar lastFileDate;
    private static volatile String currentFileName;
    private static LoggerThread loggerThread = new LoggerThread();
    private static volatile ConcurrentHashMap<String, BotLogger> instances = new ConcurrentHashMap<>();
    private final static ConcurrentLinkedQueue<String> logsToFile = new ConcurrentLinkedQueue<>();

    static {

        loggerThread.start();
    }

    public static BotLogger getLogger(@NotNull String className) {
        if (!instances.containsKey(className)) {
            synchronized (BotLogger.class) {
                if (!instances.containsKey(className)) {
                    BotLogger instance = new BotLogger(className);
                    instances.put(className, instance);
                    return instance;
                } else {
                    return instances.get(className);
                }
            }
        } else {
            return instances.get(className);
        }
    }

    private BotLogger(String classname) {
        logger = Logger.getLogger(classname);
        logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        lastFileDate = new GregorianCalendar();
        if  (currentFileName == null || currentFileName.length() == 0) {
            currentFileName = BuildVars.pathToLogs + dateFormaterForFileName(lastFileDate) + ".log";
            try {
                File file = new File(currentFileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public void log(@NotNull Level level, String msg) {
        logger.log(level, msg);
        logToFile(level, msg);
    }


    public void severe(String msg) {
        logger.severe(msg);
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
        logger.warning(msg);
        logToFile(Level.WARNING, msg);
    }


    public void info(String msg) {
        logger.info(msg);
        logToFile(Level.INFO, msg);
    }


    public void config(String msg) {
        logger.config(msg);
        logToFile(Level.CONFIG, msg);
    }


    public void fine(String msg) {
        logger.fine(msg);
        logToFile(Level.FINE, msg);
    }


    public void finer(String msg) {
        logger.finer(msg);
        logToFile(Level.FINER, msg);
    }


    public void finest(String msg) {
        logger.finest(msg);
        logToFile(Level.FINEST, msg);
    }


    public void log(@NotNull Level level, @NotNull Throwable throwable) {
        throwable.printStackTrace();
        logToFile(level, throwable);
    }

    public void log(@NotNull Level level, String msg, Throwable thrown) {
        logger.log(level, msg, thrown);
        logToFile(level, msg ,thrown);
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

    private boolean isCurrentDate(Calendar calendar) {
        if (calendar.get(Calendar.DAY_OF_MONTH) != lastFileDate.get(Calendar.DAY_OF_MONTH)) {
            return false;
        }
        if (calendar.get(Calendar.MONTH) != lastFileDate.get(Calendar.MONTH)) {
            return false;
        }
        if (calendar.get(Calendar.YEAR) != lastFileDate.get(Calendar.YEAR)) {
            return false;
        }
        return true;
    }

    private String dateFormaterForFileName(@NotNull Calendar calendar) {
        String dateString = "";
        dateString += calendar.get(Calendar.DAY_OF_MONTH);
        dateString += calendar.get(Calendar.MONTH) + 1;
        dateString += calendar.get(Calendar.YEAR);
        return dateString;
    }

    private String dateFormaterForLogs(@NotNull Calendar calendar) {
        String dateString = "[";
        dateString += calendar.get(Calendar.DAY_OF_MONTH) + "_";
        dateString += (calendar.get(Calendar.MONTH) + 1) + "_";
        dateString += calendar.get(Calendar.YEAR) + "_";
        dateString += calendar.get(Calendar.HOUR_OF_DAY) + "_";
        dateString += calendar.get(Calendar.MINUTE) + ":";
        dateString += calendar.get(Calendar.SECOND);
        dateString += "] ";
        return dateString;
    }

    private void updateAndCreateFile(Calendar calendar) {
        if (isCurrentDate(calendar)) {
            return;
        }
        lastFileDate = new GregorianCalendar();
        currentFileName = BuildVars.pathToLogs + dateFormaterForFileName(lastFileDate) + ".log";
        try {
            logginFile.flush();
            logginFile.close();
            File file = new File(currentFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            logginFile = new PrintWriter(new BufferedWriter(new FileWriter(currentFileName, true)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logToFile(@NotNull Level level, Throwable throwable) {
        if (!isLoggable(level)){
            return;
        }
        synchronized (lockToWrite) {
            Calendar currentDate = new GregorianCalendar();
            String dateForLog = dateFormaterForLogs(currentDate);
            updateAndCreateFile(currentDate);
            logThrowableToFile(level, throwable, dateForLog);
        }
    }



    private void logToFile(@NotNull Level level, String msg) {
        if (!isLoggable(level)){
            return;
        }
        synchronized (lockToWrite) {
            Calendar currentDate = new GregorianCalendar();
            updateAndCreateFile(currentDate);
            String dateForLog = dateFormaterForLogs(currentDate);
            logMsgToFile(level, msg, dateForLog);
        }
    }

    private void logToFile(Level level, String msg, Throwable throwable) {
        if (!isLoggable(level)){
            return;
        }
        synchronized (lockToWrite) {
            Calendar currentDate = new GregorianCalendar();
            updateAndCreateFile(currentDate);
            String dateForLog = dateFormaterForLogs(currentDate);
            logMsgToFile(level, msg, dateForLog);
            logThrowableToFile(level, throwable, dateForLog);
        }
    }

    private void logMsgToFile(Level level, String msg, String dateForLog) {
        dateForLog += " [" + logger.getName() + "]"  + level.toString() + " - " + msg;
        logsToFile.add(dateForLog);
        synchronized (logsToFile) {
            logsToFile.notifyAll();
        }
    }

    private void logThrowableToFile(Level level, Throwable throwable, String dateForLog) {
        String throwableLog = dateForLog + level.getName() + " - " + throwable + "\n";
        for (StackTraceElement element : throwable.getStackTrace()) {
            throwableLog += "\tat " + element + "\n";
        }
        logsToFile.add(throwableLog);
        synchronized (logsToFile) {
            logsToFile.notifyAll();
        }
    }

    private boolean isLoggable(Level level) {
        return logger.isLoggable(level) && BuildVars.debug;
    }

    @Override
    protected void finalize() throws Throwable {
        logginFile.flush();
        logginFile.close();
        super.finalize();
    }

    private static class LoggerThread extends Thread {
        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while(true) {
                ConcurrentLinkedQueue<String> stringsToLog = new ConcurrentLinkedQueue<>();
                synchronized (logsToFile) {
                    if (logsToFile.isEmpty()) {
                        try {
                            logsToFile.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (logsToFile.isEmpty()) {
                            continue;
                        }
                    }
                    stringsToLog.addAll(logsToFile);
                    logsToFile.clear();
                }

                for (String stringToLog: stringsToLog) {
                    logginFile.println(stringToLog);
                }
                logginFile.flush();
            }
        }
    }
}
