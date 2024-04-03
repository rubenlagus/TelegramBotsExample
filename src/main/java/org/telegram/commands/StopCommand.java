package org.telegram.commands;

import lombok.extern.slf4j.Slf4j;
import org.telegram.database.DatabaseManager;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * This commands stops the conversation with the bot.
 * Bot won't respond to user until he sends a start command
 *
 * @author Timo Schulz (Mit0x2)
 */
@Slf4j
public class StopCommand extends BotCommand {

    public static final String LOGTAG = "STOPCOMMAND";

    /**
     * Construct
     */
    public StopCommand() {
        super("stop", "With this command you can stop the Bot");
    }

    @Override
    public void execute(TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
        DatabaseManager dbManager = DatabaseManager.getInstance();

        if (dbManager.getUserStateForCommandsBot(user.getId())) {
            dbManager.setUserStateForCommandsBot(user.getId(), false);
            String userName = user.getFirstName() + " " + user.getLastName();

            SendMessage answer = new SendMessage(chat.getId().toString(), "Good bye " + userName + "\n" + "Hope to see you soon!");

            try {
                telegramClient.execute(answer);
            } catch (TelegramApiException e) {
                log.error("Error", e);
            }
        }
    }
}
