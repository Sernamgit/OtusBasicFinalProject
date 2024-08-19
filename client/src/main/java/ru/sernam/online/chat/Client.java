package ru.sernam.online.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final Logger logger = LogManager.getLogger(Client.class);

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean disconnected = false;
    CommandHandler commandHandler;

    public Client() throws IOException {
        commandHandler = new CommandHandler(this);
        Scanner scanner = new Scanner(System.in);

        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            logger.info("Соединение с сервером установлено.");

            new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/")) {
                            commandHandler.handleCommand(message);
                            if (disconnected) {
                                break;
                            }
                            continue;
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при чтении сообщения с сервера.", e);
                } finally {
                    disconnect();
                }
            }).start();

            while (!disconnected) {
                String message = scanner.nextLine();
                out.writeUTF(message);
                if (message.equals("/")) {
                    logger.info("Отправлена системная команда: {}", message);
                }

            }
        } catch (IOException e) {
            logger.error("Ошибка при попытке установить соединение с сервером.", e);
            disconnect();
            throw e;
        }
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    private void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии DataInputStream.", e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии DataOutputStream.", e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии сокета.", e);
        }
        logger.info("Соединение закрыто.");
    }
}
