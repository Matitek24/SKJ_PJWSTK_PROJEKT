package server;

import command.CommandProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Klasa nasłuchująca połączeń UDP od klientów
 */
public class UDPListener implements Runnable {
    private static final int MAX_PACKET_SIZE = 65535;

    private final int port;
    private final CommandProcessor commandProcessor;
    private final ExecutorService executorService;
    private DatagramSocket socket;
    private volatile boolean running;

    public UDPListener(int port, CommandProcessor commandProcessor) {
        this.port = port;
        this.commandProcessor = commandProcessor;
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }

    @Override
    public void run() {
        try {
            startListening();
        } catch (IOException e) {
            System.err.println("Error in UDP listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Rozpoczyna nasłuchiwanie na datagramy UDP
     */
    private void startListening() throws IOException {
        socket = new DatagramSocket(port);
        running = true;

        System.out.println("  ✓ UDP Listener ready");

        while (running) {
            try {
                // Przygotuj bufor na datagram
                byte[] buffer = new byte[MAX_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Odbierz datagram
                socket.receive(packet);

                System.out.println("[UDP] Client: " + packet.getAddress() + ":" + packet.getPort());

                // Obsłuż w osobnym wątku
                UDPClientHandler handler = new UDPClientHandler(
                        packet,
                        socket,
                        commandProcessor
                );
                executorService.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving UDP packet: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Zatrzymuje nasłuchiwanie
     */
    public void stop() {
        running = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        executorService.shutdown();
    }
}