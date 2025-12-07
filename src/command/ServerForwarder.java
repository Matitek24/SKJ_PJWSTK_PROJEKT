package command;

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

/**
 * Klasa odpowiedzialna za przekazywanie komend do serwerów (TCP i UDP)
 */
public class ServerForwarder {
    private static final int CONNECTION_TIMEOUT = 2000; // 2 sekundy
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    /**
     * Przekazuje komendę do serwera i zwraca odpowiedź
     * Automatycznie wybiera TCP lub UDP w zależności od typu serwera
     */
    public String forwardToServer(ServerInfo server, String command) {
        System.out.println("      → Forwarding to " + server);

        String response;
        if (server.getProtocol() == Protocol.TCP) {
            response = forwardTCP(server, command);
        } else {
            response = forwardUDP(server, command);
        }

        System.out.println("      ← Server response: " + response);
        return response;
    }

    /**
     * Przekazuje komendę do serwera TCP
     */
    private String forwardTCP(ServerInfo server, String command) {
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(
                    new InetSocketAddress(server.getAddress(), server.getPort()),
                    CONNECTION_TIMEOUT
            );

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Wyślij komendę
            out.println(command);

            // Odbierz odpowiedź
            String response = in.readLine();

            return response != null ? response : "NA";

        } catch (Exception e) {
            System.err.println("Error forwarding TCP to " + server + ": " + e.getMessage());
            return "NA";

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignoruj błędy zamykania
                }
            }
        }
    }

    /**
     * Przekazuje komendę do serwera UDP
     */
    private String forwardUDP(ServerInfo server, String command) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(CONNECTION_TIMEOUT);

            // Przygotuj dane do wysłania
            byte[] sendData = command.getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    address,
                    server.getPort()
            );

            // Wyślij
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

            return response;

        } catch (Exception e) {
            System.err.println("Error forwarding UDP to " + server + ": " + e.getMessage());
            return "NA";

        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Wysyła komendę do serwera bez oczekiwania na odpowiedź
     * (używane dla QUIT)
     */
    public void sendWithoutResponse(ServerInfo server, String command) {
        if (server.getProtocol() == Protocol.TCP) {
            sendTCPWithoutResponse(server, command);
        } else {
            sendUDPWithoutResponse(server, command);
        }
    }

    /**
     * Wysyła komendę TCP bez odpowiedzi
     */
    private void sendTCPWithoutResponse(ServerInfo server, String command) {
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(
                    new InetSocketAddress(server.getAddress(), server.getPort()),
                    CONNECTION_TIMEOUT
            );

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(command);

            System.out.println("Sent '" + command + "' to " + server);

        } catch (Exception e) {
            System.err.println("Error sending TCP to " + server + ": " + e.getMessage());

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignoruj błędy zamykania
                }
            }
        }
    }

    /**
     * Wysyła komendę UDP bez odpowiedzi
     */
    private void sendUDPWithoutResponse(ServerInfo server, String command) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();

            byte[] sendData = command.getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket packet = new DatagramPacket(
                    sendData,
                    sendData.length,
                    address,
                    server.getPort()
            );

            socket.send(packet);

            System.out.println("Sent '" + command + "' to " + server);

        } catch (Exception e) {
            System.err.println("Error sending UDP to " + server + ": " + e.getMessage());

        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}