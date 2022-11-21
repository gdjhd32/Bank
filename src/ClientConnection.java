import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientConnection extends Thread {
	
	private Socket client;
	private OutputStreamWriter out;
	private int userID;
	
	public ClientConnection(Socket client) {
		try {
			this.client = client;
			out = new OutputStreamWriter(client.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setUserID(int userID) {
		this.userID = userID;
	}
	
	public int getUserID() {
		return userID;
	}
	
	public OutputStreamWriter getOutput() {
		return out;
	}
	
	public String getIPAdress() {
		return client.getInetAddress().getHostAddress();
	}
	
	public void confirmLogin(boolean confirmation) {
		//TODO implement login
	}
	
}
