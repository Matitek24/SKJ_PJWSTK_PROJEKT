package server;

import command.CommandProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Klasa nasłuchująca połączeń TCP od klientów
 */
public class TCPListener implements Runnable {
    private final int port;
    private final CommandProcessor commandProcessor;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public TCPListener(int port, CommandProcessor commandProcessor) {
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
            System.err.println("Error in TCP listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Rozpoczyna nasłuchiwanie na połączenia TCP
     */
    private void startListening() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("  ✓ TCP Listener ready");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[TCP] Client: " + clientSocket.getRemoteSocketAddress());

                // Obsłuż klienta w osobnym wątku
                ClientHandler handler = new ClientHandler(clientSocket, commandProcessor);
                executorService.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Zatrzymuje nasłuchiwanie
     */
    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        executorService.shutdown();
    }
}