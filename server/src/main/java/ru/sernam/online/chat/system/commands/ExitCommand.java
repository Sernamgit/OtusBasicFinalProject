package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;

public class ExitCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(KickCommand.class);

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 1) {
            clientHandler.sendMessage("Неверный формат команды /exit");
            logger.info("Неверный формат команды /exit");
            return;
        }
        clientHandler.sendMessage("/exitok");
        clientHandler.disconnect();
        clientHandler.setAuthenticated(false);
    }
}
