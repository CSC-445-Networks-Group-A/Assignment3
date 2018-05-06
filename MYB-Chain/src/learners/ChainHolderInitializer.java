package learners;

import chain.User;
import proposers.Miner;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Michael on 5/2/2018.
 */
public class ChainHolderInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            user = new User("Some", "Person", 100.0);
        }

        Thread chainHolder = new ChainHolder(user);
        chainHolder.start();
        chainHolder.join();

    }
}
