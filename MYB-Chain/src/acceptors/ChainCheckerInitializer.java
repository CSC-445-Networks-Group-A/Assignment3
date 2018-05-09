package acceptors;

import chain.User;
import common.NodeType;
import proposers.Miner;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Michael on 5/2/2018.
 */
public class ChainCheckerInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        String userFileName = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
                File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.ACCEPTOR + "UserInfo.dat";
        User user;
        if(User.userFileExists(userFileName)){
            user = User.loadUser(userFileName);
        }else {
            Random random = new Random();
            Long randomNumber = random.nextLong();
            user = new User(NodeType.ACCEPTOR,"ChainChecker", randomNumber.toString(), 100.0);
        }

        Thread chainChecker = new ChainChecker(user);
        chainChecker.start();
        user.start();
        chainChecker.join();
        user.join();

    }

}
