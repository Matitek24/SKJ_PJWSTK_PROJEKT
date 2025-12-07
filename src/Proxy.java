import command.CommandProcessor;
import config.ProxyConfig;
import discovery.KeyDiscovery;
import server.TCPListener;
import server.UDPListener;

/**
 * Główna klasa aplikacji Proxy
 *
 * Uruchomienie:
 * java pl.pja.s28201.proxy.Proxy -port <port> -server <addr> <port> ...
 *
 * Przykład:
 * java pl.pja.s28201.proxy.Proxy -port 8000 -server localhost 7001 -server localhost 7002
 *
 * Obsługuje:
 * - Klientów TCP i UDP
 * - Serwery TCP i UDP
 * - Tłumaczenie między protokołami (np. klient TCP -> serwer UDP)
 */
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

    /**
     * Punkt wejścia aplikacji
     */
    public static void main(String[] args) {
        printBanner();

        try {
            // Parsuj argumenty
            ProxyConfig config = ProxyConfig.parseArguments(args);

            printConfiguration(config);

            // Utwórz i uruchom proxy
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
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   PROXY SERVER - Etap 2 (TCP + UDP)   ║");
        System.out.println("║           PJA SKJ 2025                 ║");
        System.out.println("║         MAX 400 PUNKTÓW                ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Wyświetla konfigurację
     */
    private static void printConfiguration(ProxyConfig config) {
        System.out.println("Configuration:");
        System.out.println("  Proxy port: " + config.getProxyPort() + " (TCP + UDP)");
        System.out.println("  Servers to connect: " + config.getServers().size());
        for (int i = 0; i < config.getServers().size(); i++) {
            System.out.println("    [" + (i + 1) + "] " +
                    config.getServers().get(i).getAddress() + ":" +
                    config.getServers().get(i).getPort());
        }
        System.out.println();
    }

    /**
     * Wyświetla instrukcję użycia
     */
    private static void printUsage() {
        System.err.println("Usage: java Proxy -port <port> -server <address> <port> ...");
        System.err.println();
        System.err.println("Example:");
        System.err.println("  java Proxy -port 8000 -server localhost 7001 -server localhost 7002");
        System.err.println();
        System.err.println("The proxy will:");
        System.err.println("  - Listen on both TCP and UDP on the specified port");
        System.err.println("  - Auto-detect protocol of each server (TCP or UDP)");
        System.err.println("  - Translate between protocols as needed");
    }

    /**
     * Uruchamia proxy server
     */
    public void start() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║          STARTING PROXY...             ║");
        System.out.println("╚════════════════════════════════════════╝");

        // Krok 1: Odkryj klucze z serwerów (wykryj protokoły)
        keyDiscovery.discoverKeys(config.getServers());

        // Krok 2: Uruchom TCP listener
        System.out.println("Starting TCP listener on port " + config.getProxyPort() + "...");
        tcpListener = new TCPListener(config.getProxyPort(), commandProcessor);
        Thread tcpThread = new Thread(tcpListener, "TCP-Listener");
        tcpThread.start();

        // Daj czas TCP na start
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Krok 3: Uruchom UDP listener
        System.out.println("Starting UDP listener on port " + config.getProxyPort() + "...");
        udpListener = new UDPListener(config.getProxyPort(), commandProcessor);
        Thread udpThread = new Thread(udpListener, "UDP-Listener");
        udpThread.start();

        // Daj czas UDP na start
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    PROXY FULLY OPERATIONAL! 🚀         ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("Accepting connections on:");
        System.out.println("  ✓ TCP port " + config.getProxyPort());
        System.out.println("  ✓ UDP port " + config.getProxyPort());
        System.out.println();
        System.out.println("Ready to serve " + keyDiscovery.getKeyCount() + " keys");
        System.out.println();

        // Dodaj shutdown hook
        addShutdownHook();
    }

    /**
     * Dodaje hook do czystego zamknięcia aplikacji
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n=== Shutdown signal received ===");

            if (tcpListener != null) {
                System.out.println("Stopping TCP listener...");
                tcpListener.stop();
            }

            if (udpListener != null) {
                System.out.println("Stopping UDP listener...");
                udpListener.stop();
            }

            System.out.println("Proxy stopped.");
        }));
    }
}