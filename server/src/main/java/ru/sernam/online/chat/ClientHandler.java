package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private Role role = Role.GUEST;
    private boolean authenticated = false;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        server.getSessionHandler().updateClientActivity(this);

        new Thread(() -> {
            try {
                logger.info("Подключился новый клиент: {}", socket);
                sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username'");
                authenticateClient();
                messenger();
            } catch (IOException e) {
                logger.error("Ошибка в обработке клиента", e);
            } finally {
                disconnect();
            }
        }).start();
    }

    // Аутентификация
    private void authenticateClient() throws IOException {
        while (true) {
            String message = in.readUTF();
            server.getSessionHandler().updateClientActivity(this);
            if (message.startsWith("/")) {
                server.getSystemCommandHandler().handleCommand(this, message);
                if (authenticated) {
                    break;
                }
            }
        }
    }

    // Принятие сообщений + рассылка
    private void messenger() throws IOException {
        while (true) {
            String message = in.readUTF();
            server.getSessionHandler().updateClientActivity(this);
            if (message.startsWith("/")) {
                server.getSystemCommandHandler().handleCommand(this, message);
                if (!authenticated) {
                    logger.info("Изменен статус пользователя {}, завершение сеанса", username);
                    break;
                }
                continue;
            }
            server.broadcastMessage(username + ": " + message);
        }
    }

    // Отправка сообщений
    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            logger.error("Ошибка при отправке сообщения пользователю: {}", username, e);
        }
    }

    // Отправка сообщений + время
    public void sendMessage(String message, boolean includeTimestamp) {
        try {
            if (includeTimestamp) {
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                out.writeUTF(String.format("[%s] %s", timestamp, message));
            } else {
                out.writeUTF(message);
            }
        } catch (IOException e) {
            logger.error("Ошибка при отправке сообщения с временем пользователю: {}", username, e);
        }
    }

    // Дисконнект
    public void disconnect() {
        server.unsubscribe(this);
        server.getSessionHandler().removeClient(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии DataInputStream для пользователя: {}", username, e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии DataOutputStream для пользователя: {}", username, e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии Socket для пользователя: {}", username, e);
        }
        logger.info("Пользователь {} отключен", username);
    }
}
