package learners;

import chain.Block;
import chain.User;
import common.Addresses;
import common.Ports;
import javafx.util.Pair;
import packets.acceptances.AcceptedPacket;

import packets.responses.SuccessfulUpdate;
import packets.responses.TransactionDenied;
import packets.responses.TransactionPending;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.interfaces.RSAPrivateKey;

/**
 * Created by Michael on 4/18/2018.
 */
public class ChainHolder extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int COLLISION_PREVENTING_TIMEOUT_TIME = 5000;
    private final static int MIN_COLLISION_PREVENTING_TIMEOUT_TIME = 500;
    private final static int TTL = 12;
    private final static int N = 10;
    private final static int f = (N-1)/3;
    private final User holder;
    private final RSAPrivateKey checkerPrivateKey;
    private final InetAddress learningAddress;
    private final InetAddress finalAcceptanceAddress;
    private final int learningPort;
    private final int finalAcceptancePort;


    public ChainHolder(User mybHolder) throws IOException, ClassNotFoundException {
        super("ChainHolder: " + mybHolder.getID());
        this.holder = mybHolder;
        this.checkerPrivateKey = holder.loadPrivateKeyFromFile();
        this.learningAddress = InetAddress.getByName(Addresses.HOLDER_LEARNING_ADDRESS);
        this.finalAcceptanceAddress = InetAddress.getByName(Addresses.HOLDER_CHECKING_ADDRESS);
        this.learningPort = Ports.HOLDER_LEARNING_PORT;
        this.finalAcceptancePort = Ports.HOLDER_CHECKING_PORT;
        /*this.pendingTransactions = new ConcurrentLinkedQueue<>();
        this.pendingAddresses = new ConcurrentLinkedQueue<>();*/

    }


    @Override
    public void run() {

    }


    private void listen() {
        try {
            System.out.println("STARTING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + learningPort);

            MulticastSocket multicastSocket = new MulticastSocket(learningPort);
            multicastSocket.joinGroup(learningAddress);

            boolean running = true;

            while (running) {
                byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);
                ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                ObjectInputStream inputStream = new ObjectInputStream(bais);

                Object object = inputStream.readObject();
                if ((object != null) && (object instanceof AcceptedPacket)) {
                    AcceptedPacket acceptedPacket = (AcceptedPacket) object;
                    Block block = acceptedPacket.getBlock();
                    BigInteger chainLength = acceptedPacket.getChainLength();

                    if (transaction.isVerified()) {
                        pendingTransactions.add(transaction);
                        pendingAddresses.add(new Pair<>(userAddress, userPort));
                        respondToUserRequest(userAddress, userPort, new TransactionPending("Transaction Pending..."));
                    }else {
                        respondToUserRequest(userAddress, userPort, new TransactionDenied("Transaction Denied: Insufficient Funds or Invalid Signature"));
                    }
                }else if ((object != null) && (object instanceof SuccessfulUpdate)) {



                }

                inputStream.close();
                bais.close();
            }

            multicastSocket.leaveGroup(requestAddress);
            System.out.println("FINISHING:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }



}