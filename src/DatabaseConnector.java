import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ClientConnection extends Thread {

    private final Socket client;
    private final DatabaseThread dbt;
    private PrintWriter writer;
    private BufferedReader reader;
    private int userID;

    public ClientConnection(Socket client, DatabaseThread dbt) {
        this.client = client;
        this.dbt = dbt;
        try {
            writer = new PrintWriter(client.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public PrintWriter getOutput() {
        return writer;
    }

    public String getIPAddress() {
        return client.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        boolean running = true;

        try (client) {
            while (running) {
                String message;
                try {
                    message = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    running = false;
                    continue;
                }

                Iterator<String> args = Arrays.stream(message.split(" ")).iterator();
                String prefix = args.next();

                try {
                    switch (prefix) {
                        case "register" -> {
                            String firstname = args.next();
                            String lastname = args.next();
                            String password = args.next();

                            dbt.newUser(this, firstname, lastname, password);
                        }
                        case "login" -> {
                            int userID;
                            try {
                                userID = Integer.parseUnsignedInt(args.next());
                            } catch (NumberFormatException e) {
                                writer.println("Expected numerical argument.");
                                continue;
                            }
                            String password = args.next();

                            PasswordValidator.generateHash(password, dbt.getUserSalt(userID));
                            byte[][] saltAndPassword = dbt.getSaltAndHash(userID);
                            boolean valid = PasswordValidator.isValid(password, saltAndPassword[0], saltAndPassword[1]);

                            if (valid) {
                                this.userID = userID;
                                writer.println("You are now logged in.");
                            } else {
                                writer.println("Wrong password.");
                            }
                            // TODO: DatabaseThread must implement a method to retrieve the userID, together with the password hash + salt.
                        }
                        case "transfer" -> {
                            int amount, senderAccountID, receiverAccountID;
                            try {
                                amount = Integer.parseUnsignedInt(args.next());
                                senderAccountID = Integer.parseUnsignedInt(args.next());
                                receiverAccountID = Integer.parseUnsignedInt(args.next());
                            } catch (NumberFormatException e) {
                                writer.println("Expected numerical argument.");
                                continue;
                            }
                            dbt.readAccounts(this);
                            dbt.transfer(this, amount, senderAccountID, receiverAccountID);
                        }
                        case "change" -> {
                            String password = args.next();
                            byte[] salt = PasswordValidator.generateSalt();

                            dbt.changeUserPassword(this, salt, PasswordValidator.generateHash(password, salt));
                        }
                        case "delete" -> {
                            dbt.deleteUser(this);
                        }
                        case "quit" -> {
                            running = false;
                            writer.println("Bye.");
                        }

                    }
                } catch (NoSuchElementException e) {
                    writer.println("Not enough arguments given.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
