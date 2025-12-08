package server;

import command.CommandProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CommandProcessor commandProcessor;

    public ClientHandler(Socket clientSocket, CommandProcessor commandProcessor) {
        this.clientSocket = clientSocket;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try {
            handleClient();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }


    private void handleClient() throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
        );
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String command = in.readLine();

        if (command == null) {
            return;
        }

        System.out.println("      Komenda: " + command);


        String response = commandProcessor.processCommand(command);

        if (response != null && !response.isEmpty()) {
            System.out.println("      Odpowiedz: " + response);
            out.println(response);
        }
    }

    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            // Igniruje
        }
    }
}