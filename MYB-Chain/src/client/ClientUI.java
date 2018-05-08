/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import chain.User;

/**
 *
 * @author admin
 */
public class ClientUI extends javax.swing.JFrame {

    private User myUser;
    /**
     * Creates new form ClientUI
     */
    public ClientUI(User user) {
        initComponents();
        this.myUser = user;
        this.tbxUsername.setText(user.getID());
        this.tbxUsername.setEditable(false);
        this.lblAccountValue.setText(String.valueOf(user.getNetWorth()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ClientPanel = new javax.swing.JPanel();
        tbxUsername = new javax.swing.JTextField();
        lblAccountValue = new javax.swing.JLabel();
        TransactionPanel = new javax.swing.JPanel();
        accountlabel = new javax.swing.JLabel();
        tbxSentToAccount = new javax.swing.JTextField();
        valuelabel = new javax.swing.JLabel();
        tbxSendValue = new javax.swing.JTextField();
        MYBlabel = new javax.swing.JLabel();
        btnSend = new javax.swing.JButton();
        lblMessage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MYB Wallet");
        setResizable(false);

        ClientPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("User Info:"));

        tbxUsername.setText("USERNAME");

        lblAccountValue.setText("ACCTVALUEMYB");

        lblMessage.setText(" ");

        javax.swing.GroupLayout ClientPanelLayout = new javax.swing.GroupLayout(ClientPanel);
        ClientPanel.setLayout(ClientPanelLayout);
        ClientPanelLayout.setHorizontalGroup(
            ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ClientPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tbxUsername)
                    .addComponent(lblAccountValue))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ClientPanelLayout.setVerticalGroup(
            ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ClientPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbxUsername)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAccountValue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tbxUsername.getAccessibleContext().setAccessibleName("lblUsername");
        lblAccountValue.getAccessibleContext().setAccessibleName("lblAccountVal");

        TransactionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Send MYB:"));

        accountlabel.setText("Account:");

        tbxSentToAccount.setToolTipText("");

        valuelabel.setText("Value:");

        MYBlabel.setText("MYB");

        btnSend.setText("Send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        lblMessage.setAutoscrolls(true);
        lblMessage.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        javax.swing.GroupLayout TransactionPanelLayout = new javax.swing.GroupLayout(TransactionPanel);
        TransactionPanel.setLayout(TransactionPanelLayout);
        TransactionPanelLayout.setHorizontalGroup(
            TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TransactionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSend, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(TransactionPanelLayout.createSequentialGroup()
                        .addGroup(TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tbxSentToAccount)
                            .addGroup(TransactionPanelLayout.createSequentialGroup()
                                .addGroup(TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(accountlabel)
                                    .addComponent(valuelabel)
                                    .addGroup(TransactionPanelLayout.createSequentialGroup()
                                        .addComponent(tbxSendValue, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(MYBlabel)))
                                .addGap(0, 135, Short.MAX_VALUE)))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TransactionPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblMessage)
                .addContainerGap())
        );
        TransactionPanelLayout.setVerticalGroup(
            TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TransactionPanelLayout.createSequentialGroup()
                .addComponent(accountlabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbxSentToAccount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valuelabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbxSendValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(MYBlabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSend)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMessage)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tbxSentToAccount.getAccessibleContext().setAccessibleName("tbxSendAccount");
        tbxSendValue.getAccessibleContext().setAccessibleName("tbxSendValue");
        btnSend.getAccessibleContext().setAccessibleName("btnSend");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ClientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(TransactionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(ClientPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TransactionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        btnSend.setEnabled(false);
        tbxSendValue.setEditable(false);
        tbxSentToAccount.setEditable(false);

        lblMessage.setText("Initiating Transaction...");

        try {
            Double transactionAmount = Double.parseDouble(tbxSendValue.getText());

            boolean transactionRequestSuccess = myUser.makeTransaction(myUser, transactionAmount); // To-Do: NULL USER, NEED TO FIGURE HOW TO SEND USER OBJECT ONLY KNOWING USERNAME
            lblMessage.setText("Transaction Sent.");
            if(transactionRequestSuccess){
                String transactionResponse =  myUser.getTransationResponseMessage();
                lblMessage.setText(transactionResponse);
            }else{
                lblMessage.setText("Transaction Error!");
            }

        }catch(NumberFormatException e){
            lblMessage.setText("Invalid Amount");
            e.printStackTrace();
        }

        btnSend.setEnabled(true);
        tbxSendValue.setEditable(true);
        tbxSentToAccount.setEditable(true);
        
    }




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ClientPanel;
    private javax.swing.JLabel MYBlabel;
    private javax.swing.JPanel TransactionPanel;
    private javax.swing.JLabel accountlabel;
    private javax.swing.JButton btnSend;
    private javax.swing.JLabel lblAccountValue;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JTextField tbxUsername;
    private javax.swing.JTextField tbxSendValue;
    private javax.swing.JTextField tbxSentToAccount;
    private javax.swing.JLabel valuelabel;
    // End of variables declaration//GEN-END:variables
}
