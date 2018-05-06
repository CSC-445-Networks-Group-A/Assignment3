package acceptors;

/**
 * Created by Yingying Xia on 2018/5/3.
 */

/**
 * update checker for updating users
 */

import chain.User;
import common.Addresses;
import common.Ports;
import packets.acceptances.AcceptedUpdatePacket;
import packets.proposals.UpdateUsersPacket;
import packets.verifications.VerifyUpdatePacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.net.*;

/**
 * update checker for updating users
 */
public class UpdateChecker extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private User checker;

    private final int proposalPort; //for listening to update Manager (proposers)
    private final InetAddress proposalAddress;

    public UpdateChecker(User checker) throws UnknownHostException {
        super("ChainChecker: " + checker.getID());
        this.proposalPort = Ports.UPDATE_MANAGER_PROPOSAL_PORT;
        this.proposalAddress = InetAddress.getByName(Addresses.UPDATE_MANAGER_PROPOSAL_ADDRESS);
    }

    @Override
    public void run() {
        this.acceptRequests();
    }

    /**
     * accepting update requests send by update managers (proposers)
     */
    public void acceptRequests(){
        try{
            MulticastSocket multicastSocket = new MulticastSocket(proposalPort);
            multicastSocket.joinGroup(proposalAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);
            boolean done = false;
            while(!done){
                try{
                    byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(datagramPacket);
                    ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    ObjectInputStream inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof UpdateUsersPacket)) {
                        UpdateUsersPacket proposalPacket = (UpdateUsersPacket) object;
                       // BigInteger lastUpdatedBlockNumber = proposalPacket.getLastBlockRecorded();
                        //FIXME: is validation needed here?
                        VerifyUpdatePacket verifyPacket = verifyWithAllAcceptors(proposalPacket);
                        sendNotifications(verifyPacket);
                    }
                    inputStream.close();
                    bais.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    done = true;
                }
            }





        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public VerifyUpdatePacket verifyWithAllAcceptors(UpdateUsersPacket packet){
        VerifyUpdatePacket vup = null;

        /**
         * TODO
         */


        return vup;
    }


    /**
     * method to inform all learners to take action and all proposers that update has been taken place
     *
     */
    public void sendNotifications(VerifyUpdatePacket verifiedUpdatePacket){


        /**
         * inform all learners
         */
        AcceptedUpdatePacket updatePacket = new AcceptedUpdatePacket(verifiedUpdatePacket.getBlocksToUpdate(), verifiedUpdatePacket.getLastUpdatedBlockNumber());

        /**
         * inform all proposers that an update proposal has been accepted
         */
    }


}
