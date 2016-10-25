package client;


import java.io.IOException;

/**
 * Client test class.
 */
public class ClientTest {
    public static void main(String[] args) {
        FTPClient client = new FTPClient("a");
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
