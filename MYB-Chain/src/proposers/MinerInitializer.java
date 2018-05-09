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
        String userFileName = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
                File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.PROPOSER + "UserInfo.dat";
        User user;
        if(User.userFileExists(userFileName)){
            user = User.loadUser(userFileName);
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
