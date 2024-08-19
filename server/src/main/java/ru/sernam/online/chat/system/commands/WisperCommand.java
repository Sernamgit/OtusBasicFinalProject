package ru.sernam.online.chat.system.commands;

import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class WisperCommand implements SystemCommand{
    private static final Logger logger = LogManager.getLogger(WisperCommand.class);

    private Server server;
    private final String commandFormat = "Неверный формат команды wisper (/w username)";

    public WisperCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler senderClient, String[] args) {
        if (args.length < 3) {
            senderClient.sendMessage(commandFormat);
            logger.warn("Пользователь {} ввел команду с неправильным форматом: {}", senderClient.getUsername(), String.join(" ", args));
            return;
        }
        String privateMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        ClientHandler destClient = server.getClientByUsername(args[1]);
        if (destClient != null){
            destClient.sendMessage(String.format("Личное сообщение от %s: %s", senderClient.getUsername(), privateMessage ), true);
            senderClient.sendMessage(String.format("Отправлено личное сообщение %s: %s", destClient.getUsername(), privateMessage ) , true);
        } else {
            senderClient.sendMessage("Пользователь не найден");
            logger.warn("Пользователь {} не найден для отправки личного сообщения от {}", args[1], senderClient.getUsername());
        }
    }
}

