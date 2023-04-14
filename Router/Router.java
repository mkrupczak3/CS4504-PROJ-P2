import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;


public class Router {

    private ConcurrentHashMap<String, InetAddress> myPeers_ = new ConcurrentHashMap<String, InetAddress>();

    private int requesterListenPort;
    private int counterpartyListenPort;
    private String counterpartyHost;
    private int counterpartyPort;

    private String dataFileName;

    private ServerSocket requesterListenSocket_;
    private ServerSocket counterpartyListenSocket_;
    private Socket counterpartySocket_;

    public Router(int requesterListenPort, int counterpartyListenPort,
                  String counterpartyHost, int counterpartyPort, String dataFileName) {
        this.requesterListenPort = requesterListenPort;
        this.counterpartyListenPort = counterpartyListenPort;
        this.counterpartyHost = counterpartyHost;
        this.counterpartyPort = counterpartyPort;
        this.dataFileName = dataFileName;

        createDataFile();
    }

    public synchronized void createDataFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dataFileName));
            writer.write("peer_name,lookup_time,message_size,peer_cycle_time\n");
            writer.close();
        } catch (IOException e) {
            System.err.println("Error opening data file! "+ e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void appendDataFile(String peerName, double lookupTime, double messageSize, double peerCycleTime) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dataFileName, true)); // append 
            writer.append(String.format("%s,%f,%f,%f\n", peerName, lookupTime, messageSize, peerCycleTime));
            writer.close();
        } catch (IOException e) {
            System.err.println("Error opening data file! "+e.getMessage());
            e.printStackTrace();
        }
    }

    // doesn't have to be synchronized because myPeers_ is a ConcurrentHashMap 
    public void addPeer(String name, InetAddress addr) {
        myPeers_.put(name, addr);
    }

    // doesn't have to be synchronized because myPeers_ is a ConcurrentHashMap 
    public InetAddress getLocalPeerIp(String key) {
        return myPeers_.get(key);
    }

    public synchronized InetAddress getCounterpartyPeerIp(String key) {
        try {
            PrintWriter out = new PrintWriter(counterpartySocket_.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(counterpartySocket_.getInputStream()));
            
            out.println(key);
            String response = in.readLine();
            if (response.equals("404NOTFOUND")) {
                return null;
            }

            return InetAddress.getByName(response);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
            close();
            System.exit(1);
        }

        return null; // this shouldn't be reached
    }

    public InetAddress getPeerIp(String key) {
        InetAddress addr = getLocalPeerIp(key);
        if (addr != null) {
            return addr;
        }

        // couldn't find peer in our list, try counterparty
        addr = getCounterpartyPeerIp(key);
        return addr; // may be null
    }


    public void run() throws InterruptedException {
        try {
            requesterListenSocket_ = new ServerSocket(requesterListenPort);
        } catch (IOException e) {
            System.err.println("Could not listen for requesters on port "+requesterListenPort+".");
            close();
            System.exit(1);
        }

        try {
            counterpartyListenSocket_ = new ServerSocket(counterpartyListenPort);
        } catch (IOException e ) {
            System.err.println("Could not listen for counterparty on port: "+counterpartyListenPort+".");
            close();
            System.exit(1);
        }

        Thread counterpartyListenThread = new Thread() {
            public void run() {
                try (Socket s = counterpartyListenSocket_.accept()) {
                    String hostAddr = s.getInetAddress().getHostAddress();
                    System.out.println(String.format("Got connection from counterparty %s.", hostAddr));

                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(s.getInputStream()));

                    while (true) {
                        String request = in.readLine();
                        if (request == null) {
                            System.out.println("Counterparty closed connection, exiting.");
                            close();
                            System.exit(0);
                        };
                        System.out.println("Request from counterparty: " + request);

                        InetAddress addr = getLocalPeerIp(request);
                        String response = addr == null ? "404NOTFOUND" : addr.getHostAddress();
                        System.out.println("Response: " + response);
                        out.println(response);
                    }
                } catch (IOException e) {
                    System.err.println("Connection error with counterparty: " + e.getMessage());
                    e.printStackTrace();
                    close();
                    System.exit(1);
                }
            }
        };

        Thread requesterListenThread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Socket s = requesterListenSocket_.accept();
                        RequestorThread t = new RequestorThread(s, Router.this);
                        t.start();
                    } catch (IOException e) {
                        System.err.println("Connection error with requester: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        };

        counterpartyListenThread.start();
        System.out.println(String.format("Attempting to connect to counterparty at %s:%s.",
                                         counterpartyHost, counterpartyPort));
        int counter = 0;
        while (true) {
            try {
                counterpartySocket_ = new Socket(counterpartyHost, counterpartyPort);

                System.out.println(String.format("Connection to counterparty %s:%s succeeded!",
                                                 counterpartyHost, counterpartyPort));
                break;
            } catch (Exception e) {
                counter++;
                System.out.println(String.format("Connection to counterparty %s:%s failed, trying again... (%d)",
                                                 counterpartyHost, counterpartyPort, counter));
                Thread.sleep(1000);
            }
        }

        requesterListenThread.start();

        counterpartyListenThread.join();
        requesterListenThread.join();
    }

    public void close() {
        if (requesterListenSocket_ != null) {
            try {
                requesterListenSocket_.close();
            } catch (IOException e) {
                System.err.println("Error closing requesterListenSocket_: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (counterpartyListenSocket_ != null) {
            try {
                counterpartyListenSocket_.close();
            } catch (IOException e) {
                System.err.println("Error closing counterpartyListenSocket_: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (counterpartySocket_ != null) {
            try {
                counterpartySocket_.close();
            } catch (IOException e) {
                System.err.println("Error closing counterpartySocket_: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
