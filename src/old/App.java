package old;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public class App {

    public static void main(String[] args) {
        try {
            new App(
                    MessageDigest.getInstance("SHA-256"),
                    SecureRandom.getInstanceStrong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SessionState {
        User user;
        boolean running;

        public SessionState() {
            user = null;
            running = true;
        }
    }

    public synchronized String processMessage(SessionState state, String message, BankManager bm) {
        String[] split = message.split(" ");
        if (split.length == 0) {
            return "Message length must be greater than 0.";
        }
        String prefix = split[0];
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        switch (prefix) {
            /* Command for creating a new user. */
            case "register" -> {
                if (state.user != null) {
                    return "-You are already logged in. Log out first to register another account.";
                }
                if (args.length != 3) {
                    return "-InvalidFormat should be 'register <firstname> <lastname> <password> <repeatpassword>'.";
                }
                String firstname = args[0];
                String lastname = args[1];
                byte[] password = args[2].getBytes();

                User user = bm.register(firstname, lastname, password);

                if (user == null) {
                    return "-User '" + firstname + ' ' + lastname + "' already exists.";
                }

                state.user = user;

                return "+User '" + user.getFirstname() + ' ' + user.getLastname()
                        + "' has been created successfully.\nYour ID is " + user.getUserID() + '!';
            }


            /* Command for logging into an existing user. */
            case "login" -> {
                if (state.user != null) {
                    return "-You are already logged in. Log out first to log into another account.";
                }
                if (args.length != 2) {
                    return "-InvalidFormat should be 'login <userID> <password>'";
                }
                long id = -1;
                try {
                    id = Long.parseLong(args[0]);
                } catch (NumberFormatException e) {
                    return "-InvalidID ID must be a number.";
                }
                byte[] password = args[1].getBytes();
                User user = bm.login(id, password);
                if (user == null) {
                    return "-User '" + id + "' does not exist or has another password.";
                }

                state.user = user;

                return "+Welcome, " + user.getFirstname() + ' ' + user.getLastname() + "!";
            }


            /* Command for retrieving the accounts owned by the current user. */
            case "accounts" -> {
                if (state.user == null)
                    return "-You are not logged in.";
                Account[] accounts = bm.am.getAccounts(state.user);
                return "+Your accounts: " + Arrays.toString(accounts) + '.';
            }


            /* Command for transferring money from one account to another. */
            case "transfer" -> {
                if (state.user == null)
                    return "-You are not logged in.";
                if (args.length != 3)
                    return "-InvalidFormat should be 'transfer <amount> <from (AccountID)> <to (AccountID)>";
                long amount = 0;
                long idFrom = -1;
                long idTo = -1;
                try {
                    amount = Long.parseUnsignedLong(args[0]);
                    idFrom = Long.parseLong(args[1]);
                    idTo = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    return "-InvalidInput amount and AccountID must be positive numeric.";
                }
                Account from = null;
                for (Account account : bm.am.getAccounts(state.user)) {
                    if (account.getAccountID() == idFrom)
                        from = account;
                }
                if (from == null)
                    return "-You don't have permission to transfer money from an account you don't own.";
                Account to = bm.am.getAccount(idTo);

                if (to == null)
                    return "-The account you want to transfer money to does not exist.";

                if (from.transferTo(to, amount)) {
                    return "+Amount successfully transfered.";
                } else {
                    return "+Amount could not be transfered.";
                }
            }


            /* Command for creating a new account. */
            case "create" -> {
                if (state.user == null)
                    return "-You are not logged in.";
                if (args.length != 1)
                    return "-InvalidFormat should be 'create <password>'";

                byte[] password = args[0].getBytes();
                Account account = bm.am.createAccount(state.user, new Password(password, bm.md, bm.sr));
                return "+Your account '" + account.toString() + "' has been created successfully.";
            }


            /* Command for changing the password of the active user or of an account. */
            case "change" -> {
                if (state.user == null)
                    return "-You are not logged in.";
                if (args.length != 2)
                    return "-InvalidFormat should be 'change user/(account <id>) <password>'";
                if (args[0].equals("user")) {
                    state.user.changePassword(new Password(args[1].getBytes(), bm.md, bm.sr));
                    return "+User password changed successfully.";
                } else if (args[0].equals("account")) {
                    long id = -1;
                    try {
                        id = Long.parseLong(args[1]);
                    } catch (NumberFormatException e) {
                        return "-InvalidInput amount and AccountID must be numeric.";
                    }
                    Account account = null;
                    for (Account acc : bm.am.getAccounts(state.user)) {
                        if (acc.getAccountID() == id) {
                            account = acc;
                        }
                    }

                    if (account == null)
                        return "-This account does not exist.";

                    account.changePassword(new Password(args[1].getBytes(), bm.md, bm.sr));

                    return "+Account password changed successfully.";
                } else {
                    return "-UnknownOption must be either 'user' or 'account'.";
                }
            }


            /* Command for checking the combined balance of all accounts. */
            case "balance" -> {
                if (state.user == null)
                    return "-You are not logged in.";
                Account[] accounts = bm.am.getAccounts(state.user);
                long balance = 0;
                for (Account acc : accounts)
                    balance += acc.getBalance();
                return "+Your total balance: " + balance + '.';
            }
            case "whoami" -> {
                if (state.user == null) {
                    return "-You are currently not logged in.";
                }
                return "+You are '" + state.user.getFirstname() + ' ' + state.user.getLastname() + "'.";
            }
            case "quit" -> {
                state.running = false;
                return "+Bye.";
            }
            case "logout" -> {
                if (state.user == null) {
                    return "-You are currently not logged in.";
                }
                state.user = null;
                return "+You are now logged out.";
            }
            default -> {
                return "-UnknownCommand";
            }
        }
    }

    public App(MessageDigest md, SecureRandom sr) {
        BankManager bm = new BankManager(md, sr);
        try (ServerSocket serverSocket = new ServerSocket(6969)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    char[] buf = new char[1024];
                    SessionState state = new SessionState();
                    try (socket;
                            InputStreamReader is = new InputStreamReader(socket.getInputStream());
                            OutputStreamWriter os = new OutputStreamWriter(socket.getOutputStream())) {
                        while (state.running) {
                            int len = is.read(buf);
                            if (len < 0) {
                                state.running = false;
                                continue;
                            }
                            String output = processMessage(state, String.valueOf(buf, 0, len), bm);
                            os.write(output);
                            os.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}