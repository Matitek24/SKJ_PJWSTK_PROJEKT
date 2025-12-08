package command;

import model.Protocol;
import model.ServerInfo;
import java.io.*;
import java.net.*;

public class ServerForwarder {
    private static final int TIMEOUT = 2000;
    private static final int BUFFER_SIZE = 65535;

    // Główna metoda publiczna - czysta logika biznesowa
    public String forwardToServer(ServerInfo server, String command) {
        System.out.println("      → Forwarding to " + server);
        try {
            if (server.getProtocol() == Protocol.TCP) {
                return sendTCP(server, command, true);
            } else {
                return sendUDP(server, command, true);
            }
        } catch (IOException e) {
            System.err.println("Communication error with " + server + ": " + e.getMessage());
            return "NA";
        }
    }

    // Metoda dla QUIT (bez czekania na odpowiedź)
    public void sendWithoutResponse(ServerInfo server, String command) {
        try {
            if (server.getProtocol() == Protocol.TCP) {
                sendTCP(server, command, false);
            } else {
                sendUDP(server, command, false);
            }
        } catch (IOException ignored) {
            // Przy QUIT ignorujemy błędy, bo serwer i tak się zamyka
        }
    }

    // --- Metody pomocnicze (ukrywają brudną robotę z gniazdami) ---

    private String sendTCP(ServerInfo server, String command, boolean waitForResponse) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getAddress(), server.getPort()), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(command); // PrintWriter sam dodaje nową linię

            if (waitForResponse) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response = in.readLine();
                System.out.println("      ← Server response: " + response);
                return response != null ? response : "NA";
            }
            return null;
        }
    }

    private String sendUDP(ServerInfo server, String command, boolean waitForResponse) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            byte[] sendData = (command + "\n").getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, server.getPort());
            socket.send(sendPacket);

            if (waitForResponse) {
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("      ← Server response: " + response);
                return response;
            }
            return null;
        }
    }
}