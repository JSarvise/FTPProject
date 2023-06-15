import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;

public class Server {

    // Define the path to the server's directory
    Path serverDirectory = Paths.get(".\\server_directory");

    // Define the default control and data ports
    private int controlPort = 21;
    private int dataPort = 20;

    // Define the server and client sockets
    private ServerSocket controlSocket;
    private Socket dataConnection;

    // Define control flags
    private boolean isRunning = true;
    private boolean dataConnectionStatus = false;

    // Define the control and data IO streams
    private PrintWriter controlWriter;
    private BufferedReader controlReader;
    private PrintWriter dataWriter;
    private BufferedReader dataReader;


    public static void main(String[] args) {
        Server ftpServer = new Server();

        ftpServer.listen();
    }

    // Server constructor
    public Server() {
        try {
            // Create a new server socket for control connection
            controlSocket = new ServerSocket(controlPort);
        } catch(IOException e) {
            System.out.println("Could not create the server socket at port "+ controlPort);
            System.exit(-1);
        }

        System.out.println("Listening at port " + controlPort);
    }

    // Listen for incoming control connection
    public void listen() {
        try {
            while(isRunning) {
                // Accept incoming control connection
                Socket controlConnection = controlSocket.accept();

                // Define the control input/output streams
                controlReader = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
                controlWriter = new PrintWriter(controlConnection.getOutputStream(), true);

                // Send initial service ready message
                sendControlMessage("220 Service ready for new user");

                while(isRunning) {
                    // Listen for incoming control commands
                    try {
                        // Read incoming command and parse arguments
                        String[] input = controlReader.readLine().split(" ");

                        // Switch on the command type
                        switch (input[0]) {
                            case "PORT": {
                                // Handle PORT command
                                String message = port(input[1]);
                                controlWriter.println(message);
                                
                                break;
                            }
                            case "LIST": {
                                // Handle LIST command
                                if(dataConnection.isClosed()) {
                                    controlWriter.println("503 Bad sequence of commands");
                                } else {
                                    list();
                                    closeDataConnection();
                                }
                                
                                break;
                            }
                            
                            case "RETR": {
                            	//Handle downloading from the server
                            	if(dataConnection.isClosed()) {
                                    controlWriter.println("503 Bad sequence of commands");
                                } else {
                                    retrieve(input[1]);
                                    closeDataConnection();
                                }
                            	break;
                            }
                            default:
                        }
                    } catch (SocketException | EOFException e) {
                        // Handle unexpected client disconnection
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

        // Close control socket when server is stopped
        try {
            controlSocket.close();
            System.out.println("Server stopped cleanly. Exiting...");
        } catch(IOException e) {
            System.out.println("Could not stop the server cleanly. Exiting...");
            System.exit(-1);
        }
    }

    // Send a control message to the client
    private void sendControlMessage(String message) {
        controlWriter.println(message);
    }

    // List files in the server's directory and send them to the client
    private void list() {
        File directory = new File(serverDirectory.toString());
        if(directory.exists() && directory.canRead())
        {
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
    
    /*private void serverSendFile(OutputStream outputStream, Path filePath) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }*/
    
    private void retrieve(String path) {
    	
    	BufferedOutputStream dataWriter = null;
    	
		try {
			dataWriter = new BufferedOutputStream(dataConnection.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	File file = new File(serverDirectory.toString() + "\\" + path);
    	if(file.exists() && file.canRead())
        {
    		sendControlMessage("150 File status okay; about to open data connection");
    		
			FileInputStream fileIn = null;
			try {
				fileIn = new FileInputStream(file);		
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
				sendControlMessage("426 Connection closed; transfer aborted");
				e.printStackTrace();
			}
        } else {
        	sendControlMessage("550 Requested action not taken. File unavailable");
        }   
    	
    	closeDataConnection();
    }

    // Handle PORT command
    private String port(String hostPort) {
        String[] structure = hostPort.split(",");

        // Extract IP address and port from client message
        String ip = structure[0]+"."+structure[1]+"."+structure[2]+"."+structure[3];
        int port = (Integer.parseInt(structure[4])*256)+Integer.parseInt(structure[5]);

        try {
            // Create a new data connection to the client
            createDataConnection(ip, port);
            return "200 Command okay";
        } catch (IOException e) {
            e.printStackTrace();
            return "500 Couldn't open connection";
        }
    }

    // Create a new data connection to the client
    private void createDataConnection(String ip, int port) throws UnknownHostException, IOException {
    	InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
        dataConnection = new Socket(ip, port, serverAddr,dataPort); //Data conection from port 20 to client specified port.
        dataWriter = new PrintWriter(dataConnection.getOutputStream(), true);
    }

    // Close the data connection
    private void closeDataConnection() {
        try {
            dataConnection.close();
            dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
