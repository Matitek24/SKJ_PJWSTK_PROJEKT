package server;

import command.CommandProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
            System.err.println("Error w TCP Listener: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void startListening() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println(" + TCP Listener ready");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[TCP] Client: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, commandProcessor);
                executorService.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }


    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error zamknięty socket: " + e.getMessage());
        }

        executorService.shutdown();
    }
}