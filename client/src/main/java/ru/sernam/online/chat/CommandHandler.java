package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.system.commands.*;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {
    private static final Logger logger = LogManager.getLogger(CommandHandler.class);

    private final Client client;
    private final Map<String, SystemCommand> commands;

    public CommandHandler(Client client) {
        this.client = client;
        this.commands = new HashMap<>();

        commands.put("/exitok", new ExitCommandConf());
        commands.put("/authok", new AuthCommandConf());
        commands.put("/regok", new RegisterCommandConf());
        commands.put("/kicked", new KickedCommand());
        commands.put("/disconnected", new DisconnectedCommand());

        logger.info("Команды успешно инициализированы.");
    }

    public void handleCommand(String message) {
        String[] elements = message.split(" ");
        String commandKey = elements[0];

        SystemCommand systemCommand = commands.get(commandKey);
        if (systemCommand != null) {
            logger.info("Обработка команды: {}", commandKey);
            systemCommand.execute(client, elements);
        } else {
            logger.warn("Получена неизвестная команда: {}", commandKey);
        }
    }
}
