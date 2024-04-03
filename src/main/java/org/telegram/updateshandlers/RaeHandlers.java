package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.services.RaeService;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Handler for inline queries in Raebot
 */
@Slf4j
public class RaeHandlers implements LongPollingSingleThreadUpdateConsumer {
    private static final Integer CACHETIME = 86400;
    private final RaeService raeService = new RaeService();
    private static final String THUMBNAILBLUE = "https://lh5.ggpht.com/-kSFHGvQkFivERzyCNgKPIECtIOELfPNWAQdXqQ7uqv2xztxqll4bVibI0oHJYAuAas=w300";
    private static final String helpMessage = "Este bot puede ayudarte a buscar definiciones de palabras según el diccionario de la RAE.\n\n" +
            "Funciona automáticamente, no hay necesidad de añadirlo a ningún sitio.\n" +
            "Simplemente abre cualquiera de tus chats y escribe `@raebot loquesea` en la zona de escribir mensajes.\n" +
            "Finalmente pulsa sobre un resultado para enviarlo." +
            "\n\n" +
            "Por ejemplo, intenta escribir `@raebot Punto` aquí.";

    private final TelegramClient telegramClient;

    public RaeHandlers(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasInlineQuery()) {
                handleIncomingInlineQuery(update.getInlineQuery());
            } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
                try {
                    telegramClient.execute(getHelpMessage(update.getMessage()));
                } catch (TelegramApiException e) {
                    log.error("Error", e);
                }
            }
        } catch (Exception e) {
            log.error("Unknown exception", e);
        }
    }

    /**
     * For an InlineQuery, results from RAE dictionariy are fetch and returned
     * @param inlineQuery InlineQuery recieved
     */
    private void handleIncomingInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        log.debug("Searching: {}", query);
        try {
            if (!query.isEmpty()) {
                List<RaeService.RaeResult> results = raeService.getResults(query);
                telegramClient.execute(converteResultsToResponse(inlineQuery, results));
            } else {
                telegramClient.execute(converteResultsToResponse(inlineQuery, new ArrayList<>()));
            }
        } catch (TelegramApiException e) {
            log.error("Error handing inline query", e);
        }
    }

    /**
     * Converts resutls from RaeService to an answer to an inline query
     * @param inlineQuery Original inline query
     * @param results Results from RAE service
     * @return AnswerInlineQuery method to answer the query
     */
    private static AnswerInlineQuery converteResultsToResponse(InlineQuery inlineQuery, List<RaeService.RaeResult> results) {
        return AnswerInlineQuery
                .builder()
                .inlineQueryId(inlineQuery.getId())
                .cacheTime(CACHETIME)
                .results(convertRaeResults(results))
                .build();
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
            InlineQueryResultArticle article = new InlineQueryResultArticle(
                    Integer.toString(i),
                    raeResult.getTitle(),
                    InputTextMessageContent
                            .builder()
                            .disableWebPagePreview(true)
                            .parseMode(ParseMode.MARKDOWN)
                            .messageText(raeResult.getDefinition())
                            .build()
            );
            article.setDescription(raeResult.getDescription());
            article.setThumbnailUrl(THUMBNAILBLUE);
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
        return SendMessage
                .builder()
                .chatId(message.getChatId())
                .parseMode(ParseMode.MARKDOWN)
                .text(helpMessage)
                .build();
    }
}
