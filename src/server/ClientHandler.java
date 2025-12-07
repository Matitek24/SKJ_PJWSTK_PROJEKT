package server;

import command.CommandProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Klasa obsługująca połączenie z pojedynczym klientem TCP
 */
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

    /**
     * Obsługuje komunikację z klientem
     */
    private void handleClient() throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
        );
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        // Odczytaj komendę od klienta
        String command = in.readLine();

        if (command == null) {
            return;
        }

        System.out.println("      Command: " + command);

        // Przetwórz komendę
        String response = commandProcessor.processCommand(command);

        // Wyślij odpowiedź (jeśli jest)
        if (response != null && !response.isEmpty()) {
            System.out.println("      Response: " + response);
            out.println(response);
        }
    }

    /**
     * Zamyka socket klienta
     */
    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            // Ignoruj błędy zamykania
        }
    }
}