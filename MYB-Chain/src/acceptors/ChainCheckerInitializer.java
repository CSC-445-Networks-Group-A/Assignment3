package acceptors;

import chain.User;
import proposers.Miner;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Michael on 5/2/2018.
 */
public class ChainCheckerInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            user = new User("Some", "Person", 100.0);
        }

        Thread chainChecker = new ChainChecker(user);
        chainChecker.start();
        chainChecker.join();

    }

}
