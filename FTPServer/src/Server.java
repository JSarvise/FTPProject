import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
 
public class Server {
	
	Path serverDirectory = Paths.get(".\\server_directory");
	
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
	
	
	public static void main(String[] args)
	{
		Server ftpServer = new Server();
 
		ftpServer.listen();
	}
	public Server()
	{
		try {
			controlSocket = new ServerSocket(controlPort);
		}
		catch(IOException e)
		{
			System.out.println("Could not create the server socket at port "+ controlPort);
			System.exit(-1);
		}
 
		System.out.println("Listening at port " + controlPort);
	}
 
	public void listen()
	{
		try{
			while(isRunning)
			{
				Socket controlConnection = controlSocket.accept();
				
				// We create a Buffered Reader to avoid costly read operations every time
				controlReader = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
 
				// We want to enable automatic flushing so each line gets sent over the socket when it's written
				controlWriter = new PrintWriter(controlConnection.getOutputStream(), true);
 
				sendControlMessage("220 Service ready for new user");
				
				while(isRunning) 
				{
					if(controlConnection.isClosed())
					{
						controlConnection = controlSocket.accept();
						
						// We create a Buffered Reader to avoid costly read operations every time
						controlReader = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
		 
						// We want to enable automatic flushing so each line gets sent over the socket when it's written
						controlWriter = new PrintWriter(controlConnection.getOutputStream(), true);
					}
					//Read input coming from client and getting the command that we use for the switch case
					String[] input = controlReader.readLine().split(" ");
					
					System.out.println(input[0]);
					switch (input[0]) {
					case "PORT": {
						String message = port(input[1]);
						controlWriter.println(message);

					}
					case "LIST": {
						if(dataConnection.isClosed())
						{
							controlWriter.println("503 Bad sequence of commands");
						}else {
							list();
							closeDataConnection();
						}
					}
					default:
						
					}
				}
			}
		}
 
		catch(IOException e)
		{
			System.out.println("Could not accept a new connection");
		}
 
		try {
			controlSocket.close();
			System.out.println("Server stopped cleanly. Exiting...");
		}
		catch(IOException e)
		{
			System.out.println("Could not stop the server cleanly. Exiting...");
			System.exit(-1);
		}
	}
 
	private void sendControlMessage(String message)
	{
		controlWriter.println(message);
	}
	
	private void list()
	{
		//Obtain list of files from desired directory
		File directory = new File(serverDirectory.toString());
		File[] files = directory.listFiles();
		//Send the list of files to the client
		for(File file : files) {
			if(file.isFile())
			{
				dataWriter.println(file.getName());
			}
		}
		closeDataConnection();
	}
	
	private String port(String hostPort)
	{
	
		String[] structure = hostPort.split(",");
		
		String ip = structure[0]+"."+structure[1]+"."+structure[2]+"."+structure[3];
		
		int port = (Integer.parseInt(structure[4])*256)+Integer.parseInt(structure[5]);
		try {
			createDataConnection(ip, port);
			return "200 Command okay";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "500 Couldn't open connection";
		}
	}
	
	private void createDataConnection(String ip, int port) throws UnknownHostException, IOException
	{		
		System.out.println(ip + " " + port);
		dataConnection = new Socket(ip, port);
		dataWriter = new PrintWriter(dataConnection.getOutputStream(), true);
	}
	private void closeDataConnection()
	{		
		try {
			dataConnection.close();
			dataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
