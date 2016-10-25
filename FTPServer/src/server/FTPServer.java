package server;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class could instance ftp server object.
 *
 * @author Zhanghan Ke
 */
public class FTPServer {
    private String host = "";
    private int port = 0;
    private String name = null;
    private ServerSocket ftpserver = null;
    private Map<Integer, ClientThread> client_threads = null; //线程映射
    private Map<String, String> user = null;
    private int thread_id = 1;
    private int client_count = 0;    //当前线程数量

    public FTPServer(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.client_threads = new HashMap<>();
        this.user = new HashMap<>();
        this.user.put("admin", "admin");
    }

    /**
     * Start the server.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        ftpserver = new ServerSocket(port);
        System.out.println("[*] FTPServer start,listening on: (" + host + ":" + port + ")");
        System.out.println("[*] Waiting for connect...");

        while (true) {
            Socket cli_socket = ftpserver.accept();
            String cli_addr = cli_socket.getInetAddress().toString();

            System.out.println("\n[*] Accepter connection from: " + cli_addr);
            ClientThread thread = new ClientThread(this, cli_socket, cli_addr, thread_id, "client_thread");
            thread.start();

            thread_id += 1;
            client_count += 1;
            client_threads.put(thread_id, thread);
            System.out.println("[*] Now has " + client_count + " connections.");
        }
    }

    /**
     * Tranfer the command from client, now not finish.
     *
     * @param req The request from client
     * @return  The server command has been format
     */
    public String command_format(String req) {
        if (req.equals("QUIT")) {
            return req;
        }
        if (req.startsWith("CWD")) {
            return req;
        }
        if (req.startsWith("PASV")) {
            return req;
        }
        if (req.equals("LIST")) {
            return req;
        }
        if (req.startsWith("TYPE")) {
            return req;
        }
        if (req.startsWith("RETR")) {
            return req;
        }
        if (req.startsWith("REST")) {
            return req;
        }
        if (req.startsWith("STOR")) {
            return req;
        }
        if (req.startsWith("SIZE")) {
            return req;
        }
        if (req.equals("PWD")) {
            return req;
        }
        return "500 No such command.";
    }

    /**
     * Check on user's login in.
     *
     * @param cli_socket The socket object connect by client
     * @param server This ftp server
     * @return Whether the user login in successfui
     * @throws IOException
     */
    public boolean user_check(Socket cli_socket, FTPServer server) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(cli_socket.getOutputStream()));
        BufferedReader br = new BufferedReader(new InputStreamReader(cli_socket.getInputStream()));
        Map usermap = server.getUser();
        boolean user_tag = true;
        boolean pass_tag = true;
        boolean r = false;
        while (user_tag) {
            String user = br.readLine();
            String[] username = user.split(" ");

            if (username.length == 2) {
                if (username[0].equals("USER") && !username[1].equals("")) {
                    for (Object keys : usermap.keySet()) {
                        String key = (String) keys;
                        if (username[1].equals(key)) {
                            pw.write("331 Legal user.\r\n");
                            pw.flush();
                            user_tag = false;
                            break;
                        } else {
                            pw.write("530 Error user.\r\n");
                            pw.flush();
                        }
                    }
                }
            } else {
                pw.write("500 Please use command: USER username.\r\n");
                pw.flush();
            }
        }

        while (pass_tag) {
            String pass = br.readLine();
            String[] password = pass.split(" ");
            if (password.length == 2 && password[0].equals("PASS")) {
                for (Object key : usermap.keySet()) {
                    String p = user.get(key);
                    if (password[1].equals(p)) {
                        pw.write("230 Legal password.\r\n");
                        pw.flush();
                        r = true;
                        pass_tag = false;
                        break;
                    } else {
                        pw.write("530 Error password.\r\n");
                        pw.flush();
                    }
                }
            } else {
                pw.write("530 Please use command: PASS password.\r\n");
                pw.flush();
            }
        }

        return r;
    }

    /**
     * Handle the command QUIT.
     *
     * @param server This ftp server
     * @param thread The client thread
     * @param pw The Output stream, send response to the client
     * @throws IOException
     */
    public void cmdQuit(FTPServer server, ClientThread thread, PrintWriter pw) throws IOException {
        pw.write("221 Close the connection.\r\n");
        pw.flush();
        thread.getCli_socket().close();
        server.getClient_threads().remove(thread.getThread_id());
        int num = server.getClient_count() - 1;
        server.setClient_count(num);
        thread.setIsrunning(false);
        System.out.println("[*] End connection from (" + thread.getAddr() + ")");
        System.out.print("\n[*] Now has " + server.getClient_count() + " connection.");

    }

    /**
     * Handle the command CWD.
     *
     * @param server This server object
     * @param thread The client thread
     * @param command The command from client
     * @param pw The Output stream, send response to the client
     */
    public void cmdCwd(FTPServer server, ClientThread thread, String command, PrintWriter pw) {
        System.out.println(command);
        String filepath = "";

        if (command.split(" ").length == 2) {
            filepath = command.split(" ")[1];
            //fixme  Sometimes it can't work well.
            if (filepath.equals("/")) {
                thread.setAccess_path(System.getProperty("user.dir") + "\\src\\server\\resources");
            }
            //} else if (thread.getAccess_path().endsWith("resources/")) {
            //    thread.setAccess_path(System.getProperty("user.dir") + "\\src\\server\\resources");
            //}
            //if (!filepath.startsWith(thread.getAccess_path())) {
            //    filepath = thread.getAccess_path() + filepath;
            //}
            String p = thread.getAccess_path();
            String[] f1 = thread.getAccess_path().split("/");
            String[] f2 = filepath.split("/");
            int i = f1.length;
            if (f2.length > 1) {
                if (f1[i - 1].equals(f2[1])) {
                    for (int j = 2; j < f2.length; j++) {
                        System.out.println(f2[j]);
                        p += "/" + f2[j];
                    }
                    filepath = p;
                } else {
                    filepath = thread.getAccess_path() + filepath;
                }
            }
            System.out.println(filepath);
            filepath = filepath.replace("\\", "/");
            File file = new File(filepath);
            System.out.println("=============" + filepath);
            if (file.exists()) {
                if (file.isDirectory()) {
                    thread.setAccess_path(filepath);
                    String path = thread.getAccess_path();
                    pw.write("250 CWD command successful. Using filepath change to:" + path + "\r\n");
                    pw.flush();
                } else {
                    pw.write("550 No such directory.\r\n");
                    pw.flush();
                }
            } else {
                pw.write("550 No such directory-.\r\n");
                pw.flush();
            }
        } else {
            pw.write("500 please use command: CWD filepath.\r\n");
            pw.flush();
        }
    }

    /**
     * Handle the command PWD.
     *
     * @param thread The client client
     * @param pw The The Output stream, send response to the client
     */
    public void cmdPwd(ClientThread thread, PrintWriter pw) {
        String path = thread.getAccess_path();
        System.out.println(path); //TODO test
        Pattern pattern = Pattern.compile("/src/server/resources.*");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            int s = matcher.start() + 21;
            int e = path.length();
            if (s == e) {
                path = "/";
            } else {
                path = path.substring(s, e);
            }
        }
        pw.write("257 \"" + path + "\" is your current location\n");
        pw.flush();
    }

    /**
     * Handle the command RETR.
     *
     * @param server This ftp server
     * @param thread The client thread
     * @param command The command from client
     * @param pw The Output stream, send response to the client
     * @throws IOException
     */
    public void cmdRetr(FTPServer server, ClientThread thread, String command, PrintWriter pw) throws IOException {
        String[] f = command.split("/");
        String filename = f[f.length - 1];

        String filepath = thread.getAccess_path() + "\\" + filename;
        File file = new File(filepath);

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(thread.getDtp_socket().getOutputStream()));
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        byte[] buf = new byte[1460];

        pw.write("150 Accepted data connection. " + getFileSize(thread, thread.getAccess_path(), file) + " bytes to download.\r\n");
        pw.flush();
        System.out.println("[*] Start to transfer file: " + filepath + " to (client thread " + thread.getThread_id() + ")");
        int len;
        while ((len = dis.read(buf, 0, 1460)) != -1) {
            out.write(buf, 0, len);
            out.flush();
        }
        System.out.println("[*] Finish to transfer file: " + filepath + " to (client thread " + thread.getThread_id() + ")");
        pw.write("226 File successfully transferred\r\n");
        pw.flush();
        out.flush();
        out.close();
        dis.close();
        thread.getDtp_socket().close();
        thread.getDtp().close();
        thread.setPasvmode(false);
    }

    /**
     * Handle the command STOR.
     *
     * @param server This ftp server
     * @param thread The client thread
     * @param command The command from client
     * @param pw The Output stream, send response to the client
     * @throws IOException
     */
    public void cmdStor(FTPServer server, ClientThread thread, String command, PrintWriter pw) throws IOException {
        String savepath = "";
        Pattern pattern = Pattern.compile("STOR .+\\..+");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int s = matcher.start() + 5;
            int e = command.length();
            savepath = command.substring(s, e);
        }

        String[] s = savepath.split("/");
        String path = thread.getAccess_path();

        for (int i = 1; i < s.length - 1; i++) {
            path += "\\" + s[i];
        }
        path = path.replace("\\", "/");
        File p = new File(path);
        p.mkdirs();
        pw.write("150 Accepted data connection.\r\n");
        pw.flush();

        File file = new File(thread.getAccess_path() + "/" + savepath);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        DataInputStream dis = new DataInputStream(thread.getDtp_socket().getInputStream());
        byte[] buf = new byte[1460];

        //System.out.println("filesize:" + getFileSize(thread, thread.getAccess_path(), file));
        System.out.println("[*] Start to receive file: " + savepath + " from (client thread " + thread.getThread_id() + ")");
        int len;
        while ((len = dis.read(buf, 0, 1460)) != -1) {
            out.write(buf, 0, len);
            out.flush();
        }
        System.out.println("[*] Finish to receive file: " + savepath + " from (client thread " + thread.getThread_id() + ")");
        pw.write("226 File successfully transferred\r\n");
        pw.flush();
        out.flush();
        out.close();
        dis.close();
        thread.getDtp_socket().close();
        thread.getDtp().close();
        thread.setPasvmode(false);
    }


    /**
     * Handle the command SIZE.
     *
     * @param server This ftp server
     * @param thread The client thread
     * @param command The command from client
     * @param pw The Output stream, send response to the client
     */
    public void cmdSize(FTPServer server, ClientThread thread, String command, PrintWriter pw) {
        String filename = "";
        String filepath = "";
        Pattern pattern = Pattern.compile("SIZE .+\\..+");
        Matcher m = pattern.matcher(command);
        if (m.find()) {
            filename = command.substring(5, command.length());
        }
        if (!filename.startsWith(thread.getAccess_path())) {
            filepath = thread.getAccess_path() + "/" + filename;
        } else {
            filepath = filename;
        }
        System.out.println("filepath:" + filepath);
        System.out.println("filename:" + filename);
        //获取文件大小
        File file = new File(filepath);
        pw.write("213 " + file.length() + "\r\n");
        pw.flush();
    }

    /**
     * Handle the command PASV.
     *
     * @param thread The client thread
     * @param pw The Output stream, send response to the client
     * @throws IOException
     */
    public void cmdPasv(ClientThread thread, PrintWriter pw) throws IOException {
        int port_high;
        int port_low;
        ServerSocket dtp;
        Socket dtp_socket = null;
        String resp;
        Random ra = new Random();
        String serverhost;
        while (true) {
            port_high = 1 + ra.nextInt(20);
            port_low = 100 + ra.nextInt(1000);
            serverhost = InetAddress.getLocalHost().toString();
            try {
                dtp = new ServerSocket(port_high * 256 + port_low);
                serverhost = serverhost.split("/")[1];
                resp = "227 Entering Passive Mode(" + serverhost.replace(".", ",") + "," + port_high + "," + port_low + ")";
                pw.write(resp + "\r\n");
                pw.flush();
                break;
            } catch (IOException e) {
                continue;
            }
        }
        dtp_socket = dtp.accept();
        thread.setDtp_socket(dtp_socket);
        thread.setDtp(dtp);     // 不用这个变量？
        thread.setPasvmode(true);
    }

    /**
     * Handle the command LIST.
     *
     * @param server This ftp server
     * @param thread The client thread
     * @param pw The Output stream, send response to the client
     * @throws IOException
     */
    public void cmdList(FTPServer server, ClientThread thread, PrintWriter pw) throws IOException {
        DataOutputStream data_dos;
        String[] filelist = new String[0];

        data_dos = new DataOutputStream(thread.getDtp_socket().getOutputStream());

        File file = new File(thread.getAccess_path());
        pw.write("150 open success, start...\r\n");
        pw.flush();

        if (file.isDirectory()) {
            filelist = file.list();
        }
        String resp = "\n";
        String isDoc;
        for (String filename : filelist) {
            File f = new File(thread.getAccess_path() + "/" + filename);
            if (f.isDirectory()) {
                isDoc = "d";
            } else {
                isDoc = "-";
            }
            System.out.println(thread.getAccess_path() + "/" + filename);
            Long time = f.lastModified();
            SimpleDateFormat dateFormater = new SimpleDateFormat("MMM dd yyyy", new Locale("en"));
            Long size = server.getFileSize(thread, filename, f);
            resp += isDoc + " " + size + " " + dateFormater.format(time) + " " + filename + " \n";
        }
        if (resp != null) {
            data_dos.writeUTF(resp);
        } else {
            data_dos.writeUTF("550 No such file.");
        }
        pw.write("226 Finish data send. \r\n");
        pw.flush();

        data_dos.close();
        thread.getDtp_socket().close();
        thread.getDtp().close();
        thread.setPasvmode(false);
    }

    /**
     * Given a file, get it's size.
     *
     * @param thread The client thread
     * @param fpath The file path
     * @param f The file want to get size
     * @return The file size
     * @throws IOException
     */
    public long getFileSize(ClientThread thread, String fpath, File f) throws IOException {
        long size = 0;
        if (f.isDirectory()) {
            String[] fl = f.list();
            for (String aFL : fl) {
                String filepath = thread.getAccess_path() + "/" + fpath + "/" + aFL;
                File newf = new File(filepath);
                size += getFileSize(thread, aFL, newf);
            }
        } else {
            size = f.length();
        }
        return size;
    }

    public Map<String, String> getUser() {
        return user;
    }

    public Map<Integer, ClientThread> getClient_threads() {
        return client_threads;
    }

    public int getClient_count() {
        return client_count;
    }

    public void setClient_count(int num) {
        client_count = num;
    }

}
