package ru.sernam.online.chat;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Rights {
    private static final Logger logger = LogManager.getLogger(Rights.class);

    private Server server;
    private final Map<Role, List> roleMap;
    private List<String> userRights;
    private List<String> adminRights;
    private List<String> guestRights;

    public Rights(Server server) {
        this.server = server;
        this.roleMap = new HashMap<>();

        this.guestRights = server.getDataBaseHandler().getRights(Role.GUEST);
        this.userRights = server.getDataBaseHandler().getRights(Role.USER);
        this.adminRights = server.getDataBaseHandler().getRights(Role.ADMIN);

        roleMap.put(Role.GUEST, guestRights);
        roleMap.put(Role.USER, userRights);
        roleMap.put(Role.ADMIN, adminRights);
    }

    public void updateRights() {
        guestRights = server.getDataBaseHandler().getRights(Role.GUEST);
        userRights = server.getDataBaseHandler().getRights(Role.USER);
        adminRights = server.getDataBaseHandler().getRights(Role.ADMIN);
        logger.debug("Правка для всех ролей обновлены");
    }

    public List<String> getRights(Role role) {
        logger.debug("Получение прав для роли: {}", role);
        return roleMap.get(role);
    }
}
