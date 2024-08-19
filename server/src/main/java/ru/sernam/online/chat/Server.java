package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.system.database.AuthenticationProvider;
import ru.sernam.online.chat.system.database.DataBaseHandler;
import ru.sernam.online.chat.system.database.JDBCAuthenticationProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private final int port;
    private ServerSocket serverSocket;
    private Connection connection;  //коннекшн для базы данных
    private final List<ClientHandler> clients;  //спиок клиентов
    private final SystemCommandHandler systemCommandHandler;    //обработчик команд
    private final AuthenticationProvider authenticationProvider;    //сервис регисрации
    private final DataBaseHandler dataBaseHandler;  //работа с базой банных
    private final SessionHandler sessionHandler;    //следит за активностью пользователей



    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IOException("Не удается найти config.properties");
            }
            Properties properties = new Properties();
            properties.load(input);
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            this.connection = DriverManager.getConnection(url, username, password);
            logger.info("Установлено соединение с базой данных");
        } catch (IOException | SQLException e) {
            logger.error("Ошибка при установлении соединения с базой данных", e);
        }

        this.dataBaseHandler = new DataBaseHandler(connection);
        this.authenticationProvider = new JDBCAuthenticationProvider(this);

        this.systemCommandHandler = new SystemCommandHandler(this);
        this.sessionHandler = new SessionHandler(this);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Сервер запущен на порту: {}", port);
            authenticationProvider.initialize();
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.info("Новое подключение на порту: {}", port);
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            logger.error("Ошибка в работе сервера", e);
        } finally {
            shutDown(); // Закрытие всех ресурсов
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.add(clientHandler);
        logger.info("Новый пользователь подключен к рассылке: {}", clientHandler.getUsername());
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        if (clientHandler.isAuthenticated()) {
            broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
        }
        logger.info("Пользователь отключен от рассылки: {}", clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message, true);
        }
    }

    public SystemCommandHandler getSystemCommandHandler() {
        return systemCommandHandler;
    }

    public SessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public DataBaseHandler getDataBaseHandler() {
        return dataBaseHandler;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public ClientHandler getClientByUsername(String user) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(user)) {
                return c;
            }
        }
        return null;
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void shutDown() {
        logger.info("Остановка сервера...");

        // Закрываем все клиентские соединения
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setAuthenticated(false);
            clients.get(i).sendMessage("/disconnected");
            clients.get(i).disconnect();
        }

        clients.clear(); // Очистка списка клиентов
        logger.info("Список клиентов очищен");
        // Закрываем обработчик сессий
        sessionHandler.shutdown();
        logger.info("Остановлен обработчик сессий");

        // Закрываем соединение с базой данных
        if (connection != null) {
            try {
                connection.close();
                logger.info("Соединение с базой данных закрыто.");
            } catch (SQLException e) {
                logger.error("Ошибка при закрытии соединения с базой данных.", e);
            }
        }

        // Закрываем серверный сокет, если он не закрыт
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("ServerSocket закрыт.");
            } catch (IOException e) {
                logger.error("Ошибка при закрытии ServerSocket.", e);
            }
        }
    }
}
