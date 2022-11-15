package old;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class BankManager {
    AccountManager am;
    UserManager um;

    MessageDigest md;
    SecureRandom sr;

    public BankManager(MessageDigest md, SecureRandom sr) {
        this.md = md;
        this.sr = sr;
        um = new UserManager();
        am = new AccountManager();
    }

    /**
     * Tries to log in to an existing user.
     *
     * @return the {@link User} if the {@link Password} is equal, or null if it is
     *         not
     */
    public User login(long id, byte[] password) {
        User user = um.getUser(id);
        if (user != null && user.getPassword().login(password, md, sr)) {
            return user;
        } else {
            return null;
        }
    }

    /**
     * @return the newly created {@link User}
     */
    public User register(String firstname, String lastname, byte[] password) {
        return um.createUser(firstname, lastname, password, md, sr);
    }
}
