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
    private static final String BLOCKCHAIN_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator + "UserResources" + File.separator + "BLOCKCHAIN" + File.separator + NodeType.PROPOSER;
    private static final String USER_INFO_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.PROPOSER;
    private static final String PRIVATE_KEY_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PRIVATE" + File.separator + NodeType.PROPOSER;
    private static final String PUBLIC_KEY_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PUBLIC" + File.separator + NodeType.PROPOSER;

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        makeDirectories();
        String blockChainFileName = BLOCKCHAIN_PATH + File.separator + "BlockChain.dat";
        String userFileName = USER_INFO_PATH + File.separator + "UserInfo.dat";
        String privateFileName = PRIVATE_KEY_PATH + File.separator + "PK.dat";
        String publicFileName = PUBLIC_KEY_PATH + File.separator + "PublicKey.dat";
        File userFile = new File(userFileName);
        File privateFile = new File(privateFileName);
        File publicFile = new File(publicFileName);
        User user = null;

        if(userFile.exists() && privateFile.exists() && publicFile.exists()){
            System.out.println("Files exist");
            user = User.loadUser(userFileName);
            System.out.println(user.getFullName());
            /*User myUser = User.loadUser(userFileName);
            myUser.updateBlockChain();

            ClientUI client = new ClientUI(myUser);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            myUser.start();
            myUser.join();*/
        }else {
            System.out.println("Files do not exist");
            Random random = new Random();
            Long randomNumber = random.nextLong();
            user = new User(userFileName, privateFileName, publicFileName, blockChainFileName, "Miner", randomNumber.toString(), 10000.0);
            user.persist();
            System.out.println(user.getFullName());
            /*RegistrationView regView = new RegistrationView();
            regView.setVisible(true);*/
        }

        Miner miner = new Miner(user);
        miner.start();
        user.start();
        miner.join();
        user.join();

    }


    private static void makeDirectories() {
        File file = new File(BLOCKCHAIN_PATH);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        file = new File(USER_INFO_PATH);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        file = new File(PRIVATE_KEY_PATH);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        file = new File(PUBLIC_KEY_PATH);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
    }

}
