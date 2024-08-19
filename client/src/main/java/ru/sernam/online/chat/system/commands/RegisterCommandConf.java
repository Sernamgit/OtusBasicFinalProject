package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.Client;
import ru.sernam.online.chat.CommandHandler;


public class RegisterCommandConf implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(CommandHandler.class);


    @Override
    public void execute(Client client, String[] args) {

        logger.info("Удалось успешно пройти регистрацию и войти в чат под именем пользователя: {}", args[1]);
        System.out.println("Удалось успешно пройти регистрацию и войти в чат под именем пользователя: " + args[1]);
    }
}
