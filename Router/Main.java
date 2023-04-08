import java.util.HashMap;
import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        int requesterListenPort = Integer.valueOf(getRequiredEnv("CLIENT_LISTEN_PORT", "5555"));
        int counterpartyListenPort = Integer.valueOf(getRequiredEnv("COUNTERPARTY_LISTEN_PORT", "6666"));
        String counterpartyHost = getRequiredEnv("COUNTERPARTY_HOST");
        int counterpartyPort = Integer.valueOf(getRequiredEnv("COUNTERPARTY_PORT", "6666"));

        Router router = new Router(requesterListenPort, counterpartyListenPort,
                                   counterpartyHost, counterpartyPort);

        try {
            router.run();
        } catch (InterruptedException e) {
            System.err.println("Caught interruption, exiting."); 
        }

        router.close();
    }

    // will never return null or empty string
    public static String getRequiredEnv(String envvar) {
        return getRequiredEnv(envvar, null);
    }

    // will never return null or empty string
    public static String getRequiredEnv(String envvar, String default_value) {
        String ret = null;
        try {
            ret = System.getenv(envvar); 
        } catch (SecurityException se) {
            System.err.println("Process failed to obtain needed Env Variable due to security policy. Exiting...");
            System.exit(1);
        }

        if (ret == null || ret.equals("")) {
            if (default_value != null) {
                System.err.println(envvar + " not in env, using default " + default_value + ".");
                ret = default_value;
            } else {
                System.err.println("Required env variable " + envvar + " not found. Exiting...");
                System.exit(1);
            }
        }

        return ret;
    }
}
