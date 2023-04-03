import java.io.*;
import java.net.*;
import java.util.HashMap;

public class RequestorThread extends Thread {
    private HashMap<String, InetAddress> RoutingMap;
    private InterRouterThread irt;

    private PrintWriter counterpartyRequestLine, requestorReturnLine;
    private BufferedReader in; // reader (for reading from the requestor)
    private String inputLine, outputLine, requestor, target, addr; // communication strings
    private Socket outSocket; // socket for communicating with the other ServerRouter

    // public static SynchronizedRollingAverage lookupAverage = new SynchronizedRollingAverage();
    // public static SynchronizedRollingAverage messageSizeAverage = new SynchronizedRollingAverage();

    RequestorThread(HashMap<String, InetAddress> RoutingMap, Socket incomingSocket, InterRouterThread irt) {
        requestorReturnLine = new PrintWriter(incomingSocket.getOutputStream(), true); // A way to send result back to the requestor
        in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream())); // A way to recieve request from the requestor
        this.RoutingMap = RoutingMap; // this will only be used in the special case that our peer is already on the same router
        this.irt = irt;
        addr = incomingSocket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            requestor = in.readLine();
            target = in.readLine();
            System.out.println(String.format("Requestor %s is asking for target %s", requestor, target));
            InetAddress lookup = RoutingMap.get(target);
            boolean isTargetOnSameRouter = (lookup != null);
            if (isTargetOnSameRouter) {
                requestorReturnLine.println(lookup.getHostAddress());
            } else {
                irt.sendRequest(requestor, target);
                // TODO recv result from irt here (may be pretty tricky)
            }

        } catch (IOException e) {
            System.err.println("RequestorThread not able to use incoming socket.");
            System.exit(1);
        }
    }
}
