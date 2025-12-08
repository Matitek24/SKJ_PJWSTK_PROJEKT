package command;

import discovery.KeyDiscovery;
import model.ServerInfo;

import java.util.List;
import java.util.Set;


public class CommandProcessor {
    private final KeyDiscovery keyDiscovery;
    private final ServerForwarder serverForwarder;
    private final List<ServerInfo> servers;

    public CommandProcessor(KeyDiscovery keyDiscovery, List<ServerInfo> servers) {
        this.keyDiscovery = keyDiscovery;
        this.serverForwarder = new ServerForwarder();
        this.servers = servers;
    }


    public String processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "NA";
        }

        String[] parts = command.trim().split("\\s+");
        String cmd = parts[0];

        switch (cmd) {
            case "GET":
                return handleGet(parts);

            case "SET":
                return handleSet(parts);

            case "QUIT":
                handleQuit();
                return "";

            default:
                return "NA";
        }
    }


    private String handleGet(String[] parts) {
        if (parts.length < 2) {
            return "NA";
        }

        if (parts[1].equals("NAMES")) {
            return handleGetNames();
        } else if (parts[1].equals("VALUE") && parts.length >= 3) {
            String key = parts[2];
            return handleGetValue(key);
        }

        return "NA";
    }

    private String handleGetNames() {
        Set<String> allKeys = keyDiscovery.getAllKeys();

        if (allKeys.isEmpty()) {
            return "OK 0";
        }

        StringBuilder sb = new StringBuilder("OK ");
        sb.append(allKeys.size());

        for (String key : allKeys) {
            sb.append(" ").append(key);
        }

        return sb.toString();
    }


    private String handleGetValue(String key) {
        ServerInfo server = keyDiscovery.getServerForKey(key);

        if (server == null) {
            return "NA";
        }

        return serverForwarder.forwardToServer(server, "GET VALUE " + key);
    }


    private String handleSet(String[] parts) {
        if (parts.length < 3) {
            return "NA";
        }

        String key = parts[1];
        String value = parts[2];

        ServerInfo server = keyDiscovery.getServerForKey(key);

        if (server == null) {
            return "NA";
        }

        return serverForwarder.forwardToServer(server, "SET " + key + " " + value);
    }

    private void handleQuit() {
        System.out.println("\n=== KONIEC QUIT ===");

        for (ServerInfo server : servers) {
            serverForwarder.sendWithoutResponse(server, "QUIT");
        }

        System.out.println("Proxy kończy pracę...");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}