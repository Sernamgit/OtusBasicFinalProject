package ru.sernam.online.chat.system.commands;
import ru.sernam.online.chat.Client;

public interface SystemCommand {
    void execute(Client client, String[] args);
}
