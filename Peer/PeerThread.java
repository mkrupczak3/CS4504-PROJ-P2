import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PeerThread extends Thread
{
    private Socket outSocket;
    private PrintWriter out = null; // for writing to ServerRouter
    private BufferedReader in = null; // for reading form ServerRouter
    private AtomicInteger sharedInt;

    //Variables for data
    public static SynchronizedRollingAverage lookupAverage = new SynchronizedRollingAverage();
    public static SynchronizedRollingAverage messageSizeAverage = new SynchronizedRollingAverage();

    public PeerThread(Socket s, AtomicInteger aInt)
    {
        outSocket = s;
        sharedInt = aInt;
    }

    public void run()
    {
        sharedInt.getAndDecrement(); //tells parent class Peer to stop listening
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