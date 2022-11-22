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
		//write
		NEW_USER,
		DELETE_USER,
		TRANSFER,
		CHANGE_USER_PASSWORD,
//		CHANGE_ACCOUNT_PASSWORD,
		NEW_ACCOUNT,
		REMOVE_ACCOUNT,
		CONNECT_ACCOUNT,
		//read
		READ_ACCOUNTS
	}
	
	/*
	 * if(input.equals("connect account") {
	 * 	databaseThread.getAccountSaltPassword
	 * 	if(passwordCorrect)
	 * 		databaseThread.connectAccount
	 *} 
	 * 
	 */
	
	//byte[] to String -> { new String((byte[])data[n])
	
	DatabaseConnector database;
	List<Request> requests;
	PrintWriter out; //current output
	String ipClient;	//current ip address of the 
	ClientConnection client;
	
	public DatabaseThread() {
		database = new DatabaseConnector("", 0, "Bank.db", "", "");

		LinkedList<Request> list = new LinkedList<Request>();
		requests = Collections.synchronizedList(list);
	}
	
	@Override
	public void run() {
		while(true) {
			while(requests.isEmpty()) {
				Thread.onSpinWait();
			}
			//pick first request in the queue
			Request currentRequest = requests.remove(0);
			//extracting the ClientConnection from the start of data[]
			Object[] data = currentRequest.getArray();
			client = (ClientConnection) data[0];
			data = Arrays.copyOfRange(data, 1, data.length);
			
			String startMessage = "";
			switch(currentRequest.getType()) {
				case NEW_USER -> {
					//execution
					startMessage = "The creation of a new user was unsuccessful, because ";
					byte[] salt = PasswordValidator.generateSalt();
					byte[] password = PasswordValidator.generateHash((String) data[2], salt);
					executeStatement("INSERT INTO User (firstname, lastname, salt, password) VALUES (" + data[0] + "," + data[1] + "," + salt + password + ");");
					if(database.getErrorMessage() != null) 
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
					executeStatement("SELECT MAX(userID) FROM User;");
					if(database.getErrorMessage() != null) 
						write(startMessage + "there was a error while reading the ID of the new user. Please immediatly report this to an admin.", database.getErrorMessage());
					else
						write("Your userID is " + database.getCurrentQueryResult().getData()[0][0] + ". Please remember this number and your password, since your account will be unaccessible for anyone but the admins when you forget it. You are now logged in and can create an account to start you banking journey.");
				}
				case DELETE_USER -> {
					//validation
					startMessage = "The deletion of the user " + data[0] + "was unsuccessful, because ";
					if(!iDExists("User", (int) data[0])) {
						write(startMessage + "the corresponding user doesn't exist.");
						break;
					}
					//execution
					executeStatement("DELETE FROM User WHERE userID = " + data[0] + ";");
					if(database.getErrorMessage() != null) 
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
					executeStatement("DELETE FROM in_charge_of WHERE userID = " + data[0]);
					if(database.getErrorMessage() != null) 
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
				}
				case NEW_ACCOUNT -> {
					//validation
					startMessage = "The creation of a new account was unsuccessful, because ";
					if(!iDExists("User", (int) data[0])) {
						write(startMessage + "the corresponding user doesn't exist.");
						break;
					}
					
					//execution
					byte[] salt = PasswordValidator.generateSalt();
					byte[] password = PasswordValidator.generateHash((String) data[1], salt);
					executeStatement("INSERT INTO Account (balance, salt, password, accountName) VALUES (0, " + salt + ", " + password + ", " + data[2] + ");");
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
						break;
					}
					executeStatement("SELECT MAX(accountID) FROM Account;");
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while reading the database.", database.getErrorMessage());
						break;
					} 
					String accountID = database.getCurrentQueryResult().getData()[0][0];
					executeStatement("INSERT INTO in_charge_of VALUES (" + data[0] + ", " + accountID + ");");
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while associating the user with the new account.", database.getErrorMessage());
						break;
					}
					write("The creation of the new account with the accountID " + accountID + "was successful.");
				}
				case TRANSFER -> {
					try {
						//validating transfer
						executeStatement("SELECT accountID FROM Account WHERE accountID = " + data[1] + " OR " + data[2] + ";");
						startMessage = "The transfer of " + data[0] + " Euro from the account " + data[1] + " to the account " + data[2] + " was unsuccessful, because ";
						if(!iDExists("Account", (int) data[1]) && !iDExists("Account", (int) data[2])) {
							write(startMessage + "both accounts don't exist.");
							break;
						} else if(!iDExists("Account", (int) data[1])) {
							write(startMessage + "the account " + data[1] + "doesn't exists.");
							break;
						} else if(!iDExists("Account", (int) data[2])) {
							write(startMessage + "the account " + data[2] + "doesn't exists.");						
							break;
						}	
							
						executeStatement("SELECT balance FROM Account WHERE accountID = " + data[1] + ";");
						if(database.getErrorMessage() != null) {
							write(startMessage + "the balance of the sending user could not be found.", database.getErrorMessage());
							break;
						}
						int funds = Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]);
						if((funds - (int) data[0]) < 0) {
							write(startMessage + "you don't have enough funds to do that transaction from that account. You are missing " + ((int) data[0] - funds) + " Euro to make that transaction.");
							break;
						} 
						
						//executing transfer
						executeStatement("UPDATE Account SET balance = " + (Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]) - (int) data[0]) + " WHERE accountID = " + data[1] + ";");
						executeStatement("SELECT balance FROM Account WHERE accountID = " + data[2]);
						executeStatement("UPDATE Account SET balance = " + (Integer.parseInt(database.getCurrentQueryResult().getData()[0][0]) + (int) data[0]) + " WHERE accountID = " + data[2] + ";");
						if(database.getErrorMessage() != null)
							write("The transfer of " + data[0] + " from the account " + data[1] + " to the account " + data[2] + " was successful.");
						else
							write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());					
					} catch(NumberFormatException e) {
						System.out.println("Failed to read the balances while transferring money."); 
						e.printStackTrace();
					}
				}
				case CHANGE_USER_PASSWORD -> {
					startMessage = "The change of the password of the user " + data[0] + " was unsuccessful, because ";
					//validation
					if(!iDExists("User", (int) data[0])) {
						write(startMessage + "the user doesn't exist.");
						break;
					}
					//execution
					executeStatement("UPDATE User SET salt = " + new String((byte[])data[1]) + ", password = " + new String((byte[])data[2]) + " WHERE userID = " + data[0] + ";");
					if(database.getErrorMessage() != null)
						write("The password of the user " + data[0] + " was successfully changed.");
					else
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
				}
//				case CHANGE_ACCOUNT_PASSWORD -> {
//					startMessage = "The change of the password of the account " + data[0] + " was unsuccessful, because ";
//					//validation
//					if(!iDExists("Account", (int) data[0])) {
//						write(startMessage + "the account doesn't exist.");
//						break;
//					}
//					//execution
//					executeStatement("UPDATE Account SET salt = " + new String((byte[])data[1]) + ", password = " + new String((byte[])data[2]) + " WHERE accountID = " + data[0] + ";");
//					if(database.getErrorMessage() != null)
//						write("The password of the account " + data[0] + " was successfully changed.");
//					else
//						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
//					}
				case REMOVE_ACCOUNT -> {
					//validation
					startMessage = "The removal of the account " + data[0] + "was unsuccessful, because ";
					if(!iDExists("Account", (int) data[0])) {
						write(startMessage + "the account doesn't exist.");
						break;
					}
					//execution
					executeStatement("DELETE FROM in_charge_of WHERE accountID = " + data[0] + " AND userID = " + data[1] + ";");
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
						break;
					}
					executeStatement("SELECT * FROM in_charge_of WHERE accountID = " + data[0]);
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
						break;
					}
					if(database.getCurrentQueryResult().getData().length == 0) {
						executeStatement("DELETE FROM Account WHERE accountID = " + data[0] + ";");
						if(database.getErrorMessage() != null) 
							write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
							break;
					}
				}
				case CONNECT_ACCOUNT -> {
					//validation 
					startMessage = "The connection of the user " + data[1] + " to the account " + data[0] + "was unsuccessful, because ";
					if(!iDExists("Account", (int) data[0])) {
						write(startMessage + "the account doesn't exist.");
						break;
					}
					
				}
				case READ_ACCOUNTS -> {
					//validation 
					startMessage = "The reading of the accounts of the user " + data[0] + " was unsuccessful, because ";
					if(iDExists("User", (int) data[0])) {
						write(startMessage + "the user doesn't exist.");
						break;
					}
					//execution 
					executeStatement("SELECT Account.accountName, in_charge_of.accountID, Account.balance FROM in_charge_of INNER JOIN Account ON in_charge_of.accountID = Account.accountID WHERE in_charge_of.accountID = " + data[0] + " ORDER BY in_charge_of.accountID ASC;");
					if(database.getErrorMessage() != null) {
						write(startMessage + "there was a error while accessing the database.", database.getErrorMessage());
						break;
					}
					String message = "AccountID AccountName balance/n";
					String[][] result = database.getCurrentQueryResult().getData();
					for(int i = 0; i < result.length; i++) {
						for(int o = 0; o < result[i].length; o++) {
							message = message + result[i][o] + " ";
						}
						message = message + "/n";
					}
					try {
						int number = 0;
						for(int i = 0; i < result[0].length; i++) {
							number = number + Integer.parseInt(result[i][2]);
						}
						message = message + "Total: " + number;
					} catch(NumberFormatException e) {
						e.printStackTrace();
						System.out.println("Something went terribly wrong. Good luck catching that.");
					}
					write(message);
				}
			}
		}
	}

	public void newUser(ClientConnection requestThread, String firstname, String lastname, String password) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = firstname;
		temp[2] = lastname;
		temp[3] = password;
		requests.add(new Request(RequestType.NEW_USER, temp));
	}
	
	public void deleteUser(ClientConnection requestThread) { //userID
		Object[] temp = new Object[2];
		temp[0] = requestThread;
		temp[1] = requestThread.getUserID();
		requests.add(new Request(RequestType.DELETE_USER, temp));
	}
	
	public void transfer(ClientConnection requestThread, int ammount, int senderAccountID, int receiverAccountID) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = ammount;
		temp[2] = senderAccountID;
		temp[3] = receiverAccountID;
		requests.add(new Request(RequestType.TRANSFER, temp));
	}
	
	public void changeUserPassword(ClientConnection requestThread, byte[] salt, byte[] password) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = requestThread.getUserID();
		temp[2] = salt;
		temp[3] = password;
		requests.add(new Request(RequestType.CHANGE_USER_PASSWORD, temp));
	}
	
//	public void changeAccountPassword(ClientConnection requestThread, int accountID, byte[] salt, byte[] password) {
//		Object[] temp = new Object[4];
//		temp[0] = requestThread;
//		temp[1] = accountID;
//		temp[2] = salt;
//		temp[3] = password;
//		requests.add(new Request(RequestType.CHANGE_ACCOUNT_PASSWORD, temp));
//	}
	
	public void newAccount(ClientConnection requestThread, String password, String newAccountName) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = requestThread.getUserID();
		temp[2] = password;
		temp[3] = newAccountName;
		requests.add(new Request(RequestType.NEW_ACCOUNT, temp));
	}
	
	public void removeAccount(ClientConnection requestThread, int accountID) {
		Object[] temp = new Object[3];
		temp[0] = requestThread;
		temp[1] = accountID;
		temp[2] = requestThread.getUserID();
		requests.add(new Request(RequestType.REMOVE_ACCOUNT, temp));
	}
	
	public void connectAccount(ClientConnection requestThread, int accountID, byte[] password) {
		Object[] temp = new Object[4];
		temp[0] = requestThread;
		temp[1] = accountID;
		temp[2] = requestThread.getUserID();
		temp[3] = password;
		requests.add(new Request(RequestType.CONNECT_ACCOUNT, temp));
	}
	
	public void readAccounts(ClientConnection requestThread) {
		Object[] temp = new Object[2];
		temp[0] = requestThread;
		temp[1] = requestThread.getUserID();
		requests.add(new Request(RequestType.READ_ACCOUNTS, temp));
	}
	
	
	private static class Request {
		
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
	
	private void write(String message) {
		try {
			out.write(message);
			out.flush();
			log(client, message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(ClientConnection client, String message) {
		try {
			PrintWriter output = client.getOutput();
			output.write(message);
			output.flush();
			log(client, message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(String message, String sqlError) {
		try {
			PrintWriter output = client.getOutput();
			output.write(message);
			output.flush();
			log(client, message, sqlError);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
//	public void startServer() {
//		log("Server started.");
//	}
//	
//	public void endServer() {
//		log("Server shutdown.");
//	}
	
	private void log(ClientConnection client, String message) {
		executeStatement("INSERT INTO Log (date, time, ipClient, message) VALUES (" + LocalDate.now() + ", " + LocalTime.now() + ", " + client.getIPAddress() + ", " +  message + ");");
		if(database.getErrorMessage() != null) {
			System.out.println(database.getErrorMessage());
		}
	}
	
	private void log(ClientConnection client, String message, String sqlError) {
		executeStatement("INSERT INTO Log (date, time, ipClient, message, sqlError) VALUES (" + LocalDate.now() + ", " + LocalTime.now() + ", " + client.getIPAddress() + ", " +  message + ", " + sqlError + ");");
		if(database.getErrorMessage() != null) {
			System.out.println(database.getErrorMessage());
		}
	}
	
	private synchronized void executeStatement(String statement) {
		database.executeStatement(statement);
	}
	
	/**
	 * 
	 * @param table
	 * @param iD
	 * @return
	 */
	private boolean iDExists(String table, int iD)  {
		executeStatement("SELECT * FROM " + table + " WHERE " + table.toLowerCase() + "ID = " + iD + ";");
		if(database.getErrorMessage() != null) {
			System.out.println(database.getErrorMessage());
			return false;
		}
		String[][] data = database.getCurrentQueryResult().getData();
		return data.length != 0;
	}

}
