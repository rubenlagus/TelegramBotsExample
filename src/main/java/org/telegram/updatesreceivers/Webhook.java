package org.telegram.updatesreceivers;

import com.sun.jersey.api.json.JSONConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.telegram.BuildVars;
import org.telegram.services.BotLogger;
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
    private static volatile BotLogger log = BotLogger.getLogger(Webhook.class.getName());

    private static final String KEYSTORE_SERVER_FILE = "./keystore_server";
    private static final String KEYSTORE_SERVER_PWD = "asdfgh";

    private final RestApi restApi;

    public Webhook() {
        this.restApi = new RestApi();
    }

    public void registerWebhook(UpdatesCallback callback, String botName) {
        restApi.registerCallback(callback, botName);
    }

    public void startServer() {
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);

        ResourceConfig rc = new ResourceConfig();
        rc.register(restApi);
        rc.register(JacksonFeature.class);
        rc.property(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        log.error("Internal webhook: " + getBaseURI().toString());
        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(
                getBaseURI(),
                rc,
                true,
                new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(false));
        try {
            grizzlyServer.start();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void startDebugServer() {
        ResourceConfig rc = new ResourceConfig();
        rc.register(restApi);
        rc.register(JacksonFeature.class);
        rc.property(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc);
    }

    public static String getExternalURL(String botName) {
        return String.format("%s/callback/%s", BuildVars.EXTERNALWEBHOOKURL, botName);
    }

    private static URI getBaseURI() {
        return URI.create(BuildVars.INTERNALWEBHOOKURL);
    }
 }
