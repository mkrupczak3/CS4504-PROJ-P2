import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Router {

    static InterRouterThread irt;
    static AtomicBoolean isIRTOpen = new AtomicBoolean(false);

    public static void main(String[] args) throws IOException {
        String otherRouterHostname = getOtherHostnameFromEnv(); // System.getenv("COUNTERPARTY_HOSTNAME");

        // Create a ThreadPoolExecutor with 3 fixed threads
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        // Create a queue for storing results from child threads
        BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();

        int announcementPort = 4444;
        final DatagramSocket announcementRecvSocket;
        DatagramSocket tempDS = null;
        try {
            tempDS = new DatagramSocket(announcementPort);
        } catch (IOException e) {
            System.err.println("Could not listen for announcements on port: "+announcementPort+"/udp.");
            System.exit(1);
        }
        announcementRecvSocket = tempDS;

        byte[] buffer = new byte[1024];
        DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);

        int requestorPort = 5555;
        final ServerSocket requestorSocket;
        ServerSocket tempRS = null;
        try {
            tempRS = new ServerSocket(requestorPort);
        } catch (IOException e ) {
            System.err.println("Could not listen for requests on port: "+requestorPort+".");
            System.exit(1);
        }
        requestorSocket = tempRS;

        int counterpartyPort = 6666;
        final ServerSocket counterpartySocket;
        ServerSocket tempCS = null;
        try {
            tempCS = new ServerSocket(counterpartyPort); // listening port
        } catch (IOException e ) {
            System.err.println("Could not listen for requests from counterparty: "+counterpartyPort+".");
            System.exit(1);
        }
        counterpartySocket = tempCS;

        // Lookup map, key is a String for hostname, value is its IP
        //     note, routingMap will only contain peers owned by this router and not the counterparty Router
        ConcurrentHashMap<String, InetAddress> routingMap = new ConcurrentHashMap<String, InetAddress>();

        // final InterRouterThread irt;
        // final AtomicBoolean isIRTOpen = new AtomicBoolean(false);

        // Callable for handling announcement packets
        Callable<Void> announcementHandler = () -> {
            // ... (same as original code)
            while (true) {
                try {
                    announcementRecvSocket.receive(incomingPacket); // Receive incoming UDP announcement packet
                    String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
                    String[] parts = message.split("\\s+"); // Split the message by whitespace
                    parts[0] = parts[0].trim();
                    parts[1] = parts[1].trim();
                    String result = String.format("Peer %s announced its presence with IP: %s", parts[0], parts[1]);
                    String proclaimerName = parts[0];
                    InetAddress proclaimerIP = InetAddress.getByName(parts[1]);
                    routingMap.put(proclaimerName, proclaimerIP); // add the name and ip to routingMap
                    resultQueue.put(result);
                } catch (SocketTimeoutException e) {
                    // Do nothing upon no activity, just continue the loop
                    assert(true);
                }
            }
        };

        BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

        // Create a sendThread to initiate connection with the counterParty Router
        Thread sendThread = new Thread(() -> {
            try {
                Socket sendSocket = new Socket(otherRouterHostname, counterpartyPort);
                PrintWriter out = new PrintWriter(sendSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
                while (true) {
                    String messageLineOne = sendQueue.take();
                    out.println(messageLineOne);
                    String messageLineTwo = sendQueue.take();
                    out.println(messageLineTwo);

                    // Read the response and put it in the responseQueue
                    String responseLineOne = in.readLine();
                    if (responseLineOne != null) {
                        responseQueue.put(responseLineOne);
                    }

                    String responseLineTwo = in.readLine();
                    if (responseLineTwo != null) {
                        responseQueue.put(responseLineTwo);
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to initiate connection with counterParty Router.");
                e.printStackTrace();
            }
        });
        sendThread.start();

        // Callable for recieving inter-router requests
        Callable<Void> interRouterHandler = () -> {
            try {
                Socket incomingSocket = counterpartySocket.accept(); // accept an incoming TCP connection from the other Router relaying a request from its Peer
                irt = new InterRouterThread(routingMap, incomingSocket, sendQueue, responseQueue);
                irt.start();
                isIRTOpen.set(true);
                String result = "Router recieved incoming connection from counterparty Router: " + incomingSocket.getInetAddress().getHostAddress();
                resultQueue.put(result);
            } catch (IOException e) {
                System.err.println("Counterparty Router failed to connect to this Router.");
                isIRTOpen.set(false);
            }
            return null;
        };

        // Callable for handling requestor connections
        Callable<Void> requestorHandler = () -> {
            // ... (same as original code)
            while (true) {
                if (!isIRTOpen.get() || irt == null) { continue; } // wait to accept Peer connection until IRT is open
                try {
                    Socket incomingSocket = requestorSocket.accept();
                    RequestorThread r = new RequestorThread(routingMap, incomingSocket, irt);
                    r.start();
                    String result = "Router recieved request from Peer: " + incomingSocket.getInetAddress().getHostAddress();
                    resultQueue.put(result);
                } catch (IOException e) {
                    System.err.println("Peer failed to connect to this Router.");
                    continue;
                }
            }
        };

        // Submit the Callables to the executor
        executor.submit(announcementHandler);
        executor.submit(interRouterHandler);
        executor.submit(requestorHandler);

        try {
            while (true) {
                try {
                    String result = resultQueue.take();
                    System.out.println(result);
                } catch (InterruptedException e) {
                    assert(true);
                }
            }
        } finally {
            announcementRecvSocket.close();
            requestorSocket.close();
            counterpartySocket.close();
        }
    }

    public static String getOtherHostnameFromEnv() {
        String otherRouterHostname = null;
        try {
            otherRouterHostname = System.getenv("COUNTERPARTY_HOSTNAME"); // get the other Router's hostname from bash environment variable
        } catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }
        if (otherRouterHostname == null || otherRouterHostname.equals("")) {
            System.err.println("Counterparty Hostname was never provided. Exiting...");
            System.exit(1);
        }

        return otherRouterHostname;
    }
}
