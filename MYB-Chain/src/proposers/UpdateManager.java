package proposers;

import chain.BlockChain;
import chain.Transaction;
import chain.User;
import javafx.util.Pair;
import packets.Packet;
import packets.proposals.ProposalPacket;
import packets.proposals.UpdateUsersPacket;
import packets.requests.UpdateRequest;
import packets.responses.GeneralResponse;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
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
    //TODO: another address and port for receiving from acceptors
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
                sendUpdateRequestsToAcceptors();
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


    // adds to the pending requests, and hand to the sendUpdateRequest
    public void listenForClientUpdateRequest() {
        try {
            System.out.println("UPDATE MANAGER: Starting " + Thread.currentThread().getName() +
                    " to listen for clients update requests");
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

            Object object = inputStream.readObject();
            if ((object != null) && (object instanceof UpdateRequest)) {

                UpdateRequest updateRequest = (UpdateRequest) object;
                Pair <BigInteger, User> pair = new Pair<>(updateRequest.getLastRecordedBlock(), updateRequest.getUser());
                pendingRequest.add(pair);
                //TODO: maybe? sending back a simple message to user indicating that it is updating...?
                InetAddress userAddress = updateRequest.getUser().getReceiveUpdateAddress();
                int userPort = updateRequest.getUser().getReceiveUpdatePort();
                GeneralResponse messageToUser = new GeneralResponse("Updating in progress......");
                //TODO: add to pending address? not sure if it is needed this point
            }
            inputStream.close();
            bais.close();
            //TODO

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }



    public void sendUpdateRequestsToAcceptors() {
        //no agreement
        //take whoever accepts first
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

    /**
     *  tcp communication  -- send back a message back to user indicating his/her request is processing
     * @param userAddress
     * @param userPort
     * @param response
     */
    private void respondToUserUpdateRequest(InetAddress userAddress, int userPort, Packet response) {
        try {
            System.out.println("UPDATE MANAGER: Responding to user request at" + userAddress + " at "+ userPort);
            Socket socket = new Socket(userAddress,userPort);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

            outputStream.writeObject(response);
            outputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
