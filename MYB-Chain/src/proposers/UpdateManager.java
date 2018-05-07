package proposers;

import javafx.util.Pair;
import packets.Packet;
import packets.proposals.UpdateUsersPacket;
import packets.requests.UpdateRequest;
import packets.responses.GeneralResponse;
import packets.responses.SuccessfulUpdate;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Michael on 4/18/2018.
 */
public class UpdateManager extends Thread {
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private final InetAddress listenAddress; //this is for listening to clients/users
    private final InetAddress requestAddress;
    private final InetAddress listenForAcceptorAddress;
    private final int listenForAcceptorPort;
    private HashMap<InetAddress, Integer> usersAddressBook; //just one ...

    //TODO: another address and port for receiving from acceptors
    private final int listenPort;
    private final int requestPort;
    private ConcurrentLinkedQueue<Pair<BigInteger, InetAddress>> pendingRequest;
    private ConcurrentLinkedQueue<Pair<InetAddress, Integer>> pendingAddresses;

    public UpdateManager(int listenPort, String listenAddress, int requestPort, String requestAddress, int listenAcceptorPort,
                         String listenAcceptorAddress) throws UnknownHostException {
        this.listenPort = listenPort;
        this.requestPort = requestPort;
        this.listenAddress = InetAddress.getByName(listenAddress);
        this.requestAddress = InetAddress.getByName(requestAddress);
        this.listenForAcceptorAddress = InetAddress.getByName(listenAcceptorAddress);
        this.listenForAcceptorPort = listenAcceptorPort;
        this.usersAddressBook = new HashMap<>();
        this.pendingRequest = new ConcurrentLinkedQueue<>();
        this.pendingAddresses = new ConcurrentLinkedQueue<>();

    }


    @Override
    public void run() {
        try {
            Thread listenThread = new Thread(() -> listenForClientUpdateRequest());
            Thread sendUpdateRequestThread = new Thread(() -> {
                sendUpdateRequestsToAcceptors();
            });
            Thread listenForAcceptorUpdateThread = new Thread(() -> {
                receiveResponseFromAcceptor();
            });

            listenThread.start();
            sendUpdateRequestThread.start();
            receiveResponseFromAcceptor();

            while (!listenThread.getState().equals(State.TERMINATED) && !sendUpdateRequestThread.getState().equals(State.TERMINATED)
                    && !listenForAcceptorUpdateThread.getState().equals(State.TERMINATED)) {
                /*
                * SPIN
                * */
            }

            listenThread.join();
            sendUpdateRequestThread.join();
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
                Pair <BigInteger, InetAddress> pair = new Pair<>(updateRequest.getLastRecordedBlock(), updateRequest.getUserAddress());
                pendingRequest.add(pair);
                //TODO:sending back a simple message to user indicating that it is updating...
                InetAddress userAddress = updateRequest.getUserAddress();
                int userPort = updateRequest.getUserPort();
                Pair<InetAddress, Integer> addressPortPair = new Pair<>(userAddress,userPort);
                pendingAddresses.add(addressPortPair);
                usersAddressBook.putIfAbsent(userAddress,userPort);
                GeneralResponse messageToUser = new GeneralResponse("Updating in progress......");
                respondToUserUpdateRequest(userAddress,userPort,messageToUser);
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


            //TODO: check when queue is empty? when pendingRequest.poll() == null?
            //get from the queue
            Pair<BigInteger, InetAddress> pair = pendingRequest.poll();
            Pair<InetAddress, Integer> userAddressPortPair = pendingAddresses.poll();
            BigInteger lastBlockRecorded = pair.getKey();
            InetAddress userAddress = pair.getValue();
            // this SHOULD BE the same as looping through the addressPort pairs and find by InetAddress
            int userPort = userAddressPortPair.getValue();
            UpdateUsersPacket updateRequestPacket = new UpdateUsersPacket(lastBlockRecorded,userAddress,userPort);
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
     * listening for acceptor responses - packet = AcceptedUpdatePacket
     * filters out so that only one gets send out to the user that requested it
     */
    public void receiveResponseFromAcceptor() {
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(listenForAcceptorPort);

            multicastSocket.joinGroup(listenForAcceptorAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);

            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);


            multicastSocket.receive(datagramPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            Object object = inputStream.readObject();
            if ((object != null) && (object instanceof SuccessfulUpdate)) {
                SuccessfulUpdate successPacket = (SuccessfulUpdate)object;

                //simply sending back a message to user that he/she is successfully updated
                //actual update occurs at learners side

                InetAddress userAddress = successPacket.getUserAddress();
                int userPort = successPacket.getUserPort();
                //filter
                if(usersAddressBook.keySet().contains(userAddress)&&usersAddressBook.values().contains(userPort)){
                    System.out.println("UPDATE MANAGER: Received a update for a user that exist in the address book...");
                    GeneralResponse messageToUser = new GeneralResponse("Update successful!");
                    respondToUserUpdateRequest(userAddress, userPort, messageToUser);
                    //remove from the address book
                    usersAddressBook.remove(userAddress,userPort);
                }
                //otherwise ignore the rest (because they are the same updates from learners)

            }
            inputStream.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * communication with user -- send back a message back to user indicating his/her request is processing
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