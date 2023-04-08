import java.util.HashMap;
import java.io.*;
import java.net.*;

public class Router {

    public static void main(String[] args) throws IOException {
        String otherRouterHostname = getOtherHostnameFromEnv();

        int announcementPort = 4444;
        DatagramSocket announcementRecvSocket = null; // UDP socket to collect startup announcement from each of my peers
        try {
            announcementRecvSocket = new DatagramSocket(announcementPort);
        } catch (IOException e) {
            System.err.println("Could not listen for announcements on port: "+announcementPort+"/udp.");
            System.exit(1);
        }
        byte[] buffer = new byte[1024];
        DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);

        int requestorPort = 5555;
        ServerSocket requestorSocket = null;
        try {
            requestorSocket = new ServerSocket(requestorPort);
        } catch (IOException e ) {
            System.err.println("Could not listen for requests on port: "+requestorPort+".");
            System.exit(1);
        }

        int counterpartyPort = 6666;
        ServerSocket counterpartySocket = null;
        try {
            counterpartySocket = new ServerSocket(counterpartyPort);
        } catch (IOException e ) {
            System.err.println("Could not listen for requests from counterparty: "+counterpartyPort+".");
            System.exit(1);
        }

        // Lookup map, key is a String for hostname, value is its IP
        //     note, routingMap will only contain peers owned by this router and not the counterparty Router
        HashMap<String, InetAddress> routingMap = new HashMap<String, InetAddress>();

        Socket incomingSocket = null;
        InterRouterThread irt = null;
        boolean isIRTOpen = false;
        boolean running = true;
        while (running == true) {
            try {
                announcementRecvSocket.receive(incomingPacket); // Receive incoming UDP announcement packet
                String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
                String[] parts = message.split("\\s+"); // Split the message by whitespace
                parts[0] = parts[0].trim();
                parts[1] = parts[1].trim();
                System.out.println(String.format("Peer %s announced its presence with IP: %s", parts[0], parts[1]));
                String proclaimerName = parts[0];
                InetAddress proclaimerIP = InetAddress.getByName(parts[1]);
                routingMap.put(proclaimerName, proclaimerIP); // add the name and ip to routingMap
            } catch (SocketTimeoutException e) {
                // Do nothing, just continue the loop
                assert(true);
            }

            if (!isIRTOpen) {
                try {
                    incomingSocket = counterpartySocket.accept(); // accept an incoming TCP connection from the other Router relaying a request from its Peer
                    irt = new InterRouterThread(routingMap, incomingSocket, otherRouterHostname);
                    irt.start();
                    isIRTOpen = true;
                    System.out.println("Router recieved request from counterparty Router: " + incomingSocket.getInetAddress().getHostAddress());
                } catch (IOException e) {
                    System.err.println("Counterparty Router failed to connect to this Router.");
                    System.exit(1);
                }
            }

            try {
                if (!isIRTOpen) { continue; } // wait to accept Peer connection until IRT is open
                incomingSocket = requestorSocket.accept(); // accept an incoming TCP connection from a requestor Peer
                RequestorThread r = new RequestorThread(routingMap, incomingSocket, irt);
                r.start();
                System.out.println("Router recieved request from Peer: " + incomingSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("Peer failed to connect to this Router.");
                System.exit(1);
            }

        }
        announcementRecvSocket.close();
        requestorSocket.close();
        counterpartySocket.close();
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
            System.err.println("COUNTERPARTY_HOSTNAME not in env. Exiting...");
            System.exit(1);
        }

        return otherRouterHostname;
    }
}
