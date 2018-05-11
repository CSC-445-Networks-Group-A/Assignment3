package client;

import chain.User;
import common.NodeType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Client {
    private static final String BLOCKCHAIN_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator + "UserResources" + File.separator + "BLOCKCHAIN" + File.separator + NodeType.CLIENT;
    private static final String USER_INFO_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.CLIENT;
    private static final String PRIVATE_KEY_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PRIVATE" + File.separator + NodeType.CLIENT;
    private static final String PUBLIC_KEY_PATH = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
            File.separator +"UserResources" + File.separator + "PUBLIC" + File.separator + NodeType.CLIENT;


    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
        makeDirectories();
        String blockChainFileName = BLOCKCHAIN_PATH + File.separator + "BlockChain.dat";
        String userFileName = USER_INFO_PATH + File.separator + "UserInfo.dat";
        String privateFileName = PRIVATE_KEY_PATH + File.separator + "PK.dat";
        String publicFileName = PUBLIC_KEY_PATH + File.separator + "PublicKey.dat";
        File userFile = new File(userFileName);
        File privateFile = new File(privateFileName);
        File publicFile = new File(publicFileName);

        if(userFile.exists() && privateFile.exists() && publicFile.exists()){
            System.out.println("Files exist");
            User user = User.loadUser(userFileName, privateFileName, publicFileName, blockChainFileName);
            System.out.println(user.getFullName());
            user.updateBlockChain();

            ClientUI client = new ClientUI(user);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            user.start();
            user.join();
        }else {
            System.out.println("Files do not exist");
            RegistrationView regView = new RegistrationView(userFileName, privateFileName, publicFileName, blockChainFileName);
            regView.setVisible(true);
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
