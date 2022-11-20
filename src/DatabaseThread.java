import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DatabaseThread extends Thread {
	
	//temporary main method
	public static void main(String[] args) {
		new DatabaseThread();
	}
	
	private enum RequestType {
		ADD_USER,
		TRANSFER,
		CHANGE_USER_PASSWORD,
		CHANGE_ACCOUNT_PASSWORD,
		NEW_ACCOUNT,
		REMOVE_ACCOUNT
	}
	
	//Insert/Update, 
	
	DatabaseConnector database;
	List<Request> requests;
	OutputStreamWriter out; //current output
	Socket socket;	//current socket
	
	public DatabaseThread() {
		database = new DatabaseConnector("", 0, "Bank.db", "", "");
		
		LinkedList<Request> list = new LinkedList<Request>();
		requests = Collections.synchronizedList(list);
	}
	
	@Override
	public void run() {
		while(true) {
			Request currentRequest = requests.get(0);
			requests.remove(0);
			Object[] data = currentRequest.getArray();
			out = ((ClientConnection) data[0]).getOutput();
			data = Arrays.copyOfRange(data, 1, data.length + 1);
			switch(currentRequest.getType()) {
				case ADD_USER:
					//TODO format salt and password accordingly
					database.executeStatement("INSERT INTO User (firstname, lastname, salt, password) VALUES (" + data[0] + "," + data[1] + "," + data[2] + data[3] + ");");
					//TODO send the Thread the ID of the new User
					break;
				case TRANSFER:
					try {
						//validating transfer
						database.executeStatement("SELECT accountID FROM Account WHERE accountID = " + data[1] + " OR " + data[2] + ";");
						String startMessage = "The transfer of " + data[0] + " from the account " + data[1] + " to the account " + data[2] + " was unsuccessful, because ";
						if(!iDExists("Account", (int) data[1]) && !iDExists("Account", (int) data[2])) {
							write(startMessage + "both accounts don't exist.");
						} else if(!iDExists("Account", (int) data[1])) {
							write(startMessage + "the account " + data[1] + "doesn't exists.");
						} else if(!iDExists("Account", (int) data[2])) {
							write(startMessage + "the account " + data[2] + "doesn't exists.");
						}
							
						database.executeStatement("SELECT balance FROM Account WHERE accountID = " + data[1] + ";");
						int funds = Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]);
						if((funds - (int) data[0]) < 0) {
							write(startMessage + "you don't have enough funds to do that transaction from that account. You are missing " + ((int) data[0] - funds) + " Euro to make that transaction.");
							break;
						} 
						
						//executing transfer
						database.executeStatement("UPDATE Account SET balance = " + (Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]) - (int) data[0]) + " WHERE accountID = " + data[1] + ";");
						database.executeStatement("SELECT balance FROM Account WHERE accountID = " + data[2]);
						database.executeStatement("UPDATE Account SET balance = " + (Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]) + (int) data[0]) + " WHERE accountID = " + data[2] + ";");
						if(database.getErrorMessage() != null)
							write("The transfer of " + data[0] + " from the account " + data[1] + " to the account " + data[2] + " was successful.");
						else
							write("The transfer of " + data[0] + " from the account " + data[1] + " to the account " + data[2] + " was unsuccessful. Please consult the admin for advice.");
					} catch(NumberFormatException e) {
						System.out.println("Failed to read the balances while transferring money."); 
						e.printStackTrace();
					}
					break;
				case CHANGE_ACCOUNT_PASSWORD:
					//TODO format salt and password accordingly
					database.executeStatement("UPDATE Account SET salt = " + data[1] + ", password = " + data[2] + " WHERE accountID = " + data[0] + ";");
					if(database.getErrorMessage() != null)
						write("The password of the account " + data[0] + " was successfully changed.");
					else
						write("The change of the password of the account " + data[0] + " was unsuccessful. Please consult the admin for advice.");
					break;
				case NEW_ACCOUNT:
					
					break;
				case REMOVE_ACCOUNT:
					
					break;
			}
		}
	}
	
	/**
	 * @param requestThread The thread that called the function, that that thread can request the userID of the newly created user and forward it to the user.
	 * @param firstname
	 * @param lastname
	 * @param salt
	 * @param password
	 */
	public void addUser(ClientConnection requestThread, String firstname, String lastname, byte[] salt, byte[] password) {
		Object[] temp = new Object[5];
		temp[0] = requestThread;
		temp[1] = firstname;
		temp[2] = lastname;
		temp[3] = salt;
		temp[4] = password;
		requests.add(new Request(RequestType.ADD_USER, temp));
	}
	
	public void transfer(ClientConnection requestThread, int ammount, int senderAccountID, int receiverAccountID) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = ammount;
		temp[2] = senderAccountID;
		temp[3] = receiverAccountID;
		requests.add(new Request(RequestType.TRANSFER, temp));
	}
	
	public void changeUserPassword(ClientConnection requestThread, int userID, byte[] salt, byte[] password) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = userID;
		temp[2] = salt;
		temp[3] = password;
		requests.add(new Request(RequestType.CHANGE_USER_PASSWORD, temp));
	}
	
	public void changeAccountPassword(ClientConnection requestThread, int accountID, byte[] salt, byte[] password) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = accountID;
		temp[2] = salt;
		temp[3] = password;
		requests.add(new Request(RequestType.CHANGE_ACCOUNT_PASSWORD, temp));
	}
	
	/**
	 * @param userID
	 * @param newAccountName
	 * @param requestThread The thread that called the function, that that thread can request the userID of the newly created user and forward it to the user.
	 */
	public void newAccount(ClientConnection requestThread, int userID, String newAccountName) {
		Object[] temp = new Object[3];
		temp[0] = requestThread;
		temp[1] = userID;
		temp[2] = newAccountName;
		requests.add(new Request(RequestType.NEW_ACCOUNT, temp));
	}
	
	public void removeAccount(ClientConnection requestThread, int accountID) {
		Object[] temp = new Object[2];
		temp[0] = requestThread;
		temp[1] = accountID;
		requests.add(new Request(RequestType.REMOVE_ACCOUNT, temp));
	}
	
	private class Request {
		
		private RequestType type;
		private Object[] array;
		
		private Request(RequestType type, Object[] array) {
			this.type = type;
			this.array = array;
		}
		
		public RequestType getType() {
			return type;
		}
		
		public Object[] getArray() {
			return array;
		}
		
	}
	
	public void write(String message) {
		try {
			out.write(message);
			out.flush();
			log(message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startServer() {
		log("Server started.");
	}
	
	public void endServer() {
		log("Server shutdown.");
	}
	
	private void log(String message) {
		database.executeStatement("INSERT INTO Log (date, time, ipClient,message) VALUES (" + LocalDate.now() + ", " + LocalTime.now() + ", " + message + ");");
	}
	
	/**
	 * 
	 * @param table
	 * @param iD
	 * @return
	 */
	private boolean iDExists(String table, int iD)  {
		database.executeStatement("SELECT * FROM " + table + " WHERE " + table.toLowerCase() + "ID = " + iD + ";");
		String[][] data = database.getCurrentQueryResult().getData();
		return data.length != 0;
	}

}
