package client;

import chain.User;
import common.NodeType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Client {

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException {
        String userFileName = File.separator + "localhome" + File.separator + "csc445" + File.separator + "group-A" +
                File.separator +"UserResources" + File.separator + "USER_INFO" + File.separator + NodeType.CLIENT + "UserInfo.dat";
        if(User.userFileExists(userFileName)){
            User myUser = User.loadUser(userFileName);
            myUser.updateBlockChain();

            ClientUI client = new ClientUI(myUser);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            myUser.start();
            myUser.join();
        }else {
            RegistrationView regView = new RegistrationView();
            regView.setVisible(true);
        }

    }

}
