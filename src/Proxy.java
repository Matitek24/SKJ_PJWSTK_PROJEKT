import command.CommandProcessor;
import config.ProxyConfig;
import discovery.KeyDiscovery;
import server.TCPListener;
import server.UDPListener;

public class Proxy {
    private final ProxyConfig config;
    private final KeyDiscovery keyDiscovery;
    private final CommandProcessor commandProcessor;
    private TCPListener tcpListener;
    private UDPListener udpListener;

    public Proxy(ProxyConfig config) {
        this.config = config;
        this.keyDiscovery = new KeyDiscovery();
        this.commandProcessor = new CommandProcessor(keyDiscovery, config.getServers());
    }


    public static void main(String[] args) {
        printBanner();

        try {

            ProxyConfig config = ProxyConfig.parseArguments(args);
            printConfiguration(config);

            Proxy proxy = new Proxy(config);
            proxy.start();

        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.err.println();
            printUsage();
            System.exit(1);

        } catch (Exception e) {
            System.err.println("FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Wyświetla banner startowy
     */
    private static void printBanner() {
        System.out.println(" <<<<<<  PROXY SERVER - Etap 2 (TCP + UDP)  >>>>>>>>>>>>  ");
        System.out.println();
    }

    private static void printConfiguration(ProxyConfig config) {
        System.out.println("Konfiguracja:");
        System.out.println("  Proxy port: " + config.getProxyPort() + " (TCP + UDP)");
        System.out.println("  Servers: " + config.getServers().size());
        for (int i = 0; i < config.getServers().size(); i++) {
            System.out.println("    [" + (i + 1) + "] " +
                    config.getServers().get(i).getAddress() + ":" +
                    config.getServers().get(i).getPort());
        }
        System.out.println();
    }

    private static void printUsage() {
        System.err.println("Błąd, Uzywaj tak: java Proxy -port <port> -server <address> <port> ...");
    }

    public void start() {

        System.out.println("║          STARTING PROXY            ║");
        System.out.println();

        keyDiscovery.discoverKeys(config.getServers());

        System.out.println("Startujemy TCP na porcie " + config.getProxyPort() + "...");
        tcpListener = new TCPListener(config.getProxyPort(), commandProcessor);
        Thread tcpThread = new Thread(tcpListener, "TCP-Listener");
        tcpThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Startujemy nasłuchiwac UDP na porcie " + config.getProxyPort() + "...");
        udpListener = new UDPListener(config.getProxyPort(), commandProcessor);
        Thread udpThread = new Thread(udpListener, "UDP-Listener");
        udpThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("      PROXY GOTOWE DO PRACY!         ");
        System.out.println();
        System.out.println(" Gotowy na uzyskanie " + keyDiscovery.getKeyCount() + " kluczy");
        System.out.println();

        addShutdownHook();
    }


    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n===  KONIEC ===");

            if (tcpListener != null) {
                System.out.println("Stop TCP listener...");
                tcpListener.stop();
            }

            if (udpListener != null) {
                System.out.println("Stop UDP listener...");
                udpListener.stop();
            }

            System.out.println("Proxy Koniec.");
        }));
    }
}