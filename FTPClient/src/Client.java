import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	private static final String ipServer = "localhost";
	
	Path clientDirectory = Paths.get(".\\client_directory");  // Specify your directory path here
    
    public Socket controlSocket;
    public ServerSocket dataSocket;
    public BufferedReader commandClientReader;
    public PrintWriter commandClientWriter;
    public BufferedReader dataClientReader;
    
    public Client() throws IOException {
    	try {
			controlSocket = new Socket(ipServer, 21);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	dataSocket = new ServerSocket(0);
    	commandClientReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
    	commandClientWriter = new PrintWriter(controlSocket.getOutputStream(), true);
    }
	
		public static void main(String[] args) throws IOException {
       
	        try
	        {
	        	Client client = new Client();
	        	
	        	String clientInput = "";

	        	System.out.println(client.commandClientReader.readLine());
	        	
	        	Scanner sc = new Scanner(System.in);

	        	while (true) {  // Loop indefinitely, you should add a mechanism to break the loop
	            	
	        		client.bringMenu();
	                clientInput = sc.nextLine();
	                
		            switch (clientInput) {		
			        	case "1":  //List
			        		Socket listSocket = client.createDataConnection();
			        		System.out.println("list 1");
			        		client.dataClientReader = new BufferedReader(new InputStreamReader(listSocket.getInputStream()));
			        		System.out.println("list 2");
			        		String listOutput = client.dataClientReader.readLine();
			        		System.out.println("list 3");
			        		while(listOutput != null) {
			        			System.out.println(listOutput);
			        			listOutput = client.dataClientReader.readLine();
			        		}
			        		
		                    break;
		                case "2":
		                    
		                    break;
		                case "3":
		                    
		                    break;
		                    
		                case "4":
		                    
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
	
	private void bringMenu()
	{
		System.out.println("\r\nInput one of these numbers depending on what you want to do:");
		System.out.println("Options:");
	    System.out.println("1. Close the client connection.");
	    System.out.println("2. List all files");
	    System.out.println("3. Upload a file");
	    System.out.println("4. Download a file");
	}
	
	public String makePort(int portNumber){
		
		String ip = "127,0,0,1";
		int p1 = portNumber/256;
		int p2 = portNumber%256;
		String ipFix = ip + "," + p1 + "," + p2;
		System.out.println(ipFix);
		return ipFix;
	}
	
	public Socket createDataConnection() {
		System.out.println("Punto de control 1");
		commandClientWriter.println("PORT " + makePort(dataSocket.getLocalPort()));
		System.out.println("Punto de control 2");
		Socket socket = null;
		try {
			socket = dataSocket.accept();
			System.out.println("Punto de control 3");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return socket;
	}
}
