import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DatabaseThread extends Thread {
	
	//temporary main method
	public static void main(String[] args) {
		new DatabaseThread();
	}
	
	private enum RequestType {
		ADDUSER,
		TRANSFER,
		CHANGEPASSWORD,
		NEWACCOUNT,
		REMOVEACCOUNT
	}
	
	//Insert/Update, 
	
	DatabaseConnector database;
	List<Request> requests;
	
	public DatabaseThread() {
		database = new DatabaseConnector("", 0, "Bank.db", "", "");
		
		LinkedList<Request> list = new LinkedList<Request>();
		requests = Collections.synchronizedList(list);
	}
	
	@Override
	public void run() {
		while(true) {
			//process the requests
		}
	}
	
	/**
	 * @param firstname
	 * @param lastname
	 * @param salt
	 * @param password
	 * @param requestThread The thread that called the function, that that thread can request the userID of the newly created user and forward it to the user.
	 */
	public void addUser(String firstname, String lastname, byte[] salt, byte[] password, Thread requestThread) {
		Object[] temp = new Object[4];
		temp[0] = firstname;
		temp[1] = lastname;
		temp[2] = salt;
		temp[3] = password;
		requests.add(new Request(RequestType.ADDUSER, temp));
	}
	
	public void transfer(int ammount, int senderAccountID, int receiverAccountID) {
		Object[] temp = new Object[3];
		temp[0] = ammount;
		temp[1] = senderAccountID;
		temp[2] = receiverAccountID;
		requests.add(new Request(RequestType.TRANSFER, temp));
	}
	
	public void changePassword(int userID, byte[] salt, byte[] password) {
		Object[] temp = new Object[3];
		temp[0] = userID;
		temp[1] = salt;
		temp[2] = password;
		requests.add(new Request(RequestType.CHANGEPASSWORD, temp));
	}
	
	/**
	 * @param userID
	 * @param newAccountName
	 * @param requestThread The thread that called the function, that that thread can request the userID of the newly created user and forward it to the user.
	 */
	public void newAccount(int userID, String newAccountName, Thread requestThread) {
		Object[] temp = new Object[2];
		temp[0] = userID;
		temp[1] = newAccountName;
		requests.add(new Request(RequestType.NEWACCOUNT, temp));
	}
	
	public void removeAccount(int accountID) {
		Object[] temp = new Object[1];
		temp[0] = accountID;
		requests.add(new Request(RequestType.REMOVEACCOUNT, temp));
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

}
