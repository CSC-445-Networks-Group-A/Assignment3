package acceptors;

import chain.User;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Yingying Xia on 2018/5/4.
 */
public class UpdateCheckerInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {


        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            user = new User("Some", "Person", 100.0);
        }

        Thread UpdateChecker = new UpdateChecker(user);

    }
}