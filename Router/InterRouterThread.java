import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class InterRouterThread extends Thread {

    private ConcurrentHashMap<String, InetAddress> routingMap;
    private Socket incomingSocket;
    private String otherRouterHostname;
    private PrintWriter out;
    private BufferedReader in;

    // A map to store requestor IP addresses and corresponding PrintWriter objects for sending responses
    private ConcurrentHashMap<String, PrintWriter> requestorResponseMap;

    InterRouterThread(HashMap<String, InetAddress> routingMap, Socket incomingSocket, String otherRouterHostname) {
        this.routingMap = new ConcurrentHashMap<>(routingMap);
        this.incomingSocket = incomingSocket;
        this.otherRouterHostname = otherRouterHostname;
        this.requestorResponseMap = new ConcurrentHashMap<>();

        try {
            out = new PrintWriter(incomingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("InterRouterThread not able to use incoming socket.");
            System.exit(1);
        }
    }

    public void sendRequest(String requestor, String target) {
        // Send request to the other Router
        out.println(requestor);
        out.println(target);
    }

    public void registerRequestor(String requestorIP, PrintWriter responseWriter) {
        requestorResponseMap.put(requestorIP, responseWriter);
    }

    @Override
    public void run() {
        String requestorIP, targetName, targetIP;
        try {
            while (true) {
                // Read the requestor IP and target name from the other Router
                requestorIP = in.readLine();
                targetName = in.readLine();

                if (requestorIP != null && targetName != null) {
                    // Find the InetAddress object for the target name
                    InetAddress targetInetAddress = routingMap.get(targetName);
                    targetIP = targetInetAddress != null ? targetInetAddress.getHostAddress() : null;

                    // Find the PrintWriter object for the requestor and send the target IP
                    PrintWriter responseWriter = requestorResponseMap.get(requestorIP);
                    if (responseWriter != null) {
                        responseWriter.println(targetIP);
                        requestorResponseMap.remove(requestorIP); // Remove the requestor from the map after sending the response
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("InterRouterThread failed to read from the other Router.");
            System.exit(1);
        }
    }
}
