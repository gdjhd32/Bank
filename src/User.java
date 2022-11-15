public class User {
    private final long userID;
    private final String firstname;
    private final String lastname;
    private Password password;

    public User(long id, String firstname, String lastname, Password password) {
        this.userID = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
    }

    public void changePassword(Password password) {
        this.password = password;
    }

    public long getUserID() {
        return userID;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Password getPassword() {
        return password;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (userID ^ (userID >>> 32));
        result = prime * result + ((firstname == null) ? 0 : firstname.hashCode());
        result = prime * result + ((lastname == null) ? 0 : lastname.hashCode());
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
        User other = (User) obj;
        if (userID != other.userID)
            return false;
        if (firstname == null) {
            if (other.firstname != null)
                return false;
        } else if (!firstname.equals(other.firstname))
            return false;
        if (lastname == null) {
            return other.lastname == null;
        } else return lastname.equals(other.lastname);
    }

    @Override
    public String toString() {
        return "User [userID=" + userID + ", firstname=" + firstname + ", lastname=" + lastname + ", password="
                + password + "]";
    }

}
