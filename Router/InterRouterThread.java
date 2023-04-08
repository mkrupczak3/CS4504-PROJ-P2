import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InterRouterThread extends Thread {

    private ConcurrentHashMap<String, InetAddress> routingMap;
    private Socket incomingSocket;

    private BlockingQueue<String> sendQueue;
    private BlockingQueue<String> responseQueue;

    // A map to store requestor IP addresses and corresponding PrintWriter objects for sending responses
    private ConcurrentHashMap<String, PrintWriter> requestorResponseMap;

    InterRouterThread(ConcurrentHashMap<String, InetAddress> routingMap, Socket incomingSocket, BlockingQueue<String> sendQueue, BlockingQueue<String> responseQueue) {
        this.routingMap = routingMap;
        this.incomingSocket = incomingSocket;
        this.sendQueue = sendQueue;
        this.responseQueue = responseQueue;

        this.requestorResponseMap = new ConcurrentHashMap<>();
    }

    public void sendRequest(String requestor, String target) {
        // Send request to the other Router
        try {
            sendQueue.put(requestor);
            sendQueue.put(target);
        } catch (InterruptedException e) {
            System.err.println("Failed to send request through sendThread.");
            e.printStackTrace();
        }
    }

    public void registerRequestor(String requestorIP, PrintWriter responseWriter) {
        requestorResponseMap.put(requestorIP, responseWriter);
    }

    @Override
    public void run() {
        Thread receiverThread = new Thread(() -> {
            String requestorIP, targetName, targetIP;
            try {
                while (true) {
                    // Read the requestor IP and target name from the other Router
                    requestorIP = responseQueue.take(); // take is a blocking function
                    targetName = responseQueue.take();

                    // Find the InetAddress object for the target name
                    InetAddress targetInetAddress = routingMap.get(targetName);
                    targetIP = targetInetAddress != null ? targetInetAddress.getHostAddress() : "404NOTFOUND";
                    // Find the PrintWriter object for the requestor and send the target IP
                    PrintWriter responseWriter = requestorResponseMap.get(requestorIP);
                    if (responseWriter != null) {
                        responseWriter.println(targetIP);
                        responseWriter.close();
                    }
                    requestorResponseMap.remove(requestorIP); // Remove the requestor from the map

                }
            } catch (InterruptedException e) {
                System.err.println("InterRouterThread failed to read from the other Router.");
            }
        });
        receiverThread.start();
    }
}
