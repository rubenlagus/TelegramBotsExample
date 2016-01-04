package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.BuildVars;
import org.telegram.SenderHelper;
import org.telegram.api.methods.AnswerInlineQuery;
import org.telegram.api.methods.BotApiMethod;
import org.telegram.api.methods.SendMessage;
import org.telegram.api.objects.*;
import org.telegram.services.BotLogger;
import org.telegram.services.RaeService;
import org.telegram.updatesreceivers.UpdatesThread;
import org.telegram.updatesreceivers.Webhook;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for inline queries in Raebot
 * This is a use case that works with both Webhooks and GetUpdates methods
 * @date 24 of June of 2015
 */
public class RaeHandlers implements UpdatesCallback {
    private static final String LOGTAG = "RAEHANDLERS";
    private static final String TOKEN = BotConfig.TOKENRAE;
    private static final String BOTNAME = BotConfig.USERNAMERAE;
    private static final Integer CACHETIME = 86400;
    private static final boolean USEWEBHOOK = true;
    private final RaeService raeService;
    private final Object webhookLock = new Object();
    private final String THUMBNAILBLUE = "https://lh5.ggpht.com/-kSFHGvQkFivERzyCNgKPIECtIOELfPNWAQdXqQ7uqv2xztxqll4bVibI0oHJYAuAas=w300";
    private static final String helpMessage = "Este bot puede ayudarte a buscar definiciones de palabras según el diccionario de la RAE.\n\n" +
            "Funciona automáticamente, no hay necesidad de añadirlo a ningún sitio.\n" +
            "Simplemente abre cualquiera de tus chats y escribe `@raebot loquesea` en la zona de escribir mensajes.\n" +
            "Finalmente pulsa sobre un resultado para enviarlo." +
            "\n\n" +
            "Por ejemplo, intenta escribir `@raebot Punto` aquí.";

    public RaeHandlers(Webhook webhook) {
        raeService = new RaeService();
        if (USEWEBHOOK && BuildVars.useWebHook) {
            webhook.registerWebhook(this, BOTNAME);
            SenderHelper.SendWebhook(Webhook.getExternalURL(BOTNAME), TOKEN);
        } else {
            SenderHelper.SendWebhook("", TOKEN);
            new UpdatesThread(TOKEN, this);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasInlineQuery()) {
            BotApiMethod botApiMethod = handleIncomingInlineQuery(update.getInlineQuery());
            try {
                SenderHelper.SendApiMethod(botApiMethod, TOKEN);
            } catch (InvalidObjectException e) {
                BotLogger.error(LOGTAG, e);
            }
        } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
            try {
                SenderHelper.SendApiMethod(getHelpMessage(update.getMessage()), TOKEN);
            } catch (InvalidObjectException e) {
                BotLogger.error(LOGTAG, e);
            }
        }
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        if (update.hasInlineQuery()) {
            synchronized (webhookLock) {
                return handleIncomingInlineQuery(update.getInlineQuery());
            }
        } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
            synchronized (webhookLock) {
                return getHelpMessage(update.getMessage());
            }
        }
        return null;
    }

    /**
     * For an InlineQuery, results from RAE dictionariy are fetch and returned
     * @param inlineQuery InlineQuery recieved
     * @return BotApiMethod as response to the inline query
     */
    private BotApiMethod handleIncomingInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        BotLogger.debug(LOGTAG, "Searching: " + query);
        if (!query.isEmpty()) {
            List<RaeService.RaeResult> results = raeService.getResults(query);
            return converteResultsToResponse(inlineQuery, results);
        } else {
            return converteResultsToResponse(inlineQuery, new ArrayList<>());
        }
    }

    /**
     * Converts resutls from RaeService to an answer to an inline query
     * @param inlineQuery Original inline query
     * @param results Results from RAE service
     * @return AnswerInlineQuery method to answer the query
     */
    private BotApiMethod converteResultsToResponse(InlineQuery inlineQuery, List<RaeService.RaeResult> results) {
        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(inlineQuery.getId());
        answerInlineQuery.setCacheTime(CACHETIME);
        answerInlineQuery.setResults(convertRaeResults(results));
        return answerInlineQuery;
    }

    /**
     * Converts results from RaeService to a list of InlineQueryResultArticles
     * @param raeResults Results from rae service
     * @return List of InlineQueryResult
     */
    private List<InlineQueryResult> convertRaeResults(List<RaeService.RaeResult> raeResults) {
        List<InlineQueryResult> results = new ArrayList<>();

        for (int i = 0; i < raeResults.size(); i++) {
            RaeService.RaeResult raeResult = raeResults.get(i);
            InlineQueryResultArticle article = new InlineQueryResultArticle();
            article.setDisableWebPagePreview(true);
            article.setMarkdown(true);
            article.setId(Integer.toString(i));
            article.setMessageText(raeResult.getDefinition());
            article.setTitle(raeResult.getTitle());
            article.setDescription(raeResult.getDescription());
            article.setThumbUrl(THUMBNAILBLUE);
            results.add(article);
        }

        return results;
    }

    /**
     * Create a help message when an user try to send messages directly to the bot
     * @param message Received message
     * @return SendMessage method
     */
    public BotApiMethod getHelpMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(helpMessage);
        return sendMessage;
    }
}
