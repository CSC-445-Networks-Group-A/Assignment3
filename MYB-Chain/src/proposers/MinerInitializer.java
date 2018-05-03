package proposers;

import chain.User;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Michael on 5/2/2018.
 */
public class MinerInitializer {

    public static void main(String[] args) throws UnknownHostException, NoSuchAlgorithmException, InterruptedException {
        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            user = new User("Some", "Person", 100.0);
        }

        Thread miner = new Miner(user);
        miner.start();
        miner.join();

    }
}
