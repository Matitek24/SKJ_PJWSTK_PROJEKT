package server;

import command.CommandProcessor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Klasa obsługująca żądanie od klienta UDP
 */
public class UDPClientHandler implements Runnable {
    private static final int MAX_PACKET_SIZE = 65535;

    private final DatagramPacket receivedPacket;
    private final DatagramSocket socket;
    private final CommandProcessor commandProcessor;

    public UDPClientHandler(DatagramPacket receivedPacket,
                            DatagramSocket socket,
                            CommandProcessor commandProcessor) {
        this.receivedPacket = receivedPacket;
        this.socket = socket;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try {
            handleClient();
        } catch (Exception e) {
            System.err.println("Error handling UDP client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obsługuje żądanie UDP od klienta
     */
    private void handleClient() throws Exception {
        // Pobierz dane z pakietu
        String command = new String(
                receivedPacket.getData(),
                0,
                receivedPacket.getLength()
        ).trim();

        InetAddress clientAddress = receivedPacket.getAddress();
        int clientPort = receivedPacket.getPort();

        System.out.println("      Command: " + command);

        // Przetwórz komendę
        String response = commandProcessor.processCommand(command);

        // Wyślij odpowiedź (jeśli jest)
        if (response != null && !response.isEmpty()) {
            System.out.println("      Response: " + response);
            sendResponse(response, clientAddress, clientPort);
        }
    }

    /**
     * Wysyła odpowiedź do klienta UDP
     */
    private void sendResponse(String response, InetAddress address, int port)
            throws Exception {
        byte[] responseData = response.getBytes();

        DatagramPacket responsePacket = new DatagramPacket(
                responseData,
                responseData.length,
                address,
                port
        );

        socket.send(responsePacket);
    }
}