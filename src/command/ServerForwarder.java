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

public class ServerForwarder {
    private static final int CONNECTION_TIMEOUT = 2000;
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    public String forwardToServer(ServerInfo server, String command) {
        System.out.println("      → Forwarding to " + server);
        if (server.getProtocol() == Protocol.TCP) {
            return forwardTCP(server, command);
        } else {
            return forwardUDP(server, command);
        }
    }

    private String forwardTCP(ServerInfo server, String command) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getAddress(), server.getPort()), CONNECTION_TIMEOUT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(command);
            String response = in.readLine();
            System.out.println("      ← Server response: " + response);
            return response != null ? response : "NA";
        } catch (Exception e) {
            System.err.println("Error TCP: " + e.getMessage());
            return "NA";
        }
    }

    private String forwardUDP(ServerInfo server, String command) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);

            // WAŻNE: Dodajemy spację/enter na końcu, bo Scanner na serwerze tego wymaga
            byte[] sendData = (command + "\n").getBytes();
            InetAddress address = InetAddress.getByName(server.getAddress());

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, server.getPort());
            socket.send(sendPacket);

            byte[] receiveData = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            System.out.println("      ← Server response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("Error UDP: " + e.getMessage());
            return "NA";
        }
    }

    public void sendWithoutResponse(ServerInfo server, String command) {
        if (server.getProtocol() == Protocol.TCP) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(server.getAddress(), server.getPort()), CONNECTION_TIMEOUT);
                new PrintWriter(socket.getOutputStream(), true).println(command);
            } catch (Exception ignored) {}
        } else {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] sendData = (command + "\n").getBytes(); // Tu też dodajemy \n
                InetAddress address = InetAddress.getByName(server.getAddress());
                socket.send(new DatagramPacket(sendData, sendData.length, address, server.getPort()));
            } catch (Exception ignored) {}
        }
    }
}