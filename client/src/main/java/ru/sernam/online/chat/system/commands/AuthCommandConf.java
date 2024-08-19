package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.Client;

public class AuthCommandConf implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(AuthCommandConf.class);
    private final String commandFormat = "Некорректный ответ от сервера.";

    @Override
    public void execute(Client client, String[] args) {
        if (args.length != 2) {
            System.out.println(commandFormat);
            logger.warn("Получен некорректный ответ от сервера для команды /authok: {}", String.join(" ", args));
            return;
        }
        System.out.println("Удалось успешно войти в чат под именем пользователя: " + args[1]);
        logger.info("Пользователь успешно вошел в чат под именем: {}", args[1]);
    }
}
