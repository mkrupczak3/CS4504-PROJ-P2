import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;


public class PeerThread extends Thread
{
    private Socket outSocket;
    private PrintWriter out = null; // for writing to ServerRouter
    private BufferedReader in = null; // for reading form ServerRouter
    private AtomicInteger sharedInt;


    public PeerThread(Socket s, AtomicInteger aInt)
    {
        outSocket = s;
        sharedInt = aInt;
    }

    public void run()
    {
        String fileName = "";
        //setting up PrintWriter and BufferedReader from Socket provided at Initiation
        try {
            out = new PrintWriter(outSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));

            //receiving fileName
            fileName = in.readLine();
        } catch (IOException e) {
            System.err.println("Failed to create Writer/Reader\n" + e.getMessage());
            sharedInt.getAndDecrement();
            return;
        }
        System.out.println("Connection is successful. Received file name: " + fileName);
        String returnMessage  = "Successfully received and decoded file";

        //receiving the message from ClientThread and converting message from base64 text to original bytes
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            String base64Payload = in.readLine();
            byte[] fileByteArray = Base64.getDecoder().decode(base64Payload);
            fos.write(fileByteArray);

        } catch (IOException e) {
            System.err.println("IO error occurred when trying to dump to file: " + e.getMessage());
            returnMessage = "Failed to decode file";
        }

        //Closing statements
        out.print(returnMessage);
        System.out.print("Communication finished. Closing sockets...");
        try
        {
            out.close();
            in.close();
        }
        catch(IOException e)
        {
            System.err.println("Failed  to close Reader/Writer\n" + e.getMessage());
        }
        sharedInt.getAndDecrement(); //tells parent class Peer to stop listening
    }
}