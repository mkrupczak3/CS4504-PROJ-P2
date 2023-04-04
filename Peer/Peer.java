import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer
{
    static AtomicInteger sharedInt; //closes running loop when value == 0


    public static void main(String[] args)
    {
        //Initialization with default values here
        DatagramPacket myAnnouncementPacket;
        String routerAddress = "172.23.0.6"; //to be filled in later
        int routerPortNum = 4444;
        String myAnnouncementString; //holds local addressing information
        byte[] bufferMessage;
        String targetName;
        boolean isClient = false; //will affect method later

        //adding parameters if present
        String temp = getRouterIPFromEnv();
        if(temp != null) //router address
        {
            routerAddress = temp;
        }
        temp = getTargetFromEnv();
        if(temp != null) //target node's device name
        {
            targetName = temp;
            isClient = true;
        }

        //Send local addressing data to the router
        try (DatagramSocket announceSendSocket = new DatagramSocket(routerPortNum)) //try with resources
        {
            //Setting up announcement string message
            myAnnouncementString = InetAddress.getLocalHost().getHostName(); //name
            myAnnouncementString += " " + InetAddress.getLocalHost().getHostAddress(); //IP address
            myAnnouncementString += " " + isClient; //isClient

            //sending packet over UDP socket
            bufferMessage = myAnnouncementString.getBytes();
            myAnnouncementPacket = new DatagramPacket(bufferMessage, bufferMessage.length, InetAddress.getByName(routerAddress), routerPortNum);
            announceSendSocket.send(myAnnouncementPacket);
        } catch (SocketException e) {
            System.err.println("Could not build datagram socket on port " + routerPortNum + "\n" + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Failed to find Local Address\n" + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to send packet\n" + e.getMessage());
        }

        //step 2. Either act as server or client
        int portNumber = 5556; //default value
        actAsServer(portNumber);
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

    /**
     * fetches the router IP address
     */
    private static String getRouterIPFromEnv()
    {
        String routerIP = null;
        try {
            routerIP = System.getenv("ROUTER_IPADDRESS"); // get peer's router IP Address from bash environment variable
        } catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }
        if (routerIP == null || routerIP.equals("")) {
            System.err.println("Router IP address was never provided. Exiting...");
            System.exit(1);
        }
        return routerIP;
    }

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
            System.err.println("target name was never provided. Exiting...");
            System.exit(1);
        }
        return target;
    }
}