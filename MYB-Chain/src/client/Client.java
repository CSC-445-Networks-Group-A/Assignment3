package client;

import chain.User;

import javax.swing.*;

public class Client {

    public static void main(String args[]){

        if(User.userFileExists()){
            User myUser = User.loadUser();
//            myUser.updateBlockChain();
            ClientUI client = new ClientUI(myUser);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }else {
            RegistrationView regView = new RegistrationView();
            regView.setVisible(true);
        }

    }

}
