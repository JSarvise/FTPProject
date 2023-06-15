import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {

    private Path serverDirectory = Paths.get(".\\server_directory");
    private int controlPort = 21;
    private int dataPort = 20;
    private ServerSocket controlSocket;
    private Socket dataConnection;
    private boolean isRunning = true;
    private boolean dataConnectionStatus = false;
    private PrintWriter controlWriter;
    private BufferedReader controlReader;
    private PrintWriter dataWriter;
    private BufferedReader dataReader;

    public static void main(String[] args) {
        Server ftpServer = new Server();
        ftpServer.listen();
    }

    public Server() {
        try {
            controlSocket = new ServerSocket(controlPort);
        } catch(IOException e) {
            System.out.println("Could not create the server socket at port "+ controlPort);
            System.exit(-1);
        }

        System.out.println("Listening at port " + controlPort);
    }

    public void listen() {
        try {
            while(isRunning) {
                Socket controlConnection = controlSocket.accept();
                controlReader = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
                controlWriter = new PrintWriter(controlConnection.getOutputStream(), true);
                sendControlMessage("220 Service ready for new user");

                while(isRunning) {
                    try {
                        String[] input = controlReader.readLine().split(" ");
                        switch (input[0]) {
                            case "PORT": {
                                String message = port(input[1]);
                                controlWriter.println(message);
                                break;
                            }
                            case "LIST": {
                                if(dataConnection.isClosed()) {
                                    controlWriter.println("503 Bad sequence of commands");
                                } else {
                                    list();
                                    closeDataConnection();
                                }
                                break;
                            }
                            case "RETR": {
                                if(dataConnection.isClosed()) {
                                    controlWriter.println("503 Bad sequence of commands");
                                } else {
                                    retrieve(input[1]);
                                    closeDataConnection();
                                }
                                break;
                            }
                            case "STOR": {
                                if(dataConnection.isClosed()) {
                                    controlWriter.println("503 Bad sequence of commands");
                                } else {
                                    store(input[1]);
                                    closeDataConnection();
                                }
                                break;
                            }
                            default:
                        }
                    } catch (SocketException | EOFException e) {
                        System.out.println("Client disconnected unexpectedly.");
                        controlConnection.close();
                        controlConnection = controlSocket.accept();
                        controlReader = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
                        controlWriter = new PrintWriter(controlConnection.getOutputStream(), true);
                        sendControlMessage("220 Service ready for new user");
                    }
                }
            }
        } catch(IOException e) {
            System.out.println("Could not accept a new connection");
        }

        try {
            controlSocket.close();
            System.out.println("Server stopped cleanly. Exiting...");
        } catch(IOException e) {
            System.out.println("Could not stop the server cleanly. Exiting...");
            System.exit(-1);
        }
    }

    private void sendControlMessage(String message) {
        controlWriter.println(message);
    }

    private void list() {
        File directory = new File(serverDirectory.toString());
        if(directory.exists() && directory.canRead()) {
            sendControlMessage("150 File status okay; about to open data connection");
            File[] files = directory.listFiles();
            for(File file : files) {
                if(file.isFile()) {
                    dataWriter.println(file.getName());
                }
            }
            sendControlMessage("226 Closing data connection. Requested file action successful");
        } else {
            sendControlMessage("550 Requested action not taken. File unavailable");
        }
        closeDataConnection();
    }

    private void retrieve(String path) {
        BufferedOutputStream dataWriter = null;
        try {
            dataWriter = new BufferedOutputStream(dataConnection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(serverDirectory.toString() + "\\" + path);
        if(file.exists() && file.canRead()) {
            sendControlMessage("150 File status okay; about to open data connection");
            FileInputStream fileIn = null;
            try {
                fileIn = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            try {
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataWriter.write(buffer, 0, bytesRead);
                }
                sendControlMessage("226 Closing data connection. Requested file action successful");
            } catch (IOException e) {
                sendControlMessage("426 Connection closed; transfer aborted");
                e.printStackTrace();
            }
        } else {
            sendControlMessage("550 Requested action not taken. File unavailable");
        }
        closeDataConnection();
    }

    private void store(String fileName) {
        File file = new File(serverDirectory.toString() + "\\" + fileName);
        try {
            if (file.createNewFile()) {
                sendControlMessage("150 File status okay; about to open data connection");

                BufferedInputStream dataReader = new BufferedInputStream(dataConnection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = dataReader.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                fileOutputStream.close();
                dataReader.close();
                sendControlMessage("226 Closing data connection. Requested file action successful");
            } else {
                sendControlMessage("550 Requested action not taken. File already exists");
            }
        } catch (IOException e) {
            sendControlMessage("426 Connection closed; transfer aborted");
            e.printStackTrace();
        }
    }

    private String port(String hostPort) {
        String[] structure = hostPort.split(",");
        String ip = structure[0]+"."+structure[1]+"."+structure[2]+"."+structure[3];
        int port = (Integer.parseInt(structure[4])*256)+Integer.parseInt(structure[5]);
        try {
            createDataConnection(ip, port);
            return "200 Command okay";
        } catch (IOException e) {
            e.printStackTrace();
            return "500 Couldn't open connection";
        }
    }

    private void createDataConnection(String ip, int port) throws UnknownHostException, IOException {
        InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
        dataConnection = new Socket(ip, port, serverAddr, dataPort);
        dataWriter = new PrintWriter(dataConnection.getOutputStream(), true);
    }

    private void closeDataConnection() {
        try {
            dataConnection.close();
            dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
