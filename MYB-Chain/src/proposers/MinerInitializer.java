package proposers;

import chain.User;
import common.NodeType;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Michael on 5/2/2018.
 */
public class MinerInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        User user;
        if(User.userFileExists()){
            user = User.loadUser();
        }else {
            Random random = new Random();
            Long randomNumber = random.nextLong();
            user = new User(NodeType.PROPOSER,"Miner", randomNumber.toString(), 100.0);
        }

        Thread miner = new Miner(user);
        miner.start();
        user.start();
        miner.join();
        user.join();

    }
}
