package proposers;

import chain.BlockChain;
import chain.Transaction;
import chain.User;
import javafx.util.Pair;
import packets.proposals.ProposalPacket;
import packets.requests.UpdateRequest;

import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Michael on 4/18/2018.
 */
public class UpdateManager extends Thread {
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private final InetAddress listenAddress;
    private final InetAddress requestAddress;
    private final int listenPort;
    private final int requestPort;
    private ConcurrentLinkedQueue<Pair<BigInteger, User>> pendingRequest;
    private ConcurrentLinkedQueue<Pair<InetAddress, User>> pendingAddresses;

    public UpdateManager(int listenPort, String listenAddress, int requestPort, String requestAddress) throws UnknownHostException {
        this.listenPort = listenPort;
        this.requestPort = requestPort;
        this.listenAddress = InetAddress.getByName(listenAddress);
        this.requestAddress = InetAddress.getByName(requestAddress);
    }


    @Override
    public void run() {
        try {
            Thread listenThread = new Thread(() -> listenForClientUpdateRequest());
            Thread updateRequestThread = new Thread(() -> {
                sendUpdateRequests();
            });

            listenThread.start();
            updateRequestThread.start();

            while (!listenThread.getState().equals(State.TERMINATED) && !updateRequestThread.getState().equals(State.TERMINATED)) {
                /*
                * SPIN
                * */
            }

            listenThread.join();
            updateRequestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    //TODO: return type?
    // adds to the pending requests, and hand to the sendUpdateRequest
    public BlockChain listenForClientUpdateRequest() {
        try {

            MulticastSocket multicastSocket = null;
            multicastSocket = new MulticastSocket(listenPort);
            multicastSocket.joinGroup(listenAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);

            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(datagramPacket);

            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            //TODO: add to the queue

            inputStream.close();
            bais.close();
            //TODO
            return null;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void sendUpdateRequests() {
        try {


            MulticastSocket multicastSocket = new MulticastSocket(requestPort);
            multicastSocket.joinGroup(this.requestAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);


            //get from the queue
            Pair<BigInteger, User> pair = pendingRequest.poll();
            UpdateRequest updateRequestPacket = new UpdateRequest(pair.getKey(), pair.getValue());
            outputStream.writeObject(updateRequestPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length);
            multicastSocket.send(datagramPacket);
            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(requestAddress);
            System.out.println("FINISHING PROPOSAL:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
