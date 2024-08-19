package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionHandler {
    private final Map<ClientHandler, Long> clientActivityMap = new ConcurrentHashMap<>();
    private final Server server;
    private final ScheduledExecutorService scheduler;
    private static final Logger logger = LogManager.getLogger(SessionHandler.class);

    public SessionHandler(Server server) {
        this.server = server;
        this.scheduler = Executors.newScheduledThreadPool(1);
        startSessionChecker();
        logger.info("Обработчик сессий инициализирован");
    }

    public void updateClientActivity(ClientHandler clientHandler) {
        clientActivityMap.put(clientHandler, System.currentTimeMillis());
        logger.debug("Обновлена активность клиента: {}", clientHandler.getUsername());
    }

    public void removeClient(ClientHandler clientHandler) {
        clientActivityMap.remove(clientHandler);
        logger.info("Пользователь удален из списка обработчика: {}", clientHandler.getUsername());
    }

    private void startSessionChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<ClientHandler, Long> entry : clientActivityMap.entrySet()) {
                if (currentTime - entry.getValue() > TimeUnit.MINUTES.toMillis(20)) {
                    ClientHandler client = entry.getKey();
                    logger.warn("Пользователь {} отключен за неактивность.", client.getUsername());
                    client.sendMessage("Вы были отключены за неактивность.", true);
                    client.sendMessage("/exitok");
                    client.setAuthenticated(false);
                    client.disconnect();
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
        logger.info("Запущен обработчик сессий");
    }

    public void shutdown() {
        scheduler.shutdownNow();
        logger.info("Обработчик сессий остановлен");
    }
}
