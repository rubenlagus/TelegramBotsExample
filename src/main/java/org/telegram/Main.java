package org.telegram;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.logging.BotLogger;
import org.telegram.telegrambots.logging.BotsFileHandler;
import org.telegram.updateshandlers.*;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Main class to create all bots
 * @date 20 of June of 2015
 */
public class Main {
    private static final String LOGTAG = "MAIN";

    public static void main(String[] args) {
        BotLogger.setLevel(Level.ALL);
        BotLogger.registerLogger(new ConsoleHandler());
        try {
            BotLogger.registerLogger(new BotsFileHandler());
        } catch (IOException e) {
            BotLogger.severe("MAIN", e);
        }
            // default, start all sample bots in getUpdates mode
        if (!BuildVars.useWebHook) {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

            try {
                telegramBotsApi.registerBot(new ChannelHandlers());
                telegramBotsApi.registerBot(new DirectionsHandlers());
                telegramBotsApi.registerBot(new RaeHandlers());
                telegramBotsApi.registerBot(new WeatherHandlers());
                telegramBotsApi.registerBot(new TransifexHandlers());
                telegramBotsApi.registerBot(new FilesHandlers());
                telegramBotsApi.registerBot(new CommandsHandler());
            } catch (TelegramApiException e) {
                BotLogger.error(LOGTAG, e);
            }
            // Filled a path to a pem file ? looks like you're going for the self signed option then, invoke with store and pem file to supply.
            // check https://core.telegram.org/bots/self-signed#java-keystore for generating a keypair in store and exporting the pem.
            // dont forget to split the pem bundle (begin/end), use only the public key as input!
            } else if (!BuildVars.pathToCertificatePublicKey.isEmpty()) {
                try {
                    TelegramBotsApi telegramBotsSelfWebhookApi = new TelegramBotsApi(BuildVars.pathToCertificateStore, BuildVars.certificateStorePassword, BuildVars.EXTERNALWEBHOOKURL, BuildVars.INTERNALWEBHOOKURL,BuildVars.pathToCertificatePublicKey);
                    telegramBotsSelfWebhookApi.registerBot(new WebHookExampleHandlers());
                } catch (Exception e) {
                    BotLogger.error(LOGTAG, e);
                }
            } else {
            // Non self signed, make sure you've added private/public and if needed intermediate to your cert-store.
            // Coming from a set of pem files here's one way to do it:
            // openssl pkcs12 -export -in public.pem -inkey private.pem > keypair.p12
            // keytool -importkeystore -srckeystore keypair.p12 -destkeystore server.jks -srcstoretype pkcs12
            // have (an) intermediate(s) to supply? first: cat public.pem intermediate.pem > set.pem (use set.pem as -in)
            try {
                TelegramBotsApi telegramBotsWebhookApi = new TelegramBotsApi(BuildVars.pathToCertificateStore, BuildVars.certificateStorePassword, BuildVars.EXTERNALWEBHOOKURL, BuildVars.INTERNALWEBHOOKURL);
                telegramBotsWebhookApi.registerBot(new WebHookExampleHandlers());
            } catch (Exception e) {
                BotLogger.error(LOGTAG, e);
            }

        }
    }
}
