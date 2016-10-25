package server;


import java.io.*;
import java.net.Socket;

/**
 * The thread of client thread in order to handle the client request from ftp client.
 *
 * @author Zhanghan Ke
 */
public class HandleThread extends Thread {

    private FTPServer server;

    private ClientThread thread;

    private Socket cli_socket;

    private int handle_id;

    private String command;

    private String filepath;

    private String name;

    /**
     * Init method.
     *
     * @param thread The client thread
     * @param server The ftp server
     * @param cli_socket The socket connect to the client
     * @param handle_id The thread id
     * @param command The command from client
     * @param filepath The filepath want to use
     * @param name The thread's name
     */
    public HandleThread(ClientThread thread, FTPServer server, Socket cli_socket, int handle_id, String command, String filepath, String name) {
        this.server = server;
        this.thread = thread;
        this.cli_socket = cli_socket;
        this.handle_id = handle_id;
        this.command = command;
        this.filepath = filepath;
        this.name = name;
    }

    /**
     * Carry this func while thread start.
     */
    public void run() {
        PrintWriter pw = null;
        BufferedReader br;
        try {
            pw = new PrintWriter(new OutputStreamWriter(cli_socket.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(cli_socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (command.equals("QUIT")) {
            try {
                server.cmdQuit(server, thread, pw);
                thread.setIsrunning(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (command.startsWith("CWD")) {
            server.cmdCwd(server, thread, command, pw);
        }
        if (command.equals("PASV")) {
            try {
                server.cmdPasv(thread, pw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (command.equals("LIST")) {
            if (thread.getPasvmode()) {
                try {
                    server.cmdList(server, thread, pw);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                pw.write("550 cant open file\r\n"); //Òª¸Ä
                pw.flush();
            }
        } else if (command.startsWith("LIST")) {
            pw.write("500 please use command: LIST \r\n");
            pw.flush();
        }
        if (command.startsWith("TYPE")) {
            pw.write("200 TYPE is now 8-bit binary.\r\n");
            pw.flush();
        }
        if (command.startsWith("RETR")) {
            try {
                server.cmdRetr(server, thread, command, pw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (command.startsWith("STOR")) {
            try {
                server.cmdStor(server, thread, command, pw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (command.startsWith("SIZE")) {
            server.cmdSize(server, thread, command, pw);

        }
        if (command.equals("PWD")) {
            server.cmdPwd(thread, pw);
        }
        thread.getHandle_threads().remove(handle_id);
    }
}
