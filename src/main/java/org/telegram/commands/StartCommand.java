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
 * This commands starts the conversation with the bot
 *
 * @author Timo Schulz (Mit0x2)
 */
@Slf4j
public class StartCommand extends BotCommand {
    public StartCommand() {
        super("start", "With this command you can start the Bot");
    }

    @Override
    public void execute(TelegramClient telegramClient, User user, Chat chat, String[] strings) {
        DatabaseManager databseManager = DatabaseManager.getInstance();
        StringBuilder messageBuilder = new StringBuilder();

        String userName = user.getFirstName() + " " + user.getLastName();

        if (databseManager.getUserStateForCommandsBot(user.getId())) {
            messageBuilder.append("Hi ").append(userName).append("\n");
            messageBuilder.append("i think we know each other already!");
        } else {
            databseManager.setUserStateForCommandsBot(user.getId(), true);
            messageBuilder.append("Welcome ").append(userName).append("\n");
            messageBuilder.append("this bot will demonstrate you the command feature of the Java TelegramBots API!");
        }

        SendMessage answer = new SendMessage(chat.getId().toString(), messageBuilder.toString());

        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error", e);
        }
    }
}