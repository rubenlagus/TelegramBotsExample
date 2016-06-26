package org.telegram.updateshandlers;

import org.telegram.BotConfig;
import org.telegram.services.RaeService;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.logging.BotLogger;

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
        return BotConfig.RAE_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasInlineQuery()) {
                handleIncomingInlineQuery(update.getInlineQuery());
            } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
                try {
                    sendMessage(getHelpMessage(update.getMessage()));
                } catch (TelegramApiException e) {
                    BotLogger.error(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.RAE_USER;
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
                answerInlineQuery(converteResultsToResponse(inlineQuery, results));
            } else {
                answerInlineQuery(converteResultsToResponse(inlineQuery, new ArrayList<>()));
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
            InputTextMessageContent messageContent = new InputTextMessageContent();
            messageContent.disableWebPagePreview();
            messageContent.enableMarkdown(true);
            messageContent.setMessageText(raeResult.getDefinition());
            InlineQueryResultArticle article = new InlineQueryResultArticle();
            article.setInputMessageContent(messageContent);
            article.setId(Integer.toString(i));
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
