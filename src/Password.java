import java.security.MessageDigest;
import java.security.SecureRandom;
//import java.util.HexFormat;

public class Password {
    byte[] salt;
    byte[] hashedPassword;

    public Password(byte[] password, MessageDigest md, SecureRandom sr) {
        md.reset();
        salt = new byte[4];
        sr.nextBytes(salt);
        md.update(salt);
        hashedPassword = md.digest(password);
        md.reset();
    }

    public boolean login(byte[] password, MessageDigest md, SecureRandom sr) {
        md.reset();
        md.update(salt);
        byte[] hashedResult = md.digest(password);
        md.reset();
        return MessageDigest.isEqual(hashedPassword, hashedResult);
    }

//    @Override
//    public String toString() {
//        HexFormat formatter = HexFormat.of();
//        return "Password [salt=" + formatter.formatHex(salt) + ", hashedPassword=" + formatter.formatHex(hashedPassword)
//                + "]";
//    }
}
