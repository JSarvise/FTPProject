import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String ipServer = "localhost";
    private Path clientDirectory = Paths.get(".\\client_directory");
    private Socket controlSocket;
    private ServerSocket dataSocket;
    private BufferedReader commandClientReader;
    private PrintWriter commandClientWriter;
    private BufferedReader dataClientReader;

    public static void main(String[] args) throws IOException {
        try {
            Client client = new Client();
            client.start();
        } catch (IOException e) {
            System.err.println("Can't connect.");
            System.exit(-1);
        }
    }

    public void start() throws IOException {
        controlSocket = new Socket(ipServer, 21);
        commandClientReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        commandClientWriter = new PrintWriter(controlSocket.getOutputStream(), true);

        System.out.println(commandClientReader.readLine());

        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            String clientInput = scanner.nextLine();

            switch (clientInput) {
                case "1":
                    controlClientConnection();
                    scanner.close();
                    return;
                case "2":
                    listFiles();
                    break;
                case "3":
                    uploadFile();
                    break;
                case "4":
                    downloadFile();
                    break;
                default:
                    System.out.println("\r\nInvalid option.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println("\r\nInput one of these numbers depending on what you want to do:");
        System.out.println("Options:");
        System.out.println("1. Close the client connection.");
        System.out.println("2. List all files");
        System.out.println("3. Upload a file");
        System.out.println("4. Download a file");
    }

    private void controlClientConnection() throws IOException {
        commandClientWriter.println("QUIT");
        controlSocket.close();
        System.out.println("Client connection closed.");
    }

    private void listFiles() throws IOException {
        Socket listSocket = createDataConnection();

        if (readServerResponse().equals("200")) {
            sendListCommand("");
            if (readServerResponse().equals("150")) {
                dataClientReader = new BufferedReader(new InputStreamReader(listSocket.getInputStream()));
                String listOutput = dataClientReader.readLine();
                while (listOutput != null) {
                    System.out.println(listOutput);
                    listOutput = dataClientReader.readLine();
                }
                if (!readServerResponse().equals("226")) {
                    System.out.println("An error occurred during the connection.");
                }
            } else {
                System.out.println("Could not open the server directory.");
            }
        }
        dataSocket.close();
        listSocket.close();
    }

    private void sendListCommand(String pathname) {
        if (pathname.equals("")) {
            commandClientWriter.println("LIST");
        } else {
            commandClientWriter.println("LIST " + pathname);
        }
    }

    private void uploadFile() throws IOException {
        System.out.println("Enter the path of the file you want to upload: ");
        Scanner scanner = new Scanner(System.in);
        String filePath = scanner.nextLine();

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            String fileName = file.getName();

            Socket uploadSocket = createDataConnection();

            if (readServerResponse().equals("200")) {
                sendUploadCommand(fileName);
                if (readServerResponse().equals("150")) {
                    BufferedOutputStream fileOutputWriter = new BufferedOutputStream(uploadSocket.getOutputStream());
                    FileInputStream fileInput = new FileInputStream(file);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        fileOutputWriter.write(buffer, 0, bytesRead);
                    }

                    fileOutputWriter.flush();
                    fileInput.close();
                    fileOutputWriter.close();

                    if (readServerResponse().equals("226")) {
                        System.out.println("File uploaded successfully.");
                    } else {
                        System.out.println("Connection failed during file upload.");
                    }
                } else {
                    System.out.println("Server error: File upload not allowed.");
                }
            }
            uploadSocket.close();
            dataSocket.close();
        } else {
            System.out.println("File not found or is not a valid file.");
        }
    }

    private void sendUploadCommand(String fileName) {
        commandClientWriter.println("STOR " + fileName);
    }

    private void downloadFile() throws IOException {
        System.out.println("Enter the name of the file you want to download: ");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        Socket downloadSocket = createDataConnection();

        if (readServerResponse().equals("200")) {
            sendDownloadCommand(fileName);
            if (readServerResponse().equals("150")) {
                BufferedInputStream fileInputReader = new BufferedInputStream(downloadSocket.getInputStream());

                File file = new File(clientDirectory.toString() + "\\" + fileName);

                if (file.createNewFile()) {
                    FileOutputStream fileOut = new FileOutputStream(file);

                    int count;
                    byte[] buffer = new byte[1024];
                    while ((count = fileInputReader.read(buffer)) > 0) {
                        fileOut.write(buffer, 0, count);
                    }

                    fileOut.close();
                    System.out.println("File downloaded successfully.");
                } else {
                    System.out.println("File already exists.");
                }

                if (!readServerResponse().equals("226")) {
                    System.out.println("Connection failed.");
                }
            } else {
                System.out.println("File doesn't exist.");
            }
        }

        while (commandClientReader.ready()) {
            commandClientReader.readLine();
        }

        downloadSocket.close();
        dataSocket.close();
    }

    private void sendDownloadCommand(String fileName) {
        commandClientWriter.println("RETR " + fileName);
    }

    private String readServerResponse() {
        String[] command = null;
        try {
            command = commandClientReader.readLine().split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return command[0];
    }

    private Socket createDataConnection() {
        try {
            dataSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        commandClientWriter.println("PORT " + makePort(dataSocket.getLocalPort()));

        Socket socket = null;
        try {
            socket = dataSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return socket;
    }

    private String makePort(int portNumber) {
        String ip = "127,0,0,1";
        int p1 = portNumber / 256;
        int p2 = portNumber % 256;
        String ipFix = ip + "," + p1 + "," + p2;
        return ipFix;
    }
}
