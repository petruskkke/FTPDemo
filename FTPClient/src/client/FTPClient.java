package client;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FTPClient class, could connect FTP server such ftp.neu.edu.cn or class FTPServer.

 * @author Zhanghan Ke
 *         <p>
 *         Test ftp server:
 *         121.42.118.56
 *         USER qxu1606470417
 *         PASS 2025642313
 */
public class FTPClient {
    private String name;
    private String host = "";
    private int port = 0;
    private Socket client;
    private Socket data_socket = null;
    private String resources_path;
    private List<String> downloadDocument = null;
    private boolean isrunning = false;
    private boolean pasvmode = false;
    private boolean hasfile = true;

    /**
     * Init method of this client.
     *
     * @param name Client name
     */
    public FTPClient(String name) {
        this.name = name;
        this.resources_path = System.getProperty("user.dir") + "\\src\\client\\resources";
    }

    /**
     * Start this ftp client.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw;
        BufferedReader br;
        System.out.println("[*] Please enter the socket address you want to connect(like 127.0.0.1:9999)...");
        addrCheck(sc);

        client = new Socket(this.host, this.port);
        pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
        isrunning = userLogin(sc, pw, br);

        while (isrunning) {
            String resp;
            System.out.print(">>>");
            String command = sc.nextLine();

            statusJudge(command, pw, br);
            if (downloadDocument != null) {
                for (String file : downloadDocument) {
                    command = "RETR " + file;
                    statusJudge(command, pw, br);
                    pw.write(command + "\r\n");
                    pw.flush();
                    resp = receiveResponse(br);
                    System.out.println(resp);
                    respHandle(command, resp, pw, br);
                }
                downloadDocument = null;
                continue;
            }
            pw.write(command + "\r\n");
            pw.flush();
            resp = receiveResponse(br);
            System.out.println(resp);

            respHandle(command, resp, pw, br);
        }
    }

    /**
     * Check on the ip addr.
     *
     * @param sc The Stream read input from keyboard
     * @throws UnknownHostException
     */
    private void addrCheck(Scanner sc) throws UnknownHostException {
        Pattern pattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+");
        while (true) {
            System.out.print(">>>");
            String in = sc.next();
            if (in.equals("QUIT")) {
                System.out.println("[*] 221 Service closing control connection.");
                break;
            }
            if (in.startsWith("ftp")) {
                InetAddress id = InetAddress.getByName(in);
                this.host = id.getHostAddress();
                this.port = 21;
                break;
            }
            Matcher matcher = pattern.matcher(in);
            if (matcher.find()) {
                this.host = in.split(":")[0];
                this.port = Integer.parseInt(in.split(":")[1]);
                break;
            } else {
                System.out.print(this.host + ":" + this.port);
                System.out.println("[!] Invalid address!");
            }
        }
    }

    /**
     * This func finish user's login.
     *
     * @param sc The stream read input from keyboard
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return  While user login successful, return true
     * @throws IOException
     */
    private boolean userLogin(Scanner sc, PrintWriter pw, BufferedReader br) throws IOException {
        String resp;
        int statuscode;
        boolean login_tag = false;

        resp = receiveResponse(br);
        System.out.println(resp);

        statuscode = usernameCheck(sc, pw, br);
        switch (statuscode) {
            case 331:
                System.out.println("[*] 331 Username okay, need password.");
                statuscode = passwordCheck(sc, pw, br);
                if (statuscode == 530) {
                    System.out.println("[!] 530 Not logged in.");
                } else if (statuscode == 230) {
                    System.out.println("[*] 230 User logged in, proceed.");
                    login_tag = true;
                } else {
                    System.out.println("[!] 530 Not logged in.");
                }
                break;
            case 230:
                System.out.println("[*] 331 Username okay, need password.");
                login_tag = true;
                break;
            case 530:
                break;
            case 221:
                System.out.println("[*] 221 Service closing control connection.");
                client.close();
                break;
        }
        return login_tag;
    }

    /**
     * Check on username.
     *
     * @param sc The stream read input from keyboard
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return  The statue code server response after handle command USER
     * @throws IOException
     */
    private int usernameCheck(Scanner sc, PrintWriter pw, BufferedReader br) throws IOException {
        String resp;
        String user;
        int statuscode;
        System.out.println("[*] Please to login...");
        while (true) {
            System.out.print(">>>");
            user = sc.nextLine();
            if (user.equals("")) {
                continue;
            }
            if (user.equals("QUIT")) {
                statuscode = 221;
                break;
            }
            pw.write(user + "\r\n");
            pw.flush();
            resp = receiveResponse(br);
            System.out.println(resp);
            statuscode = respCodeHandle(resp);
            if (statuscode != 530 && statuscode != 500) {
                break;
            }
        }
        return statuscode;

    }

    /**
     * Check on the user's password.
     *
     * @param sc The stream read input from keyboard
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return The statue code server response after handle command PASS
     * @throws IOException
     */
    private int passwordCheck(Scanner sc, PrintWriter pw, BufferedReader br) throws IOException {
        String resp;
        String pass;
        int statuscode;
        System.out.println("[*] Please enter your password...");
        while (true) {
            System.out.print(">>>");
            pass = sc.nextLine();

            if (pass.equals("QUIT")) {
                statuscode = 221;
                break;
            }
            pw.write(pass + "\r\n");
            pw.flush();
            resp = receiveResponse(br);
            System.out.println(resp);
            statuscode = respCodeHandle(resp);
            if (statuscode != 530 && statuscode != 500) {
                break;
            }
        }
        return statuscode;
    }

    /**
     * Finish the req: TYPE I, bubt now can't change to other type.
     *
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void typeRequest(PrintWriter pw, BufferedReader br) throws IOException {
        pw.write("TYPE I\r\n");
        pw.flush();
        String resp = receiveResponse(br);
        System.out.println(resp);
        respHandle("TYPE", resp, pw, br);
    }

    /**
     * Finish the req: PASV
     *
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void pasvRequest(PrintWriter pw, BufferedReader br) throws IOException {
        pw.write("PASV\r\n");
        pw.flush();
        String resp = receiveResponse(br);
        System.out.println(resp);
        respHandle("PASV", resp, pw, br);
    }

    /**
     * Finish the req: REST
     *
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @param filesize The size want to jump in the file
     * @throws IOException
     */
    private void restRequest(PrintWriter pw, BufferedReader br, long filesize) throws IOException {
        pw.write("REST " + filesize + "\r\n");
        pw.flush();
        String resp = receiveResponse(br);
        System.out.println(resp);
        respHandle("REST", resp, pw, br);
    }

    /**
     * Do some auto work before the input command.
     *
     * @param command The command user input in client
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void statusJudge(String command, PrintWriter pw, BufferedReader br) throws IOException {
        if (command.equals("LIST")) {
            if (!pasvmode) {
                pasvRequest(pw, br);
            }
        }
        if (command.equals("QUIT")) {
            if (pasvmode) {
                data_socket.close();
                pasvmode = false;
            }
        }
        if (command.startsWith("STOR")) {
            if (!pasvmode) {
                typeRequest(pw, br);
                pasvRequest(pw, br);
            }
        }
        if (command.startsWith("RETR")) {
            retrWayChoose(command, pw, br);
        }

    }

    /**
     * Choose which kind way to download file.
     *
     * @param command The download commend user input
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void retrWayChoose(String command, PrintWriter pw, BufferedReader br) throws IOException {
        Pattern pattern = Pattern.compile("RETR .+\\..+");
        List<String> allFiles = new ArrayList<>();
        String[] s = command.split(" ");
        String resp;
        int statuscode;

        Matcher m = pattern.matcher(command);
        if (!m.find()) {
            String folder = command.substring(5, command.length());
            String cur_location = getCurrentLocation(pw, br);
            if (!cur_location.equals("/")) {
                folder = cur_location + "/" + folder;
            } else {
                folder = cur_location + folder;
            }

            allFiles = getAllFilesInFolder(folder, pw, br);
            downloadDocument = allFiles;

        } else {
            if (!pasvmode) {
                typeRequest(pw, br);
                pasvRequest(pw, br);

                String filename = command.substring(5, command.length());
                long filesize = getFileSize(filename, pw, br);
                String filepath = resources_path + "/" + filename;
                filepath = filepath.replace("\\", "/");
                File file = new File(filepath);
                if (file.exists()) {
                    long cliFileSize = file.length();
                    if (cliFileSize == filesize) {
                        System.out.println("[*] Has download.");
                    } else if (cliFileSize < filesize) {
                        System.out.println("[*] Continue download.");
                        System.out.println("cliFilesize:" + cliFileSize);
                        System.out.println("filesize" + filesize);
                        restRequest(pw, br, cliFileSize);
                    } else {
                        System.out.println("[*] Reload.");
                    }
                }
            }
        }
    }

    /**
     * Get the ftp response code.
     *
     * @param resp The resp from ftp server
     * @return The resp code
     */
    private int respCodeHandle(String resp) {
        int statuscode;

        Pattern pattern = Pattern.compile("\\[\\*] \\d\\d\\d ");
        Matcher matcher = pattern.matcher(resp);
        if (matcher.find()) {
            int s = matcher.start() + 4;
            int e = matcher.end() - 1;
            statuscode = Integer.parseInt(resp.substring(s, e));
        } else {
            statuscode = -1;
        }
        return statuscode;
    }

    /**
     * Receive response from ftp server.
     *
     * @param br The buffered reader accept resp from ftp server
     * @return  The response string from server
     * @throws IOException
     */
    private String receiveResponse(BufferedReader br) throws IOException {
        String respStr = "";
        while (true) {
            String line = br.readLine();
            Pattern pattern = Pattern.compile("^[1-5][0-5][0-9] ");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                respStr += "[*] " + line;
                break;
            } else {
                respStr += "[*] " + line + "\n";
            }
        }
        return respStr;
    }

    /**
     * Handle the resp from  ftp server.
     *
     * @param command The download commend user input
     * @param resp The response str from server.
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void respHandle(String command, String resp, PrintWriter pw, BufferedReader br) throws IOException {
        int statuscode;
        statuscode = respCodeHandle(resp);
        switch (statuscode) {
            case 200:   //TYPE
                break;
            case 227:   //PASV
                pasvHandle(resp);
                System.out.println("[*] Set up data connection.");
                break;
            case 150:
                getData(command, pw, br);
                if (hasfile) {
                    String respStr = receiveResponse(br);
                    statuscode = respCodeHandle(respStr);
                    if (statuscode == 226) {
                        System.out.println(respStr);
                        data_socket.close();
                        pasvmode = false;
                    }
                }
                hasfile = true;
                break;
            case 213:   //SIZE
                break;
            case 425:
                break;
            //fixme  If change folder successful, don't do this case.
            case 550:
                if (data_socket != null) {
                    data_socket.close();
                    pasvmode = false;
                }
                break;
            case 500:
                if (data_socket != null) {
                    data_socket.close();
                    pasvmode = false;
                }
                break;
            case 221:
                clientQuit(pw, br);
                break;
            case -1:
                System.out.println("[!] Sorry, I can't deal it.");
        }
    }

    /**
     * User quit the client.
     *
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void clientQuit(PrintWriter pw, BufferedReader br) throws IOException {
        pw.close();
        br.close();
        client.close();
        isrunning = false;
    }

    /**
     * Change to the pasv mode.
     *
     * @param resp The response str from server
     * @throws IOException
     */
    private void pasvHandle(String resp) throws IOException {
        if (!pasvmode) {
            String[] s = resp.split(",");
            String port1 = s[4];
            String port2 = s[5].substring(0, s[5].length() - 1);
            int data_port = Integer.parseInt(port1) * 256 + Integer.parseInt(port2);
            data_socket = new Socket(host, data_port);
            pasvmode = true;
        }
    }

    /**
     * File the resp: LIST
     *
     * @throws IOException
     */
    private void listHandle() throws IOException {
        byte[] buf = new byte[2048];
        int len;
        DataInputStream dis = new DataInputStream(data_socket.getInputStream());
        //  fixme Some useless data in buf, should control it not been print.
        while ((len = dis.read(buf, 0, 2048)) != -1) {
            String s = new String(buf, "gbk");
            System.out.print(s);
        }
        System.out.println("\n");
        dis.close();
    }

    /**
     * Finish download work.
     *
     * @param command The download commend user input
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void retrHandle(String command, PrintWriter pw, BufferedReader br) throws IOException {
        DataInputStream dis = new DataInputStream(data_socket.getInputStream());
        String filename = "";

        String[] splitStr = command.split("/");
        if (splitStr.length != 1) {
            filename = splitStr[splitStr.length - 1];
        } else {
            Pattern pattern = Pattern.compile("RETR .+\\..+");
            Matcher m = pattern.matcher(command);
            if (m.find()) {
                filename = command.substring(5, command.length());
            }
        }
        String savepath = command.substring(5, command.length());
        String filepath = resources_path + "/" + savepath;
        System.out.println("-----------" + filepath);
        filepath = filepath.replace("\\", "/");
        File file = new File(filepath);

        if (file.exists()) {
            System.out.println("[*] The file has been exists()");
            System.out.println("[*] Filesize: " + file.length());
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
            byte[] buf = new byte[1460];
            System.out.println("[*] Continue to download file:" + filename);
            System.out.println("[*] Please waiting...");

            int len;
            while ((len = dis.read(buf, 0, 1460)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
            System.out.println("[*] Finish download file:" + filename);
            out.flush();
            out.close();
            dis.close();
        } else {
            System.out.println("[*] No such file.");

            String[] s = savepath.split("/");
            String path = resources_path;
            for (int i = 1; i < s.length - 1; i++) {
                path += "\\" + s[i];
            }
            path = path.replace("\\", "/") + "/";

            File p = new File(path);
            p.mkdirs();
            file.createNewFile();

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            byte[] buf = new byte[1460];

            System.out.println("[*] Begin to download file:" + filename);
            System.out.println("[*] Please waiting...");
            int len;
            while ((len = dis.read(buf, 0, 1460)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
            System.out.println("[*] Finish download file:" + filename);
            out.flush();
            out.close();
            dis.close();
            data_socket.close();
        }
    }

    /**
     * Finish the resp: STOR.
     *
     * @param command The download commend user input
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void storHandle(String command, PrintWriter pw, BufferedReader br) throws IOException {
        String filepath = null;
        Pattern pattern = Pattern.compile("STOR .+\\..+");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int s = matcher.start() + 5;
            int e = command.length();
            filepath = resources_path + "/" + command.substring(s, e);
        }
        File file = new File(filepath);

        if (file.exists()) {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(data_socket.getOutputStream()));
            byte[] buf = new byte[1460];

            System.out.println("[*] Begin to update file:" + filepath);
            System.out.println("[*] Please waiting...");
            int len;
            while ((len = dis.read(buf, 0, 1460)) != -1) {
                dos.write(buf, 0, len);
                dos.flush();
            }
            System.out.println("[*] Finish update file:" + filepath);
            dos.flush();
            dis.close();
            dos.close();
            data_socket.close();
        } else {
            System.out.println("[!] No such file.");
            hasfile = false;
            data_socket.close();
        }
    }

    /**
     * Judge a folder whether exists.
     *
     * @param filepath Where the file want to find
     * @throws IOException
     */
    private void isFolderExist(String filepath) throws IOException {
        System.out.println(filepath);
        File file = new File(filepath);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    /**
     * Before download folder, get all files in it.
     *
     * @param folder The folder which want to download
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return  The list of all files in given folder
     * @throws IOException
     */
    private List<String> getAllFilesInFolder(String folder, PrintWriter pw, BufferedReader br) throws IOException {
        String resp;
        String filelist = "";
        String filepath = "";
        int statuscode;
        List<String> allFiles = new ArrayList<>();
        //fixme When connect to different server, need to change this format.
        Pattern pattern = Pattern.compile("[A-Z][a-z][a-z]\\s+\\d+\\s+\\d+ ");
        folder = folder.replace("\\", "/");
        pw.write("CWD " + folder + "\r\n");
        pw.flush();
        resp = receiveResponse(br);
        System.out.println(resp);
        statuscode = respCodeHandle(resp);

        switch (statuscode) {
            case 550:
                break;
            case 250:
                statusJudge("LIST", pw, br);
                pw.write("LIST\r\n");
                pw.flush();
                resp = receiveResponse(br);
                System.out.println(resp);
                if (pasvmode) {
                    byte[] buf = new byte[2048];
                    DataInputStream dis = new DataInputStream(data_socket.getInputStream());
                    while (true) {
                        int i = dis.read(buf);
                        if (-1 == i) {
                            break;
                        }
                        String text = new String(buf, "gbk");
                        filelist += text;
                    }

                    //fixme  Sometimes it will be error if the filepath contains Chinese.
                    resp = receiveResponse(br);
                    System.out.println(resp);
                    pasvmode = false;
                    String[] filetext = filelist.split("\\n");

                    for (String file : filetext) {
                        Matcher m = pattern.matcher(file);
                        if (file.startsWith("d")) {
                            if (m.find()) {
                                String newfolder = file.substring(m.end(), file.length() - 1);
                                newfolder = folder + "/" + newfolder;
                                System.out.println(newfolder);
                                allFiles.addAll(getAllFilesInFolder(newfolder, pw, br));
                            }
                        } else {
                            if (m.find()) {
                                int s = m.end();
                                int e = file.length() - 1;
                                filepath = folder + "/" + file.substring(s, e);
                                allFiles.add(filepath);
                            }
                        }
                    }
                    dis.close();
                } else {
                    System.out.println("[!] Not pasv mode!");
                }
        }
        return allFiles;
    }

    /**
     * Get the size of given file.
     *
     * @param filename The file which want to get size
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return The size of the file been given
     * @throws IOException
     */
    private long getFileSize(String filename, PrintWriter pw, BufferedReader br) throws IOException {
        long filesize = 0;
        pw.write("SIZE " + filename + "\r\n");
        pw.flush();
        String resp = receiveResponse(br);
        int statuscode = respCodeHandle(resp);
        if (statuscode == 213) {
            filesize = Integer.parseInt(resp.split(" ")[2]);
        } else {
            System.out.println(resp);
        }
        return filesize;
    }

    /**
     * Get the location now been used.
     *
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @return The work path in client
     * @throws IOException
     */
    private String getCurrentLocation(PrintWriter pw, BufferedReader br) throws IOException {
        String cur_folder = "//";
        pw.write("PWD\r\n");
        pw.flush();

        String resp = receiveResponse(br);
        System.out.println(resp);
        Pattern pattern = Pattern.compile("\".+\"");
        Matcher matcher = pattern.matcher(resp);
        if (matcher.find()) {
            int s = matcher.start() + 1;
            int e = matcher.end() - 1;
            cur_folder = resp.substring(s, e);
        }
        return cur_folder;
    }

    /**
     * Choose the way and command receive data from server.
     *
     * @param command The download commend user input
     * @param pw The stream send commend to server
     * @param br The stream get response from server
     * @throws IOException
     */
    private void getData(String command, PrintWriter pw, BufferedReader br) throws IOException {
        if (command.equals("LIST")) {
            listHandle();
        }
        if (command.startsWith("RETR")) {
            retrHandle(command, pw, br);
        }
        if (command.startsWith("STOR")) {
            storHandle(command, pw, br);
        }

    }

}




