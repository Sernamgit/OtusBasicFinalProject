package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

import java.sql.Timestamp;

public class BanCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(BanCommand.class);

    private final Server server;
    private final String commandFormat = "Неверный формат команды. Используйте: /ban username -p для перманентного бана или /ban username -m int (-h -d -y) для временного бана";

    public BanCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length  < 3 || args.length > 4) {
            clientHandler.sendMessage(commandFormat);
            logger.warn("Неверный формат команды: {}", String.join(" ", args));
            return;
        }

        String username = args[1];
        String banType = args[2];

        ClientHandler clientToBan = server.getClientByUsername(username);

        if (clientToBan == null) {
            clientHandler.sendMessage("Пользователь не найден");
            logger.warn("Пользователь {} не найден.", username);
            return;
        }

        try {
            if (args.length == 3 && "-p".equals(banType)) {
                banPermanent(server, clientToBan);
            } else if (args.length == 4) {
                long duration = parseDuration(args[3], banType);
                banTime(server, clientToBan, duration);
            } else {
                clientHandler.sendMessage(commandFormat);
                logger.warn("Неверный тип бана: {}", banType);
            }
        } catch (NumberFormatException e) {
            logger.error("Ошибка при парсинге продолжительности: {}", args[3], e);
            clientHandler.sendMessage("Ошибка при попытке забанить пользователя. Неверный формат времени.");
        }
    }

    private void banPermanent(Server server, ClientHandler clientToBan) {
        server.getDataBaseHandler().setRestriction(clientToBan.getUsername());
        clientToBan.sendMessage("/disconnected");
        clientToBan.setAuthenticated(false);
        clientToBan.disconnect();
        logger.info("Пользователь {} был перманентно забанен администратором.", clientToBan.getUsername());
        server.broadcastMessage(String.format("Пользователь %s был перманентно забанен администратором.", clientToBan.getUsername()));
    }

    private void banTime(Server server, ClientHandler clientToBan, long time) {
        Timestamp restrictionUntil = new Timestamp(System.currentTimeMillis() + time);
        server.getDataBaseHandler().setRestriction(clientToBan.getUsername(), restrictionUntil);
        clientToBan.sendMessage("/disconnected");
        clientToBan.setAuthenticated(false);
        clientToBan.disconnect();
        logger.info("Пользователь {} был забанен администратором до {}.", clientToBan.getUsername(), restrictionUntil);
        server.broadcastMessage(String.format("Пользователь %s забанен администратором до %s.", clientToBan.getUsername(), restrictionUntil));
    }

    private long parseDuration(String duration, String banType) throws NumberFormatException {
        long value = Long.parseLong(duration);

        switch (banType) {
            case "-m": return value * 60 * 1000;             // Минуты
            case "-h": return value * 60 * 60 * 1000;        // Часы
            case "-d": return value * 24 * 60 * 60 * 1000;   // Дни
            case "-y": return value * 365 * 24 * 60 * 60 * 1000; // Годы
            default: throw new NumberFormatException("Неверный формат времени.");
        }
    }
}
