import java.io.*;
import java.net.*;
import java.util.HashMap;

public class RequestorThread extends Thread {
    private final HashMap<String, InetAddress> routingMap;
    private final InterRouterThread irt;
    private final PrintWriter requestorReturnLine;
    private final BufferedReader in;
    private String inputLine, outputLine, requestor, target, addr;
    // public static SynchronizedRollingAverage lookupAverage = new SynchronizedRollingAverage();
    // public static SynchronizedRollingAverage messageSizeAverage = new SynchronizedRollingAverage();

    RequestorThread(HashMap<String, InetAddress> routingMap, Socket incomingSocket, InterRouterThread irt) throws IOException {
        requestorReturnLine = new PrintWriter(incomingSocket.getOutputStream(), true); // A way to send result back to the requestor
        in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream())); // A way to recieve request from the requestor
        this.routingMap = routingMap; // this will only be used in the special case that our peer is already on the same router
        this.irt = irt;
        addr = incomingSocket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            requestor = in.readLine();
            target = in.readLine();
            System.out.println(String.format("Requestor %s is asking for target %s", requestor, target));
            InetAddress lookup = routingMap.get(target);
            boolean isTargetOnSameRouter = (lookup != null);
            if (isTargetOnSameRouter) {
                requestorReturnLine.println(lookup.getHostAddress());
            } else {
                irt.registerRequestor(addr, requestorReturnLine); // Register the requestor and its response writer
                irt.sendRequest(requestor, target);
                in.close();
                requestorReturnLine.close();
            }

        } catch (IOException e) {
            System.err.println("RequestorThread not able to use incoming socket.");

        } finally {
            in.close();
            requestorReturnLine.close();
            System.exit(1);
        }
    }
}
