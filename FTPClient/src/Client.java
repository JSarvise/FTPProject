import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.net.*;

public class Client {
	
	private static final String ipServer = "localhost";
	
		public static void main(String[] args) throws IOException {
	
	        Path clientDirectory = Paths.get(".\\client_directory");  // Specify your directory path here
	        
	        var controlSocket = new Socket(ipServer, 21);
	        //var dataSocket = new Socket(ipServer,20);
        	var clientReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        	var clientWriter = new PrintWriter(controlSocket.getOutputStream(), true);
	        
	        try
	        {
	        	
	        	String clientInput = "";
	        	
	        	System.out.println(clientReader.readLine());
	        	
	        	bringMenu();
	        	
	        	while ((clientInput = clientReader.readLine()) != null) {  // Loop indefinitely, you should add a mechanism to break the loop
	            	
	        		bringMenu();
	                
	            switch (clientInput) {		
		        	case "1":  
		        		
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
	
	public static void bringMenu()
	{
		System.out.println("\r\nInput one of these numbers depending on what you want to do:");
		System.out.println("Options:");
	    System.out.println("1. Close the client connection.");
	    System.out.println("2. List all files");
	    System.out.println("3. Upload a file");
	    System.out.println("4. Download a file");
	}
}


