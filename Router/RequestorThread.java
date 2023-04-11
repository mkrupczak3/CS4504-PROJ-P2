import java.io.*;
import java.net.*;
import java.util.HashMap;

public class RequestorThread extends Thread {
    // public static SynchronizedRollingAverage lookupAverage = new SynchronizedRollingAverage();
    // public static SynchronizedRollingAverage messageSizeAverage = new SynchronizedRollingAverage();
    private Socket incomingSocket_;
    private Router router_;

    public RequestorThread(Socket incomingSocket, Router router) throws IOException {
        super();

        incomingSocket_ = incomingSocket;
        router_ = router;
    }

    @Override
    public void run() {
        InetAddress addr = incomingSocket_.getInetAddress();
        System.out.println("Got connection from peer "+addr.getHostAddress()+".");

        try {
            PrintWriter out = new PrintWriter(incomingSocket_.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(incomingSocket_.getInputStream()));

            while (true) {
                String command = in.readLine();
                if (command == null) {
                    System.err.println(String.format("Peer %s closed connection.",
                                                     addr.getHostAddress()));
                    return;
                }

                if (command.equals("ANNOUNCE")) {
                    String requestor_name = in.readLine();
                    if (requestor_name == null) {
                        System.err.println(String.format("Peer %s closed connection.",
                                                         addr.getHostAddress()));
                        return;
                    }

                    System.out.println(String.format("Requestor %s announced.",
                                                     requestor_name));
                    router_.addPeer(requestor_name, addr);

                    String response = "ok";
                    System.out.println(String.format("Response to %s: %s", requestor_name, response)); 
                    out.println(response);
                } else if (command.equals("GETPEER")) {
                    String requestor_name = in.readLine();
                    if (requestor_name == null) {
                        System.err.println(String.format("Peer %s closed connection.",
                                addr.getHostAddress()));
                        return;
                    }
                    String target_name = in.readLine();
                    if (target_name == null) {
                        System.err.println(String.format("Peer %s closed connection.",
                                addr.getHostAddress()));
                        return;
                    }
                    System.out.println(String.format("Requestor %s is asking for target %s",
                            requestor_name, target_name));

                    InetAddress lookup = router_.getPeerIp(target_name);
                    String response = lookup == null ? "404NOTFOUND" : lookup.getHostAddress();
                    System.out.println(String.format("Response to %s: %s", requestor_name, response));
                    out.println(response);
                }else if (command.equals("ADDDATA"))    {
                    String requester_name = in.readLine();
                    double[] data = new double[3]; //0 = Router lookup time, 1 = Message size in bytes, 2 = peer cycle time
                    for(int i = 0; i < 3; i++)
                    {
                        String dataString = in.readLine();
                        try
                        {
                            data[i] = Double.parseDouble(dataString);
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Failed to parse data from Peer\n" + e.getMessage());
                        }
                    }
                    out.println("Parse Successful");
                } else {
                    System.err.println(String.format("Unexpected command from peer %s: %s",
                                                     addr.getHostAddress(), command));
                }
            }
        } catch (IOException e) {
            System.err.println("RequestorThread I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                incomingSocket_.close();
            } catch (IOException e) {
                System.out.println("Could not close socket for peer "+addr.getHostAddress()+".");
            }
        }
    }
}
