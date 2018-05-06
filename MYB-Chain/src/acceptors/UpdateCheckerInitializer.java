package acceptors;

import chain.User;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Yingying Xia on 2018/5/4.
 */
public class UpdateCheckerInitializer {

    public static void main(String[] args) throws UnknownHostException, NoSuchAlgorithmException {


        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            user = new User("Some", "Person", 100.0);
        }

        Thread UpdateChecker = new UpdateChecker(user);

    }
}
