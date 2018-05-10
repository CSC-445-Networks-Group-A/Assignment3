package proposers;

import chain.Block;
import chain.Transaction;
import chain.User;
import common.Addresses;
import common.NodeType;
import common.Ports;
import common.Pair;
import packets.Packet;
import packets.acceptances.AcceptedPacket;
import packets.proposals.ProposalPacket;
import packets.requests.TransactionRequest;
import packets.responses.TransactionAccepted;
import packets.responses.TransactionPending;
import packets.responses.TransactionDenied;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Michael on 4/15/2018.
 */
public class Miner extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private final User miner;
    private final InetAddress requestAddress;
    private final InetAddress proposalAddress;
    private final InetAddress learnAddress;
    private final int requestPort;
    private final int proposalPort;
    private final int learnPort;
    private ConcurrentLinkedQueue<Transaction> pendingTransactions;
    private ConcurrentLinkedQueue<Pair<InetAddress, Integer>> pendingAddresses;

    public Miner(User mybChainMiner) throws UnknownHostException {
        super("Miner: " + mybChainMiner.getID());
        this.miner = mybChainMiner;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.proposalAddress = InetAddress.getByName(Addresses.MINER_PROPOSAL_ADDRESS);
        this.learnAddress = InetAddress.getByName(Addresses.MINER_LEARNING_ADDRESS);
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.proposalPort = Ports.MINER_PROPOSAL_PORT;
        this.learnPort = Ports.MINER_LEARNING_PORT;
        this.pendingTransactions = new ConcurrentLinkedQueue<>();
        this.pendingAddresses = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        try {
            Thread listenThread = new Thread(() -> listenToClients());
            Thread miningThread = new Thread(() -> {
                try {
                    mine();
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            });

            listenThread.start();
            miningThread.start();

            while (!listenThread.getState().equals(State.TERMINATED) && !miningThread.getState().equals(State.TERMINATED)) {
                /*
                * SPIN
                * */
            }

            listenThread.join();
            miningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     *
     * */
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
                if ((object != null) && (object instanceof TransactionRequest)) {
                    TransactionRequest transactionRequest = (TransactionRequest) object;
                    Transaction transaction = transactionRequest.getTransaction();
                    InetAddress userAddress = transactionRequest.getReturnAddress();
                    int userPort = transactionRequest.getReturnPort();

                    System.out.println(
                            "TR from: " + userAddress.getHostAddress() + "  " +
                                    transaction.getBuyerName() + " ---(" + transaction.getTransactionAmount() + ")---> " + transaction.getSellerName()
                    );

                    if (transaction.isVerified()) {
                        pendingTransactions.add(transaction);
                        pendingAddresses.add(new Pair<>(userAddress, userPort));
                        respondToUserRequest(userAddress, userPort, new TransactionPending("Transaction Pending..."));
                    }else {
                        respondToUserRequest(userAddress, userPort, new TransactionDenied("Transaction Denied: Insufficient Funds or Invalid Signature"));
                    }
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

    /**
     *
     * */
    private void mine() throws IOException, NoSuchAlgorithmException {
        Block block = miner.getBlockChain().getMostRecentBlock();
        if (block == null) {
            block = new Block(miner.getBlockChain().getMostRecentHash());
            Transaction payment = new Transaction(miner, miner.getBlockChain().computeMinerAward(miner.getBlockChain().getChainLength()));
            block.addVerifiedTransaction(payment);
        }
        while (true) {
            //FIXME ----- TESTING
            for (int i = 1; i < 10; i++) {
                Transaction payment = new Transaction(miner, miner.getBlockChain().computeMinerAward(miner.getBlockChain().getChainLength()));
                block.addVerifiedTransaction(payment);
            }
            //FIXME ----- TESTING
            if (block.isFull()) {
                System.out.println("BLOCK IS FULL ---- COMPUTING NEW HASH");
                boolean hashFound = false;
                while (!hashFound) {
                    hashFound = block.computeNewHash();
                }
                System.out.println("HASH COMPUTED ---- PROPOSING");
                propose(block);
                Block acceptedBlock = learn();
                if (acceptedBlock == null) {
                    miner.updateBlockChain();
                    acceptedBlock = miner.getBlockChain().getMostRecentBlock();
                }
                notifyUsers(block, acceptedBlock);
                block = new Block(acceptedBlock.getProofOfWork());
                Transaction payment = new Transaction(miner, miner.getBlockChain().computeMinerAward(miner.getBlockChain().getChainLength()));
                block.addVerifiedTransaction(payment);
            }else {
                Transaction transaction = pendingTransactions.poll();
                if (transaction != null) {
                    block.addVerifiedTransaction(transaction);
                }
            }
        }
    }


    /**
     * Checks all Transactions on the Ledger, verifying that each chain.Transaction's Signature is capable of being decrypted
     * by the associated "buyer".
     * */
    private void verifyLedger(Block block) {

    }


    /**
     *
     * */
    private void propose(Block block) {
        try {
            System.out.println("PROPOSING:\t" + Thread.currentThread().getName() + "\n" +
                    "Proposal Port:\t" + proposalPort);

            MulticastSocket multicastSocket = new MulticastSocket(proposalPort);
            multicastSocket.joinGroup(proposalAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            ProposalPacket proposalPacket = new ProposalPacket(miner.getBlockChain().getChainLength(), miner.getID(), block);
            outputStream.writeObject(proposalPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, proposalAddress, proposalPort);
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(proposalAddress);
            System.out.println("FINISHING PROPOSAL:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * */
    private Block learn() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(learnPort);
            multicastSocket.joinGroup(learnAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);

            /*
            * TODO: Wait for 2f + 1 responses that are equivalent, then use that response accordingly
            * */

            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(datagramPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            Object object = inputStream.readObject();
            Block acceptedBlock = null;
            if ((object != null) && (object instanceof AcceptedPacket)) {
                acceptedBlock = ((AcceptedPacket) object).getBlock();

            }
            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(learnAddress);
            return acceptedBlock;

        } catch (SocketTimeoutException ste) {
            return null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * FIXME
     * */
    private void notifyUsers(Block blockCreated, Block blockAccepted) {
        Transaction[] transactions = blockCreated.getTransactions();
        Transaction[] transactionsAccepted = blockAccepted.getTransactions();
        for (int i = 0; i < transactions.length; i++) {
            Pair<InetAddress, Integer> pair = pendingAddresses.poll();
            boolean matchFound = false;
            for (int j = 0; j < transactions.length; j++) {
                if (transactions[i].equals(transactionsAccepted[j])) {
                    matchFound = true;
                    break;
                }
            }
            if (matchFound) {
                respondToUserRequest(pair.getKey(), pair.getValue(), new TransactionAccepted("Transaction Accepted!\n" +
                        "Thank you for choosing MYB-Coin."));
            } else {
                respondToUserRequest(pair.getKey(), pair.getValue(), new TransactionDenied("Transaction Denied.\n" +
                        "Please check your connection and try again."));
            }
        }
    }

    private void respondToUserRequest(InetAddress address, int port, Packet response) {

        System.out.println(
                "Resp to: " + address.getHostAddress() + "  with msg: " + response.getPacketType()
        );

        try {
            Thread.sleep(100);

            Socket socket = new Socket(address, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(response);
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
