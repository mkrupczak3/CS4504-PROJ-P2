import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;


public class ClientThread extends Thread
{
    private String peerTargetName; //target peer
    private String routerName;
    private String fileName;
    private int peerPortNum;
    private int routerPortNum; 
    public ClientThread(String t, String routerN, int peerPort, int routerPort, String file)
    {
        peerTargetName = t;
        routerName = routerN;
        peerPortNum = peerPort;
        routerPortNum = routerPort;
        fileName = file;
    }

    public void run()
    {
        PrintWriter out = null;
        BufferedReader in = null;
        String targetIP = null;
        Socket toPeerSocket;

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
            System.exit(1);
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
            System.exit(1);
        }
        
        //Connect to targetPeer
        try{
            toPeerSocket = new Socket(targetIP, peerPortNum);
            out = new PrintWriter(toPeerSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(toPeerSocket.getInputStream()));
        }
        catch(UnknownHostException e){
            System.err.println("Could not find " + peerTargetName + "\n" + e.getMessage());
            System.exit(1);
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
            System.exit(1);
        }

        // converting file into bytes which can be sent to targetPeer
        File sentFile = new File(fileName);
        byte[] fileByteArray = new byte[(int) sentFile.length()];
        try {
            FileInputStream fileBytes = new FileInputStream(sentFile);
            fileBytes.read(fileByteArray);
        } catch (Exception e) {
            System.err.println("File was empty or could not be read");
            System.exit(1);
        }
        String base64Payload = new String(Base64.getEncoder().encodeToString(fileByteArray));
        out.println(base64Payload); //sending base64 payload to target Peer
        




    }
}
