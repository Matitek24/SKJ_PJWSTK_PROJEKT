package discovery;

import model.Protocol;
import model.ServerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klasa odpowiedzialna za odkrywanie kluczy z serwerów TCP i UDP
 */
public class KeyDiscovery {
    private static final int CONNECTION_TIMEOUT = 1000; // Krótki timeout dla discovery
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    private final Map<String, ServerInfo> keyToServer;
    private final Set<String> allKeys;

    public KeyDiscovery() {
        this.keyToServer = new ConcurrentHashMap<>();
        this.allKeys = ConcurrentHashMap.newKeySet();
    }

    public void discoverKeys(List<ServerInfo> servers) {
        System.out.println("\n=== Odkrywanie kluczy ===");
        System.out.println("Sprawdza " + servers.size() + " serwerów...");
        System.out.println();

        for (ServerInfo server : servers) {
            try {
                System.out.println("Sprawdza server: " + server);
                detectProtocol(server);
                discoverKeysFromServer(server);
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error z " + server + ": " + e.getMessage());
            }
        }

        System.out.println("=== Zakończone ===");
        System.out.println("Ilosc kluczy które znalazłem: " + allKeys.size());
        System.out.println("Klucze: " + allKeys);
        System.out.println();
    }

    private void detectProtocol(ServerInfo server) {
        System.out.print("  Sprawdzanie protokołów... ");

        if (tryTCP(server)) {
            server.setProtocol(Protocol.TCP);
            System.out.println("TCP Odkryty");
        } else if (tryUDP(server)) {
            server.setProtocol(Protocol.UDP);
            System.out.println("UDP Odkryty");
        } else {
            System.out.println("FAILED");
            server.setProtocol(Protocol.TCP);
        }
    }

    private boolean tryTCP(ServerInfo server) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getAddress(), server.getPort()), 500);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("GET NAMES");
            socket.setSoTimeout(500);
            String response = in.readLine();
            return response != null && response.startsWith("OK");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryUDP(ServerInfo server) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);

            byte[] sendData = "GET NAMES\n".getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, server.getPort());
            socket.send(sendPacket);

            byte[] receiveData = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            return response.startsWith("OK");
        } catch (Exception e) {
            return false;
        }
    }

    private void discoverKeysFromServer(ServerInfo server) throws Exception {
        if (server.getProtocol() == Protocol.TCP) {
            discoverKeysTCP(server);
        } else {
            discoverKeysUDP(server);
        }
    }

    private void discoverKeysTCP(ServerInfo server) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getAddress(), server.getPort()), CONNECTION_TIMEOUT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("GET NAMES");
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            String response = in.readLine();
            if (response != null) {
                System.out.println("  Response: " + response);
                parseKeysResponse(response, server);
            }
        }
    }

    private void discoverKeysUDP(ServerInfo server) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);

            byte[] sendData = "GET NAMES\n".getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, server.getPort());
            socket.send(sendPacket);

            byte[] receiveData = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            System.out.println("  Odpowiedz: " + response);
            parseKeysResponse(response, server);
        }
    }

    private void parseKeysResponse(String response, ServerInfo server) {
        if (response == null || !response.startsWith("OK")) return;
        String[] parts = response.split("\\s+");
        if (parts.length < 2) return;

        try {
            int count = Integer.parseInt(parts[1]);
            for (int i = 2; i < 2 + count && i < parts.length; i++) {
                String key = parts[i];
                keyToServer.put(key, server);
                allKeys.add(key);
                System.out.println("  -> Klucz '" + key + "' na sewerze: " + server);
            }
        } catch (NumberFormatException e) {
            System.err.println("Zła ilosc kluczy: " + response);
        }
    }

    public ServerInfo getServerForKey(String key) { return keyToServer.get(key); }
    public Set<String> getAllKeys() { return new HashSet<>(allKeys); }
    public boolean hasKey(String key) { return allKeys.contains(key); }
    public int getKeyCount() { return allKeys.size(); }
}