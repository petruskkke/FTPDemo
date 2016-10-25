package server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * The thread of client in the ftp server.
 *
 * @author Zhanghan Ke
 */
public class ClientThread extends Thread {
    private FTPServer server;
    private ServerSocket dtp = null;   //TODO need it?
    private Socket cli_socket;
    private Socket dtp_socket = null;
    private String name;
    private String addr;
    private String access_path;
    private int thread_id;
    private int handle_id;
    private Map<Integer, HandleThread> handle_threads;
    private boolean isrunning = true;
    private boolean pasvmode = false;

    /**
     * Init method.
     * @param server The sftp server
     * @param cli_socket The socket connect to the client
     * @param addr The addr of client
     * @param thread_id The id of client thread
     * @param name the client thread's name
     */
    public ClientThread(FTPServer server, Socket cli_socket, String addr, int thread_id, String name) {
        this.server = server;
        this.cli_socket = cli_socket;
        this.addr = addr;
        this.thread_id = thread_id;
        this.name = name;
        this.handle_threads = new HashMap<>();
        this.handle_id = 1;
        this.access_path = System.getProperty("user.dir") + "\\src\\server\\resources";
    }

    /**
     * Carry it while thread start.
     */
    public void run() {
        PrintWriter pw = null;
        BufferedReader br = null;

        try {
            pw = new PrintWriter(new OutputStreamWriter(cli_socket.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(cli_socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pw.write("220 Welcome to connect this ftp server.\r\n");
        pw.flush();
        try {
            isrunning = server.user_check(cli_socket, server);
            if (isrunning) {
                pw.write("\n[*] Connect success.\n[*] Enter the command.\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("[*] Using filepath: " + getAccess_path());

        // main loop
        while (isrunning) {
            String command = null;
            try {
                String req = br.readLine();
                System.out.println("\n[*] Request command: (" + req + ") from (client thread " + thread_id + ")");
                command = server.command_format(req);
            } catch (IOException e) {
                System.out.println("500 Command format error.");
                pw.write("500 Command format error.\r\n");
                pw.flush();
            }
            if (command.startsWith("500 ")) {
                pw.write(command + "\r\n");
            } else {
                if (command != null) {
                    HandleThread thread = new HandleThread(this, server, cli_socket, handle_id, command, getAccess_path(), "handle_thread");
                    thread.start();
                    handle_id += 1;
                    handle_threads.put(handle_id, thread);
                }
            }
        }
    }


    public String getAddr() {
        return addr;
    }

    public boolean getIsrunning() {
        return isrunning;
    }

    public Socket getDtp_socket() {
        return dtp_socket;
    }

    public Socket getCli_socket() {
        return cli_socket;
    }

    public boolean getPasvmode() {
        return pasvmode;
    }

    public String getAccess_path() {
        return access_path;
    }

    public int getThread_id() {
        return thread_id;
    }

    public Map<Integer, HandleThread> getHandle_threads() {
        return handle_threads;
    }

    public ServerSocket getDtp() {
        return dtp;
    }

    public void setPasvmode(boolean b) {
        pasvmode = b;
    }

    public void setIsrunning(boolean b) {
        isrunning = b;
    }

    public void setDtp(ServerSocket d) {
        dtp = d;
    }

    public void setAccess_path(String filepath) {
        access_path = filepath;     //fixme  Need judge if filepath is legal or not.
    }

    public void setDtp_socket(Socket datasocket) {
        dtp_socket = datasocket;
    }
}
