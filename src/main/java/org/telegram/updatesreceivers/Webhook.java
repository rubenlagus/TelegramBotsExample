package org.telegram.updatesreceivers;

import com.sun.jersey.api.json.JSONConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.telegram.BuildVars;
import org.telegram.updateshandlers.UpdatesCallback;

import java.io.IOException;
import java.net.URI;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Webhook to receive updates
 * @date 20 of June of 2015
 */
public class Webhook {
    private static final int PORT = 443;
    private static final String KEYSTORE_SERVER_FILE = "./keystore_server";
    private static final String KEYSTORE_SERVER_PWD = "asdfgh";

    private final String path;


    public Webhook(UpdatesCallback callback, String webhookPath) {
        this.path = webhookPath;
        RestApi restApi = new RestApi(callback);
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);

        ResourceConfig rc = new ResourceConfig();
        rc.register(restApi);
        rc.register(JacksonFeature.class);
        rc.property(JSONConfiguration.FEATURE_POJO_MAPPING, true);

        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(
                getBaseURI(),
                rc,
                true,
                new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(false));
        try {
            grizzlyServer.start();
        } catch (IOException ignored) {
        }
    }

    private URI getBaseURI() {
        return URI.create(String.format("%s:%d/%s", BuildVars.INTERNALWEBHOOKURL, PORT, this.path));
    }

    public String getURL() {
        return String.format("%s/%s/callback", BuildVars.BASEWEBHOOKURL, this.path);
    }
 }
