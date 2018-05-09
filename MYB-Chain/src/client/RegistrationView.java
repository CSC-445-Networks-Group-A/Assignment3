/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import chain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author admin
 */
public class RegistrationView extends javax.swing.JFrame {

    private User myUser;

    /**
     * Creates new form RegistrationView
     */
    public RegistrationView() {
        initComponents();
        this.btnDone.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        RegistrationPanel = new javax.swing.JPanel();
        btnRegister = new javax.swing.JButton();
        tbxFirstname = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        tbxLastname = new javax.swing.JTextField();
        lblMessage = new javax.swing.JLabel();
        tbxUserId = new javax.swing.JTextField();
        btnDone = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Registration");
        setResizable(false);

        RegistrationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Create Account"));

        btnRegister.setText("Register");
        btnRegister.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegisterActionPerformed(evt);
            }
        });

        tbxFirstname.setToolTipText("Account ID");

        jLabel2.setText("Firstname:");

        jLabel1.setText("Lastname:");

        lblMessage.setText("");

        tbxUserId.setEditable(false);

        btnDone.setText("Done");
        btnDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    btnDoneActionPerformed(evt);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        jLabel4.setText("User ID:");

        javax.swing.GroupLayout RegistrationPanelLayout = new javax.swing.GroupLayout(RegistrationPanel);
        RegistrationPanel.setLayout(RegistrationPanelLayout);
        RegistrationPanelLayout.setHorizontalGroup(
            RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RegistrationPanelLayout.createSequentialGroup()
                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(RegistrationPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tbxUserId, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE))
                    .addGroup(RegistrationPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnDone)))
                .addContainerGap())
            .addGroup(RegistrationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RegistrationPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnRegister, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RegistrationPanelLayout.createSequentialGroup()
                                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(tbxFirstname)
                                    .addComponent(tbxLastname, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RegistrationPanelLayout.createSequentialGroup()
                                .addComponent(lblMessage)
                                .addContainerGap())))
                    .addGroup(RegistrationPanelLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        RegistrationPanelLayout.setVerticalGroup(
            RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegistrationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(tbxFirstname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegistrationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(tbxLastname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRegister)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbxUserId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37)
                .addComponent(btnDone)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(RegistrationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(RegistrationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegisterActionPerformed
        try {
            myUser = new User(this.getFirstname(), this.getLastname(), 0.0);
            try {
                myUser.commitUser();
                lblMessage.setText("Registration Successful.");
                tbxUserId.setText(myUser.getID());
                btnDone.setEnabled(true);
                btnRegister.setEnabled(false);
                tbxFirstname.setEnabled(false);
                tbxLastname.setEnabled(false);
            } catch (IOException e) {
                lblMessage.setText("Registration Failed!");
                e.printStackTrace();
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            lblMessage.setText("Registration Failed!");
            e.printStackTrace();
        }

    }//GEN-LAST:event_btnRegisterActionPerformed

    private void btnDoneActionPerformed(java.awt.event.ActionEvent evt) throws IOException, InterruptedException {//GEN-FIRST:event_btnDoneActionPerformed
        this.setVisible(false);
        ClientUI client = new ClientUI(myUser);
        client.setVisible(true);
        client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }//GEN-LAST:event_btnDoneActionPerformed

    public String getFirstname(){
        return this.tbxFirstname.getText();
    }

    public String getLastname(){
        return this.tbxLastname.getText();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel RegistrationPanel;
    private javax.swing.JButton btnDone;
    private javax.swing.JButton btnRegister;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextField tbxUserId;
    private javax.swing.JTextField tbxFirstname;
    private javax.swing.JTextField tbxLastname;
    // End of variables declaration//GEN-END:variables

}
