import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer
{
    static AtomicInteger sharedInt; //closes running loop when value == 0

    //Variables for data
    public static SynchronizedRollingAverage lookupAverage = new SynchronizedRollingAverage();
    public static SynchronizedRollingAverage messageSizeAverage = new SynchronizedRollingAverage();
    public static SynchronizedRollingAverage peerCycleTime = new SynchronizedRollingAverage();

    public static void main(String[] args)
    {
        //Initialization with default values here
        DatagramPacket myAnnouncementPacket;
        String routerName = null;
        int routerPortNum = 4444;
        String myAnnouncementString; //holds local addressing information
        byte[] bufferMessage;
        String targetName = null;
        boolean isClient = false; //will affect method later
        String fileName = null;

        //adding Environment Variables if present
        String temp;
        temp = getTargetFromEnv();
        if(temp != null && !temp.equals("none")) //target node's device name
        {
            targetName = temp;
            isClient = true;
            temp = null;
        }
        temp = getRouterHostNameFromEnv();
        if(temp != null)
        {
            routerName = temp;
            temp = null;
        }
        temp = getFileNameFromEnv();
        if(temp != null) //Name of file being sent to target peer
        {
            fileName = temp;
            temp = null;
        }

        //Send local addressing data to the router
        try (Socket sock = new Socket(routerName, routerPortNum)) {
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            out.println("ANNOUNCE");
            out.println(InetAddress.getLocalHost().getHostName());
            in.readLine(); // wait for ok
        } catch (SocketException e) {
            System.err.println("Could not build datagram socket on port " + routerPortNum + "\n" + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Failed to find Local Address\n" + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to send packet\n" + e.getMessage());
        }

        System.out.println("Sent announcment, waiting...");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            System.err.println("Interrupted, exiting.");
            System.exit(1);
        }

        //If this peer is a client, starts client thread. Then, executes server method
        int peerPortNumber = 5556; //port for peer to peer communication
        int routerRequestPort = 5555;
        if(isClient)
        {
            ClientThread client = new ClientThread(targetName, routerName, peerPortNumber, routerRequestPort, fileName);
            client.start();
        }
        actAsServer(peerPortNumber); //all peers act as a server
    }

    /**
     * Sets up a SocketServer and listens for other peer requests. Once a connection is made, sets up a peer thread
     */
    private static void actAsServer(int port)
    {
        ServerSocket listeningSocket = null;
        boolean isRunning = true;
        sharedInt.set(0);

        //setting up ServerSocket
        try
        {
            listeningSocket = new ServerSocket(port);
        }
        catch(IOException e)
        {
            System.err.println("Failed to set up ServerSocket");
            isRunning = false;
        }

        //accepts requests and runs until all child threads end their processes
        while(isRunning)
        {
            try(Socket tempSocket = listeningSocket.accept()) //try with resources
            {
                PeerThread temp = new PeerThread(tempSocket, sharedInt);
                temp.start();
                sharedInt.getAndIncrement();
            }
            catch(IOException e) {
                System.err.println("Failed to set serverSocket");
            }
            if(sharedInt.get() <= 0)
            {
                isRunning = false;
            }
        }

        //printing variable data
        System.out.println("Average lookup time for router: " + lookupAverage.getAverage() + "\n" +
                "Average message size: " + messageSizeAverage.getAverage() + "\n" +
                "Average cycle time for peer communication: " + peerCycleTime.getAverage());

        //Close server socket
        try{
            if(listeningSocket != null)
            {
                listeningSocket.close();
            }
        } catch(IOException e)
        {
            System.err.println("Failed to close Peer ServerSocket");
        }
    }

    private static String getRouterHostNameFromEnv()
    {
        String routerName = null;
        try {
            routerName = System.getenv("ROUTER_HOSTNAME"); // get peer's router IP Address from bash environment variable
        } catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }
        if (routerName == null || routerName.equals("")) {
            System.err.println("ROUTER_HOSTNAME not in env. Exiting...");
            System.exit(1);
        }
        return routerName;
    }

    /**
     *Fetches the target's name
     */
    private static String getTargetFromEnv()
    {
        String target = null;
        try {
            target = System.getenv("TARGET_NAME"); // get the client's target local name from bash environment variable
        } catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }
        if (target == null || target.equals("")) {
            System.err.println("TARGET_NAME not in env. Exiting...");
            System.exit(1);
        }
        return target;
    }


    private static String getFileNameFromEnv()
    {
        String file = null;
        try{
            file = System.getenv("FILE_NAME"); // get the name of the file being sent from bash enviorment variable
        }catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }
        if (file == null || file.equals("")) {
            System.err.println("FILE_NAME not in env. Exiting...");
            System.exit(1);
        }
        return file;
    }
    //SynchronizedRollingAverage copied from P1 of project
    static class SynchronizedRollingAverage {
        private double avg = 0;
        private long count = 0;

        public synchronized void addValue(double value) {
            avg = ((avg * count) + value) / (++count);
        }

        public synchronized double getAverage() {
            return avg;
        }
    }
}
