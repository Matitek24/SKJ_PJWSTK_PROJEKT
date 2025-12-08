package server;

import command.CommandProcessor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


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
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void handleClient() throws Exception {

        String command = new String(
                receivedPacket.getData(),
                0,
                receivedPacket.getLength()
        ).trim();

        InetAddress clientAddress = receivedPacket.getAddress();
        int clientPort = receivedPacket.getPort();

        System.out.println("      Komenda: " + command);

        String response = commandProcessor.processCommand(command);

        if (response != null && !response.isEmpty()) {
            System.out.println("      Odpowiedz: " + response);
            sendResponse(response, clientAddress, clientPort);
        }
    }


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