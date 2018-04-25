package proposers;

import chain.User;
import javafx.util.Pair;
import packets.Packet;
import packets.requests.LoginRequest;
import packets.responses.LoginDenied;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Michael on 4/18/2018.
 */
public class LoginManager extends Thread{

    private int requestPort;
    private InetAddress requestAddress;
    private ConcurrentLinkedQueue<User> pendingLogins = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Pair<InetAddress, Integer>> pendingAddresses = new ConcurrentLinkedQueue<>();

    public LoginManager(int requestPort, String requestAddress) throws UnknownHostException {
        super("LoginManager");
        this.requestPort = requestPort;
        this.requestAddress = InetAddress.getByName(requestAddress);
    }

    @Override
    public void run() {
        try {
            Thread clientLoginThread = new Thread(() -> listenToClients());

            clientLoginThread.start();

            while (!clientLoginThread.getState().equals(Thread.State.TERMINATED)) {
                /*
                 * SPIN
                 * */
            }

            clientLoginThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void listenToClients() {
        try {
            System.out.println("STARTING:\t" + Thread.currentThread().getName() + "\n" +
                    "Listen Port:\t" + requestPort);

            MulticastSocket multicastSocket = new MulticastSocket(requestPort);
            multicastSocket.joinGroup(requestAddress);

            boolean running = true;

            while (running) {
                byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);
                ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                ObjectInputStream inputStream = new ObjectInputStream(bais);

                Object object = inputStream.readObject();
                if ((object != null) && (object instanceof LoginRequest)) {
                    LoginRequest loginRequest = (LoginRequest) object;
                    User user = loginRequest.getUser();
                    InetAddress userAddress = loginRequest.getOriginAddress();
                    int userPort = loginRequest.getOriginPortNumber();

                    if(user.getID() != null){
                        //There is already a userID assigned to this object
                        respondToClient(userAddress, userPort, new LoginDenied("Login Failed: User already created"));
                    }else{
                        pendingLogins.add(user);
                        pendingAddresses.add(new Pair<>(userAddress, userPort));
                    }
                }

                inputStream.close();
                bais.close();
            }

            multicastSocket.leaveGroup(requestAddress);
            System.out.println("FINISHING:\t" + Thread.currentThread().getName());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //TODO: Somehow check the pending logins to see if the user id already exists. May be pointless with random ID's
    //TODO: Submit a transaction FROM login user, TO login user, AMOUNT 0.0. This is how we will commit a user to the chain
    //TODO: Respond to the user with their new user id after it has been set. (LoginAccepted) packet

    private void respondToClient(InetAddress clientAddress, int port, Packet response) {
        try {
            Socket socket = new Socket(clientAddress, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(response);
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
