package ru.sernam.online.chat.system.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataBaseHandler {
    private static final Logger logger = LogManager.getLogger(DataBaseHandler.class);

    private final Connection connection;

    public DataBaseHandler(Connection connection) {
        this.connection = connection;
    }

    //регистрация
    public synchronized boolean register(String login, String password, String username) {
        try {
            String insertUserQuery = "INSERT INTO Users (login, password, username) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertUserQuery, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                preparedStatement.setString(3, username);
                preparedStatement.executeUpdate();

                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);

                    String insertUserRoleQuery = "INSERT INTO user_to_role (user_id, role_id) VALUES (?, (SELECT id FROM Role WHERE role = 'user'))";
                    try (PreparedStatement preparedStatementRole = connection.prepareStatement(insertUserRoleQuery)) {
                        preparedStatementRole.setInt(1, userId);
                        preparedStatementRole.executeUpdate();
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации пользователя: логин = {}, имя = {}", login, username, e);
            return false;
        }
    }


    public synchronized String getUsernameByLoginAndPassword(String login, String password) {
        String query = "SELECT username FROM Users WHERE login = ? AND password = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении имени пользователя по логину и паролю: логин = {}", login, e);
        }
        return null;
    }

    public synchronized boolean isLoginAlreadyExist(String login) {
        String query = "SELECT COUNT(*) FROM Users WHERE login = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования логина: логин = {}", login, e);
        }
        return false;
    }

    public synchronized boolean isUsernameAlreadyExist(String username) {
        String query = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования имени пользователя: имя = {}", username, e);
        }
        return false;
    }

    public synchronized boolean changeUsername(ClientHandler currentClient, String newUsername) {
        if (isUsernameAlreadyExist(newUsername)) {
            currentClient.sendMessage(String.format("Имя пользователя \"%s\" уже занято", newUsername));
            logger.warn("Попытка изменения имени пользователя на уже занятое: {}", newUsername);
            return false;
        }
        String query = "UPDATE Users SET username = ? WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newUsername);
            preparedStatement.setString(2, currentClient.getUsername());
            preparedStatement.executeUpdate();
            logger.info("Имя пользователя успешно изменено с {} на {}", currentClient.getUsername(), newUsername);
            return true;
        } catch (SQLException e) {
            logger.error("Ошибка при изменении имени пользователя: текущее имя = {}, новое имя = {}", currentClient.getUsername(), newUsername, e);
        }
        return false;
    }

    public synchronized Role getUserRole(String username) {
        String query = "SELECT role.role, users.username FROM public.user_to_role " +
                "JOIN public.role ON role.id = user_to_role.role_id " +
                "JOIN public.users ON user_to_role.user_id = users.id " +
                "WHERE users.username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String role = resultSet.getString("role");
                if (role != null) {
                    logger.debug("Роль пользователя {}: {}", username, role.toUpperCase());
                    return Role.valueOf(role.toUpperCase());
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении роли пользователя: имя = {}", username, e);
        }
        return null;
    }

    public synchronized List<String> getRights(Role role) {
        List<String> result = new ArrayList<>();
        String query = "SELECT commands.command FROM commands " +
                "JOIN rights ON commands.id = rights.command_id " +
                "JOIN role ON role.id = rights.role_id " +
                "WHERE role.role = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, role.toString().toLowerCase());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getString("command"));
            }
            logger.debug("Права для роли {}: {}", role, result);
        } catch (SQLException e) {
            logger.error("Ошибка при получении прав для роли: роль = {}", role, e);
        }
        return result;
    }

    //перманентный бан
    public synchronized void setRestriction(String username) {
        String updateUserRestrictionSQL = "UPDATE Users SET restriction = TRUE WHERE username = ?";
        String insertBanSQL = "INSERT INTO UserRestrictions (user_id, restriction_type) " +
                "VALUES ((SELECT id FROM Users WHERE username = ?), 'permanent')";

        try (PreparedStatement updateUserStmt = connection.prepareStatement(updateUserRestrictionSQL);
             PreparedStatement insertBanStmt = connection.prepareStatement(insertBanSQL)) {

            updateUserStmt.setString(1, username);
            int updatedRows = updateUserStmt.executeUpdate();

            if (updatedRows == 0) {
                logger.warn("Пользователь с именем {} не найден для установки перманентного бана.", username);
                return;
            }

            insertBanStmt.setString(1, username);
            insertBanStmt.executeUpdate();
            logger.info("Перманентный бан установлен для пользователя {}", username);

        } catch (SQLException e) {
            logger.error("Ошибка при установке перманентного бана для пользователя: имя = {}", username, e);
        }
    }

    //временный бан
    public synchronized void setRestriction(String username, Timestamp restrictionUntil) {
        String updateUserQuery = "UPDATE Users SET restriction = TRUE WHERE username = ?";
        String insertBanQuery = "INSERT INTO UserRestrictions (user_id, restriction_type, restriction_until) " +
                "VALUES ((SELECT id FROM Users WHERE username = ?), 'temporary', ?)";

        try (PreparedStatement updateUserStatement = connection.prepareStatement(updateUserQuery);
             PreparedStatement insertBanStatement = connection.prepareStatement(insertBanQuery)) {

            updateUserStatement.setString(1, username);
            int updatedRows = updateUserStatement.executeUpdate();

            if (updatedRows == 0) {
                logger.warn("Пользователь с именем {} не найден для установки временного бана.", username);
                return;
            }

            insertBanStatement.setString(1, username);
            insertBanStatement.setTimestamp(2, restrictionUntil);
            insertBanStatement.executeUpdate();
            logger.info("Временный бан установлен для пользователя {} до {}", username, restrictionUntil);

        } catch (SQLException e) {
            logger.error("Ошибка при установке временного бана для пользователя: имя = {}", username, e);
        }
    }

    //проверка на наличие ограничений\бана
    public synchronized Boolean getRestrictionByLogin(String login) {
        String query = "SELECT restriction FROM Users WHERE login = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    boolean restriction = resultSet.getBoolean("restriction");
                    logger.info("Проверка ограничения для пользователя с логином {}: {}", login, restriction);
                    return restriction;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке ограничения по логину: логин = {}", login, e);
        }
        return null;
    }

    //получение ограничений\бана
    public synchronized Timestamp getBanRestriction(String login) {
        String query = "SELECT restriction_type, restriction_until FROM UserRestrictions " +
                "JOIN Users ON UserRestrictions.user_id = Users.id " +
                "WHERE Users.login = ? " +
                "ORDER BY UserRestrictions.id DESC LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, login);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String restrictionType = resultSet.getString("restriction_type");
                    Timestamp restrictionUntil = resultSet.getTimestamp("restriction_until");

                    if ("permanent".equalsIgnoreCase(restrictionType)) {
                        logger.info("Пользователь с логином {} имеет перманентный бан.", login);
                        return new Timestamp(-1);
                    } else if ("temporary".equalsIgnoreCase(restrictionType) && restrictionUntil != null) {
                        logger.info("Пользователь с логином {} имеет временный бан до {}", login, restrictionUntil);
                        return restrictionUntil;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении информации о бане по логину: логин = {}", login, e);
        }

        return null;
    }

    public synchronized void clearRestriction(String username) {
        String clearRestrictionQuery = "DELETE FROM UserRestrictions WHERE user_id = (SELECT id FROM Users WHERE username = ?)";
        String resetUserRestrictionQuery = "UPDATE Users SET restriction = FALSE WHERE username = ?";

        try (PreparedStatement clearRestrictionStmt = connection.prepareStatement(clearRestrictionQuery);
             PreparedStatement resetUserStmt = connection.prepareStatement(resetUserRestrictionQuery)) {
            connection.setAutoCommit(false);

            clearRestrictionStmt.setString(1, username);
            int rowsAffected = clearRestrictionStmt.executeUpdate();

            if (rowsAffected > 0) {
                resetUserStmt.setString(1, username);
                resetUserStmt.executeUpdate();
                connection.commit();
                logger.info("Ограничение пользователя {} удалено.", username);
            } else {
                connection.rollback();
                logger.warn("Не удалось найти ограничения для пользователя {}.", username);
            }

        } catch (SQLException e) {
            logger.error("Ошибка при снятии ограничения для пользователя: имя = {}", username, e);
        }
    }
}
