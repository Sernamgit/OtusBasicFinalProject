package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

public class KickCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(KickCommand.class);

    private final Server server;

    public KickCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 2) {
            clientHandler.sendMessage("Неверный формат команды /kick");
            logger.info("Неверный формат команды /kick");
            return;
        }
        if (clientHandler.getUsername().equals(args[1])) {
            clientHandler.sendMessage("Невозможно кикнуть самого себя.");
            logger.debug("Попытка кикнуть самого себя");
            return;
        }

        ClientHandler clientToKick = server.getClientByUsername(args[1]);

        if (clientToKick != null) {
            clientToKick.sendMessage("/kicked");
            clientToKick.setAuthenticated(false);
            clientToKick.disconnect();
            server.broadcastMessage(String.format("Пользователь %s был отключен администратором.", args[1]));
            logger.info("Пользователь {} был отключен администратором.", args[1]);
        } else {
            clientHandler.sendMessage("Пользователь не найден");
            logger.warn("Попытка отключения несуществующего пользователя {}", args[1]);
        }
    }
}
