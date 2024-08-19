package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

public class ChangeUsernameCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(ChangeUsernameCommand.class);

    private final Server server;
    private final String commandFormat = "Неверный формат команды изменить имя (/changenick username)";

    public ChangeUsernameCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 2) {
            clientHandler.sendMessage(commandFormat);
            logger.warn("Пользователь {} ввел неверный формат команды: {}", clientHandler.getUsername(), commandFormat);
            return;
        }

        String newUsername = args[1];
        if (server.getDataBaseHandler().changeUsername(clientHandler, newUsername)) {
            clientHandler.setUsername(newUsername);
            clientHandler.sendMessage("Имя пользователя успешно изменено на: " + newUsername);
          } else {
            logger.warn("Не удалось изменить имя пользователя {} на {}", clientHandler.getUsername(), newUsername);
        }
    }
}
