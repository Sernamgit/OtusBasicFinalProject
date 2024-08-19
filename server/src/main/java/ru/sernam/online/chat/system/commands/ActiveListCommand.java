package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

import java.util.List;

public class ActiveListCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(ActiveListCommand.class);

    private Server server;

    public ActiveListCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 1) {
            clientHandler.sendMessage("Неверный формат команды /activelist");
            logger.warn("Неверный формат команды /activelist: {}", String.join(" ", args));
            return;
        }

        List<ClientHandler> clients = server.getClients();
        StringBuilder message = new StringBuilder("Список активных пользователей: " + System.lineSeparator());

        for (ClientHandler client : clients) {
            message.append(client.getUsername()).append(System.lineSeparator());
        }

        clientHandler.sendMessage(message.toString());
        logger.info("Отправлен список активных пользователей для пользователя {}", clientHandler.getUsername());
    }
}
