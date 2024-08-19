package ru.sernam.online.chat.system.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sernam.online.chat.ClientHandler;
import ru.sernam.online.chat.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShutDownCommand implements SystemCommand {
    private static final Logger logger = LogManager.getLogger(ShutDownCommand.class);

    private final Server server;
    private ScheduledExecutorService scheduler;

    private final String commandFormat = "Неверный формат команды.";
    private final String shutdownWarningMessage = "Сервер будет остановлен через 1 минуту!";
    private final String shutdownCountdownMessage = "Сервер будет остановлен через %d секунд!";
    private final String shutdownMessage = "Сервер остановлен.";

    public ShutDownCommand(Server server) {
        this.server = server;
    }

    @Override
    public void execute(ClientHandler clientHandler, String[] args) {
        if (args.length != 1) {
            clientHandler.sendMessage(commandFormat);
            logger.warn("Неправильный формат команды shutdown от пользователя: {}", clientHandler.getUsername());
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        // Отправляем предупреждение всем клиентам
        server.broadcastMessage(shutdownWarningMessage);
        logger.info("Отправлено предупреждение о предстоящем отключении сервера.");

        scheduler.schedule(new Runnable() {
            int countdown = 10;

            @Override
            public void run() {
                try {
                    if (countdown > 0) {
                        server.broadcastMessage(String.format(shutdownCountdownMessage, countdown));
                        logger.info("Осталось {} секунд до отключения сервера.", countdown);
                        countdown--;

                        // Планируем следующую итерацию через 1 секунду
                        scheduler.schedule(this, 1, TimeUnit.SECONDS);
                    } else {
                        // отключение после отсчета
                        server.broadcastMessage(shutdownMessage);
                        logger.info("Сервер отключается.");
                        server.shutDown();
                        scheduler.shutdown();
                    }
                } catch (Exception e) {
                    logger.error("Ошибка во время выполнения команды shutdown: ", e);
                }
            }
        }, 50, TimeUnit.SECONDS); // Начинаем обратный отсчет за 10 секунд до окончания времени ожидания
    }
}
