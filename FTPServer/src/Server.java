
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
 
public class Server {
	
	private int controlPort = 21;
	private int dataPort = 20;
 
	private ServerSocket controlSocket;
	private Socket dataConnection;
 
	private boolean isRunning = true;
 
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
				
				//Read input coming from client and getting the command that we use for the switch case
				String[] input = controlReader.readLine().split(" ");
				
				while(isRunning) 
				{
					switch (input[0]) {
					case "PORT": {
						String message = port(controlReader.readLine());
						controlWriter.println(message);

					}
					case "LIST": {
						
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
		
	}
	
	private String port(String hostPort)
	{
	
		String[] structure = hostPort.split(",");
		
		String ip = structure[0]+"."+structure[1]+"."+structure[2]+"."+structure[3];
		
		int port = (Integer.parseInt(structure[4])*256)+Integer.parseInt(structure[5]);
		
		try {
			dataConnection = new Socket(ip, port);
			dataWriter = new PrintWriter(dataConnection.getOutputStream(), true);
			return "200 Command okay";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "503 Bad sequence of commands";
		}
	}
}