import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientConnection extends Thread {
	
	Socket client;
	OutputStreamWriter out;
	
	public ClientConnection(Socket client) {
		try {
			this.client = client;
			out = new OutputStreamWriter(client.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public OutputStreamWriter getOutput() {
		return out;
	}
	
	public String ipAdress() {
		return client.getInetAddress().getHostAddress();
	}
	
}
