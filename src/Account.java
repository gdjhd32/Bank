public class Account {
    private final long accountID;
    private Password password;
    private long balance;

    public Account(long balance, Password password, long accountID) {
        this.balance = balance;
        this.password = password;
        this.accountID = accountID;
    }

    public long getBalance() {
        return balance;
    }

    public long getAccountID() {
        return accountID;
    }

    public Password getPassword() {
        return password;
    }

    public boolean transferTo(Account to, long amount) {
        if (amount > this.balance) {
            return false;
        }
        long newBalance = this.balance - amount;
        if (newBalance > this.balance)
            return false;
        long toNewBalance = to.balance + amount;
        if (toNewBalance < to.balance)
            return false;

        this.balance = newBalance;
        to.balance = toNewBalance;
        
        return true;
    }

    public void changePassword(Password password) {
        this.password = password;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (accountID ^ (accountID >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Account other = (Account) obj;
        return accountID == other.accountID;
    }

    @Override
    public String toString() {
        return "Account [accountID=" + accountID + ", balance=" + balance + "]";
    }
}
