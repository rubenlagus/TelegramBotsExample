package org.telegram.commands;

import lombok.extern.slf4j.Slf4j;
import org.telegram.database.DatabaseManager;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * This command helps the user to find the command they need
 *
 * @author Timo Schulz (Mit0x2)
 */
@Slf4j
public class HelpCommand extends BotCommand {

    private static final String LOGTAG = "HELPCOMMAND";

    private final ICommandRegistry commandRegistry;

    public HelpCommand(ICommandRegistry commandRegistry) {
        super("help", "Get all the commands this bot provides");
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void execute(TelegramClient telegramClient, User user, Chat chat, String[] strings) {

        if (!DatabaseManager.getInstance().getUserStateForCommandsBot(user.getId())) {
            return;
        }

        StringBuilder helpMessageBuilder = new StringBuilder("<b>Help</b>\n");
        helpMessageBuilder.append("These are the registered commands for this Bot:\n\n");

        for (IBotCommand botCommand : commandRegistry.getRegisteredCommands()) {
            helpMessageBuilder.append(botCommand.toString()).append("\n\n");
        }

        SendMessage helpMessage = new SendMessage(chat.getId().toString(), helpMessageBuilder.toString());
        helpMessage.enableHtml(true);

        try {
            telegramClient.execute(helpMessage);
        } catch (TelegramApiException e) {
            log.error("Error", e);
        }
    }
}
