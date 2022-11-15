import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserManager {
    final Map<Long, User> users;
    AtomicLong id;

    public UserManager() {
        users = new HashMap<>();
        id = new AtomicLong();
    }

    public User getUser(long userID) {
        return users.get(userID);
    }

    public User createUser(String firstname, String lastname, byte[] password, MessageDigest md, SecureRandom sr) {
        Password pw = new Password(password, md, sr);
        User user = new User(id.incrementAndGet(), firstname, lastname, pw);
        users.put(user.getUserID(), user);
        return user;
    }
}
