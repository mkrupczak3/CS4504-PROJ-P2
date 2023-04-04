import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientThread extends Thread
{
    private Socket toPeer;
    private String peerName; //target peer
    private String routerName;
    private int peerPortNum;
    public ClientThread(String t, String routerN, int port)
    {
        peerName = t;
        peerPortNum = port;
    }

    public void run()
    {
        //Connect to router and find target peer's IP
        try(Socket toRouterSocket = new Socket(routerName, 5555))
        {

        }
        catch(UnknownHostException e){
            System.err.println("Could not find " + routerName + "\n" + e.getMessage());
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
        }
    }
}
