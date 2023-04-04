import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer
{
    static AtomicInteger sharedInt; //closes running loop when value == 0

     /**
     * @param args
     * 1-Router IP address,
     * 2-port number of router,
     * 3-isClient boolean,
     * 4-port number of peer
     */
    public static void main(String[] args)
    {
        //Initialization with default values here
        DatagramPacket myAnnouncementPacket;
        String routerAddress = "172.23.0.6"; //to be filled in later
        int routerPortNum = 4444;
        String myAnnouncementString; //holds local addressing information
        byte[] bufferMessage;
        boolean isClient = true; //will affect method later

        //adding parameters if present
        if(args.length > 0) //router address
        {
            routerAddress = args[0];
        }
        if(args.length > 1)//port number of router
        {
            String num = args[1];
            num = num.trim();
            routerPortNum = Integer.parseInt(num);
        }
        if(args.length > 2) //boolean isClient. determines if this peer acts as a client or server
        {
            isClient = "True".equalsIgnoreCase(args[2]);
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
}