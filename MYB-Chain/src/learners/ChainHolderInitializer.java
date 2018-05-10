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
    private static final String BLOCKCHAIN_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator + "UserResources" + File.separator + "BLOCKCHAIN" + File.separator + NodeType.LEARNER;
    private static final String USER_INFO_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.LEARNER;
    private static final String PRIVATE_KEY_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PRIVATE" + File.separator + NodeType.LEARNER;
    private static final String PUBLIC_KEY_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PUBLIC" + File.separator + NodeType.LEARNER;
    private static final int NUMBER_OF_CHAIN_HOLDERS= 4;

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        makeDirectories();

        User[] users = new User[NUMBER_OF_CHAIN_HOLDERS];
        ChainHolder[] chainHolders = new ChainHolder[NUMBER_OF_CHAIN_HOLDERS];

        for (int i = 0; i < NUMBER_OF_CHAIN_HOLDERS; i++) {
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
                users[i] = User.loadUser(userFileName);
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
                users[i] = new User(userFileName, privateFileName, publicFileName, blockChainFileName, "ChainHolder", randomNumber.toString(), 10000.0);
                users[i].persist();
                System.out.println(users[i].getFullName());
            /*RegistrationView regView = new RegistrationView();
            regView.setVisible(true);*/
            }

            chainHolders[i] = new ChainHolder(users[i], i);
        }

        for (int i = 0; i < NUMBER_OF_CHAIN_HOLDERS; i++) {
            chainHolders[i].start();
            users[i].start();
        }

        for (int i = 0; i < NUMBER_OF_CHAIN_HOLDERS; i++) {
            chainHolders[i].join();
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
