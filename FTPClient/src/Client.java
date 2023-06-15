import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    // Define the server's IP
    private static final String ipServer = "localhost";

    // Define the path to the client's directory
    Path clientDirectory = Paths.get(".\\client_directory");

    // Define the control and data sockets
    public Socket controlSocket;
    public ServerSocket dataSocket;

    // Define the control and data input/output streams
    public BufferedReader commandClientReader;
    public PrintWriter commandClientWriter;
    public BufferedReader dataClientReader;

    // Client constructor
    public Client() throws IOException {
        // Create a new control socket to the server
        controlSocket = new Socket(ipServer, 21);

        // Define the control input/output streams
        commandClientReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        commandClientWriter = new PrintWriter(controlSocket.getOutputStream(), true);
    }

    public static void main(String[] args) throws IOException {
        try {
            // Create a new client
            Client client = new Client();

            // Initialize input string
            String clientInput = "";

            // Print server's initial message
            System.out.println(client.commandClientReader.readLine());

            // Define a new scanner for user input
            Scanner sc = new Scanner(System.in);

            while (true) {  // Loop indefinitely, you should add a mechanism to break the loop
                // Print menu
                client.bringMenu();

                // Get user input
                clientInput = sc.nextLine();

                // Switch on user input
                switch (clientInput) {
                    case "1":  //QUIT
                    	break;
                    case "2":	// List files
                    	
                        client.listCommand();
                        
                        break;
                    case "3":

                        break;

                    case "4": // Retrieve file from the server
                    	System.out.println("Introduce the name of the file you want to download: ");
                    	clientInput = sc.nextLine();
                    	client.retrieveCommand(clientInput);
                    	System.out.println("retrieveCommand funciona");
                    	
                        break;

                    default:
                        System.out.println("\r\nInvalid option.");
                        continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Can't connect.");
            System.exit(-1);
        }
    }

    // Print menu to console
    private void bringMenu() {
        System.out.println("\r\nInput one of these numbers depending on what you want to do:");
        System.out.println("Options:");
        System.out.println("1. Close the client connection.");
        System.out.println("2. List all files");
        System.out.println("3. Upload a file");
        System.out.println("4. Download a file");
    }

    // Format a PORT command message
    public String makePort(int portNumber) {
        String ip = "127,0,0,1";
        int p1 = portNumber/256;
        int p2 = portNumber%256;
        String ipFix = ip + "," + p1 + "," + p2;
        return ipFix;
    }

    // Create a new data connection to the server
    public Socket createDataConnection() {
    	try {
    		//Create client data socket
			dataSocket = new ServerSocket(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // Send a PORT command to the server
        commandClientWriter.println("PORT " + makePort(dataSocket.getLocalPort()));
        Socket socket = null;
        try {
            // Accept incoming data connection from the server
            socket = dataSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return socket;
    }
    
    public void sendListCommand(String pathname)
    {
    	if(pathname.equals(""))
    	{
    		commandClientWriter.println("LIST");
    	}else {
    		commandClientWriter.println("LIST " + pathname);
    	}
    }
    
    public void listCommand() throws IOException {
    	Socket listSocket = createDataConnection();
        
        if(readServerResponse().equals("200"))
        {
            sendListCommand("");
            if(readServerResponse().equals("150"))
            {
            	dataClientReader = new BufferedReader(new InputStreamReader(listSocket.getInputStream()));
                String listOutput = dataClientReader.readLine();
                while(listOutput != null) {
                    System.out.println(listOutput);
                    listOutput = dataClientReader.readLine();
                }
                               
                if(!readServerResponse().equals("226"))
                {            
                	System.out.println("An error ocurred during connection");
                }
            } else {
            	System.out.println("Could not open server directory");
            }
        }
        dataSocket.close();
        listSocket.close();
    }
    
    public void sendRetrieveCommand(String fileName)
    {
    		commandClientWriter.println("RETR " + fileName);
    }
    
    public void retrieveCommand(String fileName) throws IOException {
    	Socket retrieveSocket = createDataConnection();
    	
    	System.out.println("data connection creada ");
    	if(readServerResponse().equals("200"))
        {
    		sendRetrieveCommand(fileName);
    		if(readServerResponse().equals("150"))
            {
    			System.out.println("Empezar descarga ");
    			BufferedInputStream fileInputReader = new BufferedInputStream(retrieveSocket.getInputStream());
    			File file = new File(clientDirectory.toString() + "\\" + fileName);
    			
    			if(file.createNewFile())
    			{
    				System.out.println("crear archivo ");
    				FileOutputStream fileOut = new FileOutputStream(file);
        			
        			int count;
        			byte[] buffer = new byte[1024];
        			while((count = fileInputReader.read(buffer))>0)
        			{
        				fileOut.write(buffer, 0, count);
        			}
        			if(readServerResponse().equals("226"))
                    {
        				fileOut.close();
                    } else {
                    	System.out.println("Connection failed");
                    }
    			} else
    			{
    				System.out.println("File already exists");
    			}
    			
            } else {
            	System.out.println("File doesn't exist");
            }
        }
    	while(commandClientReader.ready())
		{
    		commandClientReader.readLine();
		}
    	retrieveSocket.close();
    	dataSocket.close();
    }
    
    public String readServerResponse()
    {
    	String command[] = null;
		try {
			command = commandClientReader.readLine().split(" ");
			System.out.println(command[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return command[0];
    }
}
