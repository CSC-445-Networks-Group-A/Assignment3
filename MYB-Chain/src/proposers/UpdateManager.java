package proposers;

import common.Addresses;
import common.Ports;
import packets.Packet;
import packets.acceptances.AcceptedUpdatePacket;
import packets.proposals.UpdateUsersPacket;
import packets.requests.UpdateRequest;
import packets.responses.GeneralResponse;

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
    private final InetAddress listenForLearnersAddress;
    private HashMap<InetAddress, Integer> usersAddressBook; //just one ...

    //TODO: another address and port for receiving from acceptors
    private final int listenPort;
    private final int requestPort;
    private final int listenForLearnersPort;
    private ConcurrentLinkedQueue<UpdateRequest> pendingRequests;


    // TODO: clear out the ports and address arguments
    public UpdateManager() throws UnknownHostException {
        this.listenPort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.requestPort = Ports.HOLDER_UPDATING_PORT;
        this.listenForLearnersPort = Ports.HOLDER_UPDATING_PORT;

        this.listenAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.requestAddress = InetAddress.getByName(Addresses.HOLDER_UPDATING_ADDRESS);
        this.listenForLearnersAddress = InetAddress.getByName(Addresses.HOLDER_UPDATING_ADDRESS);

        this.usersAddressBook = new HashMap<>();
        this.pendingRequests = new ConcurrentLinkedQueue<>();

    }


    @Override
    public void run() {
        try {
            Thread listenThread = new Thread(() -> listenForClientUpdateRequests());
            Thread sendUpdateRequestThread = new Thread(() -> {
                sendUpdateRequestsToLearners();
            });
            Thread listenForAcceptorUpdateThread = new Thread(() -> {
                receiveResponsesFromLearners();
            });

            listenThread.start();
            sendUpdateRequestThread.start();
            listenForAcceptorUpdateThread.start();

            while (!listenThread.getState().equals(State.TERMINATED) && !sendUpdateRequestThread.getState().equals(State.TERMINATED)
                    && !listenForAcceptorUpdateThread.getState().equals(State.TERMINATED)) {
                /*
                * SPIN
                * */
            }

            listenThread.join();
            sendUpdateRequestThread.join();
            listenForAcceptorUpdateThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    // adds to the pending requests, and hand to the sendUpdateRequest
    public void listenForClientUpdateRequests() {
        while (true) {
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
                    pendingRequests.add(updateRequest);
                    usersAddressBook.putIfAbsent(updateRequest.getUserAddress(), updateRequest.getUserPort());
                    /*GeneralResponse messageToUser = new GeneralResponse("Updating in progress......");
                    respondToUserUpdateRequest(updateRequest.getUserAddress(),updateRequest.getUserPort(),messageToUser);*/
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
    }



    public void sendUpdateRequestsToLearners() {
        //no agreement
        //take whoever accepts first
        try {
            MulticastSocket multicastSocket = new MulticastSocket(requestPort);
            multicastSocket.joinGroup(this.requestAddress);
            multicastSocket.setTimeToLive(TTL);
            boolean running = true;

            while (running) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(baos);


                //TODO: check when queue is empty? when pendingRequest.poll() == null?
                //get from the queue
                UpdateRequest updateRequest = pendingRequests.poll();
                if (updateRequest != null) {
                    BigInteger lastBlockRecorded = updateRequest.getLastRecordedBlock();
                    InetAddress userAddress = updateRequest.getUserAddress();
                    // this SHOULD BE the same as looping through the addressPort pairs and find by InetAddress
                    int userPort = updateRequest.getUserPort();
                    UpdateUsersPacket updateRequestPacket = new UpdateUsersPacket(lastBlockRecorded,userAddress,userPort);
                    outputStream.writeObject(updateRequestPacket);
                    byte[] output = baos.toByteArray();
                    DatagramPacket datagramPacket = new DatagramPacket(output, output.length, requestAddress, requestPort);
                    multicastSocket.send(datagramPacket);
                    outputStream.close();
                    baos.close();
                }
            }

            multicastSocket.leaveGroup(requestAddress);
            System.out.println("FINISHING PROPOSING:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void receiveResponsesFromLearners() {
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(listenForLearnersPort);

            multicastSocket.joinGroup(listenForLearnersAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);
            boolean running = true;

            while (running) {
                byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);

                ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                ObjectInputStream inputStream = new ObjectInputStream(bais);

                Object object = inputStream.readObject();
                if ((object != null) && (object instanceof AcceptedUpdatePacket)) {
                    AcceptedUpdatePacket acceptedUpdatePacket = (AcceptedUpdatePacket)object;

                    //simply sending back a message to user that he/she is successfully updated
                    //actual update occurs at learners side
                    InetAddress userAddress = acceptedUpdatePacket.getAddress();
                    int userPort = acceptedUpdatePacket.getPort();
                    //filter
                    if(usersAddressBook.keySet().contains(userAddress)&&usersAddressBook.values().contains(userPort)){
                        System.out.println("UPDATE MANAGER: Received a update for a user that exist in the address book...");
                        respondToUserUpdateRequest(userAddress, userPort, acceptedUpdatePacket);
                        //remove from the address book
                        usersAddressBook.remove(userAddress,userPort);
                    }
                    //otherwise ignore the rest (because they are the same updates from learners)

                }
                inputStream.close();
                bais.close();
            }

            multicastSocket.leaveGroup(listenForLearnersAddress);

        } catch (IOException | ClassNotFoundException e) {
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