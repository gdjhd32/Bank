import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class AccountManager {
    Map<User, Set<Account>> accounts;
    AtomicLong id;

    public AccountManager() {
        accounts = new HashMap<>();
        id = new AtomicLong();
    }

    public Account createAccount(User user, Password password) {
        Account account = new Account(0, password, id.incrementAndGet());
        Set<Account> set = accounts.computeIfAbsent(user,
                (_user) -> new HashSet<>());
        set.add(account);
        return account;
    }

    public Account getAccount(long id) {
        for (Set<Account> set : accounts.values()) {
            for (Account acc : set) {
                if (acc.getAccountID() == id) {
                    return acc;
                }
            }
        }
        return null;
    }

    public Account[] getAccounts(User user) {
        Set<Account> set = accounts.get(user);
        if (set == null || set.size() == 0)
            return new Account[] {};
        Account[] arr = new Account[set.size()];
        int i = 0;
        for (Account acc : set)
            arr[i++] = acc;
        return arr;
    }
}
