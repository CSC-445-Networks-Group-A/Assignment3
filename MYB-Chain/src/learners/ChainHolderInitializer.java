package learners;

import chain.User;
import common.NodeType;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Michael on 5/2/2018.
 */
public class ChainHolderInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        String userFileName = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
                File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.PROPOSER + "UserInfo.dat";
        User user;
        if(User.userFileExists(userFileName)){
            user = User.loadUser(userFileName);
        }else {
            Random random = new Random();
            Long randomNumber = random.nextLong();
            user = new User(NodeType.LEARNER,"ChainHolder", randomNumber.toString(), 100.0);
        }

        Thread chainHolder = new ChainHolder(user);
        chainHolder.start();
        user.start();
        chainHolder.join();
        user.join();

    }
}
