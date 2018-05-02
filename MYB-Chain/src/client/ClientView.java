
package client;

import chain.Transaction;
import chain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author admin
 */
public class ClientView extends javax.swing.JFrame {

    private static User buyer = null;
    private static User seller = null;

    /**
     * Creates new form ClientView
     */
    public ClientView() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {

        ClientPanel = new javax.swing.JPanel();
        lblUsername = new javax.swing.JLabel();
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
        setResizable(false);

        lblUsername.setText("USERNAME");

        lblAccountValue.setText("ACCTVALUEMYB");

        javax.swing.GroupLayout ClientPanelLayout = new javax.swing.GroupLayout(ClientPanel);
        ClientPanel.setLayout(ClientPanelLayout);
        ClientPanelLayout.setHorizontalGroup(
                ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ClientPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblUsername)
                                        .addComponent(lblAccountValue))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ClientPanelLayout.setVerticalGroup(
                ClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ClientPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblUsername)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblAccountValue)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblUsername.getAccessibleContext().setAccessibleName("lblUsername");
        lblAccountValue.getAccessibleContext().setAccessibleName("lblAccountVal");

        TransactionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Send MYB"));

        accountlabel.setText("Account:");

        tbxSentToAccount.setToolTipText("");

        valuelabel.setText("Value:");

        MYBlabel.setText("MYB");

        btnSend.setText("Send");
        btnSend.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSendMouseClicked(evt);
            }
        });

        lblMessage.setText("msg");

        javax.swing.GroupLayout TransactionPanelLayout = new javax.swing.GroupLayout(TransactionPanel);
        TransactionPanel.setLayout(TransactionPanelLayout);
        TransactionPanelLayout.setHorizontalGroup(
                TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(TransactionPanelLayout.createSequentialGroup()
                                .addContainerGap()
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
                                .addContainerGap())
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TransactionPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(lblMessage)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSend))
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
                                .addGroup(TransactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSend)
                                        .addComponent(lblMessage)))
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
                                .addComponent(TransactionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 111, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnSendMouseClicked(java.awt.event.MouseEvent evt) {
        System.out.println("Transaction");
        Double amount = Double.parseDouble(tbxSendValue.getText());
        try {
            Transaction transaction = buyer.makeTransaction(seller, amount);
            System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(transaction.getSignature()));
            System.out.println(transaction.getBuyerID());
            lblUsername.setText(buyer.getID());
            lblMessage.setText("Transaction Complete");
        } catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException | java.security.InvalidParameterException e) {
            lblMessage.setText(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        //DEMO CODE
        String firstName_Person1 = "Person";
        String lastName_Person1 = "One";

        String firstName_Person2 = "Person";
        String lastName_Person2 = "Two";

        Double initialNetWorth = 20000.00;

//        try {
//            buyer = new User(firstName_Person1, lastName_Person1, initialNetWorth);
//            seller = new User(firstName_Person2, lastName_Person2, initialNetWorth);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
        //END DEMO CODE


        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify
    private javax.swing.JPanel ClientPanel;
    private javax.swing.JLabel MYBlabel;
    private javax.swing.JPanel TransactionPanel;
    private javax.swing.JLabel accountlabel;
    private javax.swing.JButton btnSend;
    private javax.swing.JLabel lblAccountValue;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JTextField tbxSendValue;
    private javax.swing.JTextField tbxSentToAccount;
    private javax.swing.JLabel valuelabel;
    // End of variables declaration
}
