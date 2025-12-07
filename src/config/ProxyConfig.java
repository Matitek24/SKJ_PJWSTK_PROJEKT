package config;

import model.ServerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa konfiguracyjna przechowująca parametry uruchomieniowe proxy
 */
public class ProxyConfig {
    private final int proxyPort;
    private final List<ServerInfo> servers;

    public ProxyConfig(int proxyPort, List<ServerInfo> servers) {
        this.proxyPort = proxyPort;
        this.servers = new ArrayList<>(servers);
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public List<ServerInfo> getServers() {
        return new ArrayList<>(servers);
    }

    /**
     * Parsuje argumenty linii komend i tworzy obiekt konfiguracji
     */
    public static ProxyConfig parseArguments(String[] args) throws IllegalArgumentException {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "Usage: java Proxy -port <port> -server <address> <port> ..."
            );
        }

        int proxyPort = -1;
        List<ServerInfo> servers = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-port")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing port number after -port");
                }
                try {
                    proxyPort = Integer.parseInt(args[++i]);
                    if (proxyPort < 1 || proxyPort > 65535) {
                        throw new IllegalArgumentException("Port must be between 1 and 65535");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number: " + args[i]);
                }

            } else if (args[i].equals("-server")) {
                if (i + 2 >= args.length) {
                    throw new IllegalArgumentException("Missing address or port after -server");
                }
                String address = args[++i];
                try {
                    int port = Integer.parseInt(args[++i]);
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException("Server port must be between 1 and 65535");
                    }
                    servers.add(new ServerInfo(address, port));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid server port: " + args[i]);
                }
            }
        }

        if (proxyPort == -1) {
            throw new IllegalArgumentException("Missing -port parameter");
        }

        if (servers.isEmpty()) {
            throw new IllegalArgumentException("At least one -server parameter is required");
        }

        return new ProxyConfig(proxyPort, servers);
    }

    @Override
    public String toString() {
        return String.format("ProxyConfig{port=%d, servers=%d}", proxyPort, servers.size());
    }
}