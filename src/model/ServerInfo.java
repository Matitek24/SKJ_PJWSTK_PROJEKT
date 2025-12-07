package model;

import java.util.Objects;


public class ServerInfo {
    private final String address;
    private final int port;
    private Protocol protocol;

    public ServerInfo(String address, int port) {
        this.address = address;
        this.port = port;
        this.protocol = Protocol.TCP;
    }

    public ServerInfo(String address, int port, Protocol protocol) {
        this.address = address;
        this.port = port;
        this.protocol = protocol;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return String.format("%s:%d (%s)", address, port, protocol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInfo that = (ServerInfo) o;
        return port == that.port &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}