package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.services.BotLogger;
import org.telegram.services.RaeService;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Handler for inline queries in Raebot
 * @date 24 of June of 2015
 */
public class RaeHandlers extends TelegramLongPollingBot {
    private static final String LOGTAG = "RAEHANDLERS";

    private static final Integer CACHETIME = 86400;
    private final RaeService raeService = new RaeService();
    private static final String THUMBNAILBLUE = "https://lh5.ggpht.com/-kSFHGvQkFivERzyCNgKPIECtIOELfPNWAQdXqQ7uqv2xztxqll4bVibI0oHJYAuAas=w300";
    private static final String helpMessage = "Este bot puede ayudarte a buscar definiciones de palabras según el diccionario de la RAE.\n\n" +
            "Funciona automáticamente, no hay necesidad de añadirlo a ningún sitio.\n" +
            "Simplemente abre cualquiera de tus chats y escribe `@raebot loquesea` en la zona de escribir mensajes.\n" +
            "Finalmente pulsa sobre un resultado para enviarlo." +
            "\n\n" +
            "Por ejemplo, intenta escribir `@raebot Punto` aquí.";

    @Override
    public String getBotToken() {
        return BotConfig.TOKENRAE;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasInlineQuery()) {
            handleIncomingInlineQuery(update.getInlineQuery());
        } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
            try {
                sendMessage(getHelpMessage(update.getMessage()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.USERNAMERAE;
    }

    /**
     * For an InlineQuery, results from RAE dictionariy are fetch and returned
     * @param inlineQuery InlineQuery recieved
     */
    private void handleIncomingInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        BotLogger.debug(LOGTAG, "Searching: " + query);
        try {
            if (!query.isEmpty()) {
                List<RaeService.RaeResult> results = raeService.getResults(query);
                sendAnswerInlineQuery(converteResultsToResponse(inlineQuery, results));
            } else {
                sendAnswerInlineQuery(converteResultsToResponse(inlineQuery, new ArrayList<>()));
            }
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    /**
     * Converts resutls from RaeService to an answer to an inline query
     * @param inlineQuery Original inline query
     * @param results Results from RAE service
     * @return AnswerInlineQuery method to answer the query
     */
    private static AnswerInlineQuery converteResultsToResponse(InlineQuery inlineQuery, List<RaeService.RaeResult> results) {
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
    private static List<InlineQueryResult> convertRaeResults(List<RaeService.RaeResult> raeResults) {
        List<InlineQueryResult> results = new ArrayList<>();

        for (int i = 0; i < raeResults.size(); i++) {
            RaeService.RaeResult raeResult = raeResults.get(i);
            InlineQueryResultArticle article = new InlineQueryResultArticle();
            article.setDisableWebPagePreview(true);
            article.enableMarkdown(true);
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
    private static SendMessage getHelpMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(helpMessage);
        return sendMessage;
    }
}
