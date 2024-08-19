package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.system.commands.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemCommandHandler {
    private static final Logger logger = LogManager.getLogger(SystemCommandHandler.class);

    private final Server server;
    private final Rights rights;
    private final Map<String, SystemCommand> commands;

    public SystemCommandHandler(Server server) {
        this.server = server;
        this.rights = new Rights(server);
        this.commands = new HashMap<>();

        // список команд
        commands.put("/auth", new AuthCommand(server));
        commands.put("/register", new RegisterCommand(server));
        commands.put("/exit", new ExitCommand());
        commands.put("/w", new WisperCommand(server));
        commands.put("/kick", new KickCommand(server));
        commands.put("/activelist", new ActiveListCommand(server));
        commands.put("/changenick", new ChangeUsernameCommand(server));
        commands.put("/ban", new BanCommand(server));
        commands.put("/shutdown", new ShutDownCommand(server));

        logger.info("Инициализирован обработчик системных команд");
    }

    public void handleCommand(ClientHandler clientHandler, String message) {
        String[] elements = message.split(" ");
        String commandKey = elements[0];

        SystemCommand systemCommand = commands.get(commandKey);
        if (systemCommand != null) {
            if (checkRights(clientHandler.getRole(), commandKey)) {
                logger.debug("Пользователь {} выполняет команду: {}", clientHandler.getUsername(), commandKey);
                systemCommand.execute(clientHandler, elements);
            } else {
                logger.warn("Пользователь {} пытался выполнить команду {} без достаточных прав", clientHandler.getUsername(), commandKey);
                clientHandler.sendMessage("Недостаточно прав.");
            }
        } else {
            logger.warn("Пользователь {} пытался выполнить неизвестную команду: {}", clientHandler.getUsername(), commandKey);
            clientHandler.sendMessage("Неизвестная команда.");
        }
    }

    private boolean checkRights(Role role, String command) {
        List<String> result = rights.getRights(role);
        boolean hasRights = result.contains(command);
        logger.debug("Проверка прав для команды {}: {}", command, hasRights);
        return hasRights;
    }
}
