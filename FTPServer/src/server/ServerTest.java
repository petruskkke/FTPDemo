package server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Server test class.
 */
public class ServerTest {

    public static void main(String[] args) {
        String host = null;
        try {
            host = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        assert host != null;
        String[] mess = host.split("/");
        String ip = mess[1];
        FTPServer server = new FTPServer(ip, 8000, "ftpserver");
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
