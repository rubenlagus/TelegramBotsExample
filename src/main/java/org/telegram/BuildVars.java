package org.telegram;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Custom build vars FILL EVERYTHING CORRECTLY
 * @date 20 of June of 2015
 */
public class BuildVars {
    public static final Boolean debug = true;
    public static final Boolean useWebHook = true;
    public static final int PORT = 8443;
    public static final String EXTERNALWEBHOOKURL = "your-external-url:" + PORT;
    public static final String INTERNALWEBHOOKURL = "your-internal-url:" + PORT;
    public static final String pathToCertificatePublicKey = "path/to/my/certkey.pem";
    public static final String certificatePublicKeyFileName = "certkey.pem";

    public static final String OPENWEATHERAPIKEY = "<your-api-key>";

    public static final String DirectionsApiKey = "<your-api-key>";

    public static final String TRANSIFEXUSER = "<transifex-user>";
    public static final String TRANSIFEXPASSWORD = "<transifex-password>";

    public static final String pathToLogs = "./";

    public static final String linkDB = "jdbc:mysql://localhost:3306/YOURDATABSENAME?useUnicode=true&characterEncoding=UTF-8";
    public static final String controllerDB = "com.mysql.jdbc.Driver";
    public static final String userDB = "<your-database-user>";
    public static final String password = "<your-databas-user-password>";
}
