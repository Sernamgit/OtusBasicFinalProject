package ru.sernam.online.chat.system.commands;

import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(AuthCommand.class);

    private Server server;

    public AuthCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 3) {
            clientHandler.sendMessage("Неверный формат команды /auth");
            logger.warn("Неверный формат команды /auth: {}", String.join(" ", args));
            return;
        }
        server.getAuthenticationProvider().authenticate(clientHandler, args[1], args[2]);
    }
}
