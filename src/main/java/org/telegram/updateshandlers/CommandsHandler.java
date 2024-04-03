package org.telegram.updateshandlers;

import lombok.extern.slf4j.Slf4j;
import org.telegram.commands.HelloCommand;
import org.telegram.commands.HelpCommand;
import org.telegram.commands.StartCommand;
import org.telegram.commands.StopCommand;
import org.telegram.database.DatabaseManager;
import org.telegram.services.Emoji;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * This handler mainly works with commands to demonstrate the Commands feature of the API
 *
 * @author Timo Schulz (Mit0x2)
 */
@Slf4j
public class CommandsHandler extends CommandLongPollingTelegramBot {
    /**
     * Constructor.
     */
    public CommandsHandler(String botToken, String botUsername) {
        super(new OkHttpTelegramClient(botToken), true, () -> botUsername);
        register(new HelloCommand());
        register(new StartCommand());
        register(new StopCommand());
        HelpCommand helpCommand = new HelpCommand(this);
        register(helpCommand);

        registerDefaultAction((telegramClient, message) -> {
            SendMessage commandUnknownMessage = new SendMessage(String.valueOf(message.getChatId()),
                    "The command '" + message.getText() + "' is not known by this bot. Here comes some help " + Emoji.AMBULANCE);
            try {
                telegramClient.execute(commandUnknownMessage);
            } catch (TelegramApiException e) {
                log.error("Error sending message in commands bot", e);
            }
            helpCommand.execute(telegramClient, message.getFrom(), message.getChat(), new String[] {});
        });
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (!DatabaseManager.getInstance().getUserStateForCommandsBot(message.getFrom().getId())) {
                return;
            }

            if (message.hasText()) {
                SendMessage echoMessage = new SendMessage(String.valueOf(message.getChatId()), "Hey heres your message:\n" + message.getText());
                try {
                    telegramClient.execute(echoMessage);
                } catch (TelegramApiException e) {
                    log.error("Error processing non-command update", e);
                }
            }
        }
    }
}