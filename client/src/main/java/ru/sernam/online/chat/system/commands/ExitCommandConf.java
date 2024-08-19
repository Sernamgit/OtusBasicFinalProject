package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.Client;

public class ExitCommandConf implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(ExitCommandConf.class);
    private final String commandFormat = "Некорректный ответ от сервера.";

    @Override
    public void execute(Client client, String[] args) {
        if (args.length != 1) {
            System.out.println(commandFormat);
            logger.warn("Получен некорректный ответ от сервера для команды /exitok: {}", String.join(" ", args));
            return;
        }
        client.setDisconnected(true);
        System.out.println("Выход выполнен успешно");
        logger.info("Пользователь успешно вышел из чата.");
    }
}
