package org.telegram.services;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.telegram.BuildVars;
import org.telegram.telegrambots.api.methods.SendDocument;

import java.io.*;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Service that allow transifex files download
 * @date 21 of June of 2015
 */
public class TransifexService {
    private static final String LOGTAG = "TRANSIFEXSERVICE";

    private static final String BASEURLAndroid = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/telegram/resource/stringsxml-48/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLiOS = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/iphone-1/resource/localizablestrings/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLOSX = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/osx/resource/localizablestrings/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLTDesktop = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/telegram-desktop/resource/langstrings/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLTemplates = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/telegram-desktop/resource/tl_generaltxt/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLWebogram = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/telegram-web/resource/en-usjson/translation/@language?file";  ///< Base url for REST
    private static final String BASEURLWP = "http://" + BuildVars.TRANSIFEXUSER + ":" + BuildVars.TRANSIFEXPASSWORD + "@www.transifex.com/api/2/project/wp-telegram-messenger-beta/resource/appresourcesresx/translation/@language?file";  ///< Base url for REST
    private static final int STATUS200 = 200;
    private static final int BYTES1024 = 1024;
    private static volatile TransifexService instance; ///< Instance of this class


    /**
     * Constructor (private due to singleton pattern)
     */
    private TransifexService() {
    }

    /**
     * Singleton
     *
     * @return Return the instance of this class
     */
    public static TransifexService getInstance() {
        TransifexService currentInstance;
        if (instance == null) {
            synchronized (TransifexService.class) {
                if (instance == null) {
                    instance = new TransifexService();
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    private String getFileAndroid(String query) {
        String result = null;
        try {
            CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
            HttpGet request = new HttpGet(BASEURLAndroid.replace("@language", query));
            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String line;
            String responseString = "";
            while ((line = rd.readLine()) != null) {
                responseString += line;
            }

            if (response.getStatusLine().getStatusCode() == STATUS200) {
                result = responseString;
            }
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    private byte[] getFileiOS(String query) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLiOS.replace("@language", query));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-16LE"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    private byte[] getFileOSX(String query) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLOSX.replace("@language", query));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-16LE"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    private byte[] getFileTDesktop(String query) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLTDesktop.replace("@language", query));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-16LE"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    public byte[] getFileTemplate(String languageCode) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLTemplates.replace("@language", languageCode));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    private byte[] getFileWebogram(String query) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLWebogram.replace("@language", query));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-16LE"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    private byte[] getFileWP(String query) {
        byte[] result = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(BASEURLWP.replace("@language", query));
            HttpResponse response = client.execute(request);
            result = IOUtils.toByteArray(new InputStreamReader(response.getEntity().getContent(), "UTF-16LE"));
        } catch (IOException e) {
            BotLogger.error(LOGTAG, e);
        }
        return result;
    }

    /**
     * For languages that are composited of a regional part, change that part to uper case for transifex
     * @param language Language received
     * @return Language fixed
     */
    private String fixCaseCompositedLanguages(String language) {
        String[] parts = language.split("_");
        if (parts.length == 1) {
            language = parts[0];
        } else {
            language = parts[0] + "_" + parts[1].toUpperCase();
        }
        return language;
    }

    /**
     * Fetch the language file for support members of android
     * @param language Language requested
     */
    public SendDocument getAndroidSupportLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            String file = getFileAndroid(language);

            if (file != null && file.getBytes().length / BYTES1024 >= 10) {
                file = file.replaceAll("\"LanguageName\"\\>(\\w*)\\<\\/string\\>", "\"LanguageName\"\\>$1_1\\<\\/string\\>").replaceAll("\"LanguageCode\"\\>(\\w*)\\<\\/string\\>", "\"LanguageCode\"\\>$1_1\\<\\/string\\>");
                try {
                    String fileName = "languages_Android_" + language + ".xml";
                    PrintWriter localFile = new PrintWriter(fileName);
                    localFile.print(file);
                    localFile.close();
                    File fileToUpload = new File(fileName);
                    sendDocument = new SendDocument();
                    sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                } catch (FileNotFoundException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for Android
     * @param language Language requested
     */
    public SendDocument getAndroidLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            String file = getFileAndroid(language);
            if (file != null && file.getBytes().length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_Android_" + language + ".xml";
                    PrintWriter localFile = new PrintWriter(fileName);
                    localFile.print(file);
                    localFile.close();
                    File fileToUpload = new File(fileName);
                    sendDocument = new SendDocument();
                    sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                } catch (FileNotFoundException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for iOS
     *
     * @param language Language requested
     */
    public SendDocument getiOSLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            byte[] file = getFileiOS(language);
            if (file != null && file.length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_ios_" + language + ".strings";
                    File fileToUpload = new File(fileName);
                    FileOutputStream output = new FileOutputStream(fileToUpload);
                    IOUtils.write(file, output);
                    output.close();
                    sendDocument = new SendDocument();
                    sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                } catch (IOException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for OSX
     * @param language Language requested
     */
    public SendDocument getOSXLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            byte[] file = getFileOSX(language);
            if (file != null && file.length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_osx_" + language + ".strings";
                    File fileToUpload = new File(fileName);
                    FileOutputStream output = new FileOutputStream(fileToUpload);
                    IOUtils.write(file, output);
                    output.close();
                    sendDocument = new SendDocument();
                    sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                } catch (IOException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for Tdesktop
     * @param language Language requested
     */
    public SendDocument getTdesktopLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            byte[] file = getFileTDesktop(language);
            if (file != null && file.length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_tdesktop_" + language + ".strings";
                    File fileToUpload = new File(fileName);
                    FileOutputStream output = new FileOutputStream(fileToUpload);
                    IOUtils.write(file, output);
                    output.close();
                    if (fileToUpload.exists()) {
                        sendDocument = new SendDocument();
                        sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for Webogram
     * @param language Language requested
     */
    public SendDocument getWebogramLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            byte[] file = getFileWebogram(language);
            if (file != null && file.length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_webogram_" + language + ".json";
                    File fileToUpload = new File(fileName);
                    FileOutputStream output = new FileOutputStream(fileToUpload);
                    IOUtils.write(file, output);
                    output.close();
                    if (fileToUpload.exists()) {
                        sendDocument = new SendDocument();
                        sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendDocument;
    }

    /**
     * Fetch the language file for WP
     * @param language Language requested
     */
    public SendDocument getWPLanguageFile(String language) {
        SendDocument sendDocument = null;
        try {
            byte[] file = getFileWP(language);
            if (file != null && file.length / BYTES1024 >= 10) {
                try {
                    String fileName = "languages_wp_" + language + ".xml";
                    File fileToUpload = new File(fileName);
                    FileOutputStream output = new FileOutputStream(fileToUpload);
                    IOUtils.write(file, output);
                    output.close();
                    if (fileToUpload.exists()) {
                        sendDocument = new SendDocument();
                        sendDocument.setNewDocument(fileToUpload.getAbsolutePath(), fileName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendDocument;
    }
}
