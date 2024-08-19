package ru.sernam.online.chat.system.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Role;
import ru.sernam.online.chat.Server;

import java.sql.*;

public class JDBCAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LogManager.getLogger(JDBCAuthenticationProvider.class);

    private final Server server;

    public JDBCAuthenticationProvider(Server server) {
        this.server = server;
    }

    @Override
    public void initialize() {
        logger.info("Сервис аутентификации запущен: JDBC режим (PostgreSQL)");
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        // Проверка корректности данных получением имени пользователя из базы
        String authUsername = server.getDataBaseHandler().getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            logger.warn("Попытка аутентификации с некорректным логином/паролем: логин = {}", login);
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }

        // Проверка занятости учетки
        if (server.isUsernameBusy(authUsername)) {
            logger.warn("Попытка аутентификации с уже занятым логином: {}", authUsername);
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }

        // Проверка на бан
        if (server.getDataBaseHandler().getRestrictionByLogin(login)) {
            Timestamp restrictionUntil = server.getDataBaseHandler().getBanRestriction(login);
            if (restrictionUntil != null) {
                if (restrictionUntil.getTime() == -1) {
                    // Пользователь забанен перманентно
                    logger.warn("Пользователь {} забанен навсегда", login);
                    clientHandler.sendMessage("Ваша учетная запись забанена навсегда.");
                    return false;
                } else if (restrictionUntil.before(new Timestamp(System.currentTimeMillis()))) {
                    // Временный бан истёк, очистка статуса
                    server.getDataBaseHandler().clearRestriction(authUsername);
                } else {
                    // Временный бан ещё активен
                    logger.warn("Пользователь {} забанен до {}", login, restrictionUntil);
                    clientHandler.sendMessage("Ваша учетная запись забанена до " + restrictionUntil.toString());
                    return false;
                }
            }
        }

        // Назначаем имя, роль и подключаем к рассылке
        clientHandler.setUsername(authUsername);
        clientHandler.setAuthenticated(true);
        Role role = server.getDataBaseHandler().getUserRole(authUsername);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);

        logger.info("Пользователь {} успешно аутентифицирован", authUsername);
        return true;
    }

    @Override
    public synchronized boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        // Проверки на формат и доступность учетных данных
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            logger.warn("Попытка регистрации с некорректными данными: логин = {}, пароль = {}, имя = {}", login, password, username);
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        if (server.getDataBaseHandler().isLoginAlreadyExist(login)) {
            logger.warn("Попытка регистрации с уже занятым логином: {}", login);
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (server.getDataBaseHandler().isUsernameAlreadyExist(username)) {
            logger.warn("Попытка регистрации с уже занятым именем пользователя: {}", username);
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        if (!server.getDataBaseHandler().register(login, password, username)) {
            clientHandler.sendMessage("Ошибка при регистрации пользователя");
            return false;
        }

        // Входим. Назначаем имя, права. Подключаем к рассылке
        clientHandler.setUsername(username);
        clientHandler.setRole(Role.USER);
        clientHandler.setAuthenticated(true);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);

        logger.info("Пользователь {} успешно зарегистрирован", username);
        return true;
    }
}
