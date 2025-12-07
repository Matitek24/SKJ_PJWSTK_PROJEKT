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
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klasa odpowiedzialna za odkrywanie kluczy z serwerów TCP i UDP
 */
public class KeyDiscovery {
    private static final int CONNECTION_TIMEOUT = 2000; // 2 sekundy
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    private final Map<String, ServerInfo> keyToServer;
    private final Set<String> allKeys;

    public KeyDiscovery() {
        this.keyToServer = new ConcurrentHashMap<>();
        this.allKeys = ConcurrentHashMap.newKeySet();
    }

    /**
     * Odkrywa klucze ze wszystkich podanych serwerów
     */
    public void discoverKeys(List<ServerInfo> servers) {
        System.out.println("\n=== Discovering keys from servers ===");
        System.out.println("Checking " + servers.size() + " servers...");
        System.out.println();

        for (ServerInfo server : servers) {
            try {
                System.out.println("Checking server: " + server);

                // Najpierw wykryj protokół
                detectProtocol(server);

                // Potem odkryj klucze
                discoverKeysFromServer(server);

                System.out.println();

            } catch (Exception e) {
                System.err.println("Error discovering keys from " + server + ": " + e.getMessage());
                System.out.println();
            }
        }

        System.out.println("=== Discovery complete ===");
        System.out.println("Total keys discovered: " + allKeys.size());
        System.out.println("Keys: " + allKeys);
        System.out.println();
    }

    /**
     * Wykrywa protokół serwera (TCP lub UDP)
     */
    private void detectProtocol(ServerInfo server) {
        System.out.print("  Detecting protocol... ");

        // Najpierw spróbuj TCP
        if (tryTCP(server)) {
            server.setProtocol(Protocol.TCP);
            System.out.println("TCP detected");
        }
        // Jeśli TCP nie działa, spróbuj UDP
        else if (tryUDP(server)) {
            server.setProtocol(Protocol.UDP);
            System.out.println("UDP detected");
        }
        else {
            System.out.println("FAILED - no response, defaulting to TCP");
            server.setProtocol(Protocol.TCP);
        }
    }

    /**
     * Sprawdza czy serwer odpowiada przez TCP
     */
    private boolean tryTCP(ServerInfo server) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(
                    new InetSocketAddress(server.getAddress(), server.getPort()),
                    1000 // Krótszy timeout - 1 sekunda
            );

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out.println("GET NAMES");
            socket.setSoTimeout(1000);
            String response = in.readLine();

            return response != null && response.startsWith("OK");

        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignoruj
                }
            }
        }
    }

    /**
     * Sprawdza czy serwer odpowiada przez UDP
     */
    private boolean tryUDP(ServerInfo server) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1000); // 1 sekunda timeout

            // Wyślij GET NAMES
            byte[] sendData = "GET NAMES".getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    address,
                    server.getPort()
            );
            socket.send(sendPacket);

            // Odbierz odpowiedź
            byte[] receiveData = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(
                    receiveData,
                    receiveData.length
            );

            socket.receive(receivePacket);

            String response = new String(
                    receivePacket.getData(),
                    0,
                    receivePacket.getLength()
            ).trim();

            return response.startsWith("OK");

        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Odkrywa klucze z pojedynczego serwera (TCP lub UDP)
     */
    private void discoverKeysFromServer(ServerInfo server) throws Exception {
        if (server.getProtocol() == Protocol.TCP) {
            discoverKeysTCP(server);
        } else {
            discoverKeysUDP(server);
        }
    }

    /**
     * Odkrywa klucze z serwera TCP
     */
    private void discoverKeysTCP(ServerInfo server) throws Exception {
        Socket socket = new Socket();

        try {
            socket.connect(
                    new InetSocketAddress(server.getAddress(), server.getPort()),
                    CONNECTION_TIMEOUT
            );

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("GET NAMES");
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            String response = in.readLine();

            if (response != null) {
                System.out.println("  Response: " + response);
                parseKeysResponse(response, server);
            }

        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // Ignoruj błędy zamykania
            }
        }
    }

    /**
     * Odkrywa klucze z serwera UDP
     */
    private void discoverKeysUDP(ServerInfo server) throws Exception {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(CONNECTION_TIMEOUT);

            // Wyślij GET NAMES
            byte[] sendData = "GET NAMES".getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    address,
                    server.getPort()
            );
            socket.send(sendPacket);

            // Odbierz odpowiedź
            byte[] receiveData = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(
                    receiveData,
                    receiveData.length
            );

            socket.receive(receivePacket);

            String response = new String(
                    receivePacket.getData(),
                    0,
                    receivePacket.getLength()
            ).trim();

            System.out.println("  Response: " + response);
            parseKeysResponse(response, server);

        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Parsuje odpowiedź GET NAMES i zapisuje klucze
     * Format: OK <liczba> <klucz1> <klucz2> ...
     */
    private void parseKeysResponse(String response, ServerInfo server) {
        if (response == null || !response.startsWith("OK")) {
            return;
        }

        String[] parts = response.split("\\s+");

        if (parts.length < 2) {
            return;
        }

        try {
            int count = Integer.parseInt(parts[1]);

            for (int i = 2; i < 2 + count && i < parts.length; i++) {
                String key = parts[i];
                keyToServer.put(key, server);
                allKeys.add(key);
                System.out.println("  -> Key '" + key + "' is on " + server);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid key count in response: " + response);
        }
    }

    /**
     * Zwraca serwer przechowujący dany klucz
     */
    public ServerInfo getServerForKey(String key) {
        return keyToServer.get(key);
    }

    /**
     * Zwraca wszystkie odkryte klucze
     */
    public Set<String> getAllKeys() {
        return new HashSet<>(allKeys);
    }

    /**
     * Sprawdza czy dany klucz istnieje w sieci
     */
    public boolean hasKey(String key) {
        return allKeys.contains(key);
    }

    /**
     * Zwraca liczbę odkrytych kluczy
     */
    public int getKeyCount() {
        return allKeys.size();
    }
}