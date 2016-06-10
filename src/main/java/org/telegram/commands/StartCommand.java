package org.telegram.commands;

import org.telegram.database.DatabaseManager;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.logging.BotLogger;

/**
 * This commands starts the conversation with the bot
 *
 * @author Timo Schulz (Mit0x2)
 */
public class StartCommand extends BotCommand {

    public static final String LOGTAG = "STARTCOMMAND";

    public StartCommand() {
        super("start", "With this command you can start the Bot");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
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

        SendMessage answer = new SendMessage();
        answer.setChatId(chat.getId().toString());
        answer.setText(messageBuilder.toString());

        try {
            absSender.sendMessage(answer);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }
}