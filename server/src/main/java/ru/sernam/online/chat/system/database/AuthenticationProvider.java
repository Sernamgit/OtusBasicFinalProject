package ru.sernam.online.chat.system.database;

import ru.sernam.online.chat.ClientHandler;


public interface AuthenticationProvider {
    void initialize() throws ClassNotFoundException;

    boolean authenticate(ClientHandler clientHandler, String login, String password);

    boolean registration(ClientHandler clientHandler, String login, String password, String username);
}
