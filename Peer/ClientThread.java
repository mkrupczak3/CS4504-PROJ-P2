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
        long routerLookupTimeStart = System.nanoTime();

        //Connect to router and find target peer's IP
        try(Socket toRouterSocket = new Socket(routerName, routerPortNum))
        {
            out = new PrintWriter(toRouterSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(toRouterSocket.getInputStream()));
            String toRouterMessage = InetAddress.getLocalHost().getHostName();
            out.println("GETPEER");
            out.println(toRouterMessage);
            out.println(peerTargetName);
            targetIP = in.readLine();

            //disconnect from router
            out.close();
            in.close();
        }
        catch(UnknownHostException e){
            System.err.println("Could not find " + routerName + "\n" + e.getMessage());
            return;
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
            return;
        }

        //Ensuring that the target IP was received
        if(targetIP.contains("NOTFOUND"))
        {
            System.err.println("The target IP was not found.\nExiting Program.");
            return;
        }
        else {
            System.out.println("Target IP retrieved: " + targetIP);
            System.out.println("Connecting to target...");
        }

        //Router lookup data
        long routerLookupTimeEnd = System.nanoTime();
        long timeDifference = routerLookupTimeStart-routerLookupTimeEnd;
        Peer.lookupAverage.addValue(timeDifference);

        //Connect to targetPeer
        try{
            toPeerSocket = new Socket(targetIP, peerPortNum);
            out = new PrintWriter(toPeerSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(toPeerSocket.getInputStream()));
        }
        catch(UnknownHostException e){
            System.err.println("Could not find " + peerTargetName + "\n" + e.getMessage());
            return;
        }
        catch(IOException e){
            System.err.println("Failed to connect\n" + e.getMessage());
            return;
        }
        //send the fileName
        out.print(fileName);

        // converting file into bytes which can be sent to targetPeer
        File sentFile = new File(fileName);
        byte[] fileByteArray = new byte[(int) sentFile.length()];
        Peer.messageSizeAverage.addValue(fileByteArray.length);
        try {
            FileInputStream fileBytes = new FileInputStream(sentFile);
            fileBytes.read(fileByteArray);
        } catch (Exception e) {
            System.err.println("File was empty or could not be read");
        }
        String base64Payload = new String(Base64.getEncoder().encodeToString(fileByteArray));
        long peerCycleTimeStart = System.nanoTime();
        out.println(base64Payload); //sending base64 payload to target Peer

        //receiving reply
        try {
            String reply = in.readLine();
            System.out.print(peerTargetName + " responded: " + reply);

            System.out.print("Communication finished. Closing sockets...");
            out.close();
            in.close();
        } catch (IOException e) {
            System.err.print("Failed to get reply");
        }

        long peerCycleTimeEnd = System.nanoTime();
        long cycleTime = peerCycleTimeStart - peerCycleTimeEnd;
        Peer.peerCycleTime.addValue(cycleTime);
    }
}
