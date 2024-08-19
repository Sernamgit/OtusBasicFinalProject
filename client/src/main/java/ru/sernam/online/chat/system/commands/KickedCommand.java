package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.Client;

public class KickedCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(KickedCommand.class);
    private final String commandFormat = "Некорректный ответ от сервера.";

    @Override
    public void execute(Client client, String[] args) {
        if (args.length != 1) {
            System.out.println(commandFormat);
            logger.warn("Получен некорректный ответ от сервера для команды /kicked: {}", String.join(" ", args));
            return;
        }
        System.out.println("Вы были отключены администратором.");
        client.setDisconnected(true);
        logger.info("Пользователь был отключен администратором.");
    }
}
