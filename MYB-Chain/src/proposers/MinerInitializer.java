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
    private final static int NUMBER_OF_MINERS = 1;

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        makeDirectories();

        User[] users = new User[NUMBER_OF_MINERS];
        Miner[] miners = new Miner[NUMBER_OF_MINERS];

        for (int i = 0; i < NUMBER_OF_MINERS; i++) {
            String blockChainFileName = BLOCKCHAIN_PATH + File.separator + "BlockChain_" + i + ".dat";
            String userFileName = USER_INFO_PATH + File.separator + "UserInfo_" + i + ".dat";
            String privateFileName = PRIVATE_KEY_PATH + File.separator + "PK_" + i + ".dat";
            String publicFileName = PUBLIC_KEY_PATH + File.separator + "PublicKey_" + i + ".dat";
            File userFile = new File(userFileName);
            File privateFile = new File(privateFileName);
            File publicFile = new File(publicFileName);
            users[i] = null;

            if(userFile.exists() && privateFile.exists() && publicFile.exists()){
                System.out.println("Files exist");
                users[i] = User.loadUser(userFileName, privateFileName, publicFileName, blockChainFileName);
                System.out.println(users[i].getFullName());
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
                users[i] = new User(userFileName, privateFileName, publicFileName, blockChainFileName, "Miner", randomNumber.toString(), 10000.0);
                users[i].persist();
                System.out.println(users[i].getFullName());
            /*RegistrationView regView = new RegistrationView();
            regView.setVisible(true);*/
            }

            miners[i] = new Miner(users[i]);
        }


        for (int i = 0; i < NUMBER_OF_MINERS; i++) {
            miners[i].start();
            users[i].start();
        }

        for (int i = 0; i < NUMBER_OF_MINERS; i++) {
            miners[i].join();
            users[i].join();
        }

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
