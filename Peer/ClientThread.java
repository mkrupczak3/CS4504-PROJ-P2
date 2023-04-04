import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientThread extends Thread
{
    private String peerTargetName; //target peer
    private String routerName;
    private int peerPortNum;
    private int routerPortNum;
    public ClientThread(String t, String routerN, int peerPort, int routerPort)
    {
        peerTargetName = t;
        routerName = routerN;
        peerPortNum = peerPort;
        routerPortNum = routerPort;
    }

    public void run()
    {
        PrintWriter out;
        BufferedReader in;
        String targetIP;

        //Connect to router and find target peer's IP
        try(Socket toRouterSocket = new Socket(routerName, routerPortNum))
        {
            out = new PrintWriter(toRouterSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(toRouterSocket.getInputStream()));
            String toRouterMessage = InetAddress.getLocalHost().getHostName();
            out.println(toRouterMessage);
            out.println(peerTargetName);
            targetIP = in.readLine();
        }
        catch(UnknownHostException e){
            System.err.println("Could not find " + routerName + "\n" + e.getMessage());
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
        }

        //Connect to targetPeer
    }
}
