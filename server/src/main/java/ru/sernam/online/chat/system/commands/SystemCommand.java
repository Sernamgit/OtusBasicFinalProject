package ru.sernam.online.chat.system.commands;

import ru.sernam.online.chat.ClientHandler;

public interface SystemCommand {
    void execute(ClientHandler clientHandler, String[] args);
}
