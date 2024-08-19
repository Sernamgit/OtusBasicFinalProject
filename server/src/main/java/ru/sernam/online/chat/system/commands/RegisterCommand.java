package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

public class RegisterCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(RegisterCommand.class);

    private final Server server;

    public RegisterCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        logger.info("Попытка новой регистрации");

        if (args.length != 4) {
            clientHandler.sendMessage("Неверный формат команды /register");
            logger.warn("Неверный формат команды /register");
            return;
        }

        String login = args[1];
        String password = args[2];
        String username = args[3];

        server.getAuthenticationProvider().registration(clientHandler, login, password, username);
    }
}
