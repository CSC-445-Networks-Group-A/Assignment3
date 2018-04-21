package proposers;

import chain.Block;
import chain.Transaction;
import chain.User;
import packets.acceptances.AcceptedPacket;
import packets.proposals.ProposalPacket;
import packets.requests.TransactionRequest;
import packets.responses.TransactionApproved;
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

    public Miner(User mybChainMiner, int requestPortNumber, String addressToMakeRequestsOn, int proposalPortNumber,
                 String addressToProposeOn, int learningPortNumber, String addressToLearnOn) throws UnknownHostException {
        super("Miner: " + mybChainMiner.getID());
        this.miner = mybChainMiner;
        this.requestAddress = InetAddress.getByName(addressToMakeRequestsOn);
        this.proposalAddress = InetAddress.getByName(addressToProposeOn);
        this.learnAddress = InetAddress.getByName(addressToLearnOn);
        this.requestPort = requestPortNumber;
        this.proposalPort = proposalPortNumber;
        this.learnPort = learningPortNumber;
        this.pendingTransactions = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {


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
            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];

            /*ServerSocket serverSocket = new ServerSocket(requestPort);
            serverSocket.bind(new InetSocketAddress(requestPort));*/
            boolean running = true;

            while (running) {
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);
                ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectInputStream inputStream = new ObjectInputStream(bais);
                ObjectOutputStream outputStream = new ObjectOutputStream(baos);

                /*Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());*/
                //Transaction transaction = (Transaction) inputStream.readObject();
                Object object = inputStream.readObject();
                if ((object != null) && (object instanceof TransactionRequest)) {
                    Transaction transaction = ((TransactionRequest) object).getTransaction();
                    if (transaction.isVerified()) {
                        pendingTransactions.add(transaction);
                        outputStream.writeObject(new TransactionApproved("Transaction Approved! Thank you for choosing MYB-Coin."));
                    }else {
                        outputStream.writeObject(new TransactionDenied("Transaction Denied: Insufficient Funds or Invalid Signature"));
                    }
                }else {
                    outputStream.writeObject(new TransactionDenied("Transaction Denied: Improperly Formatted Transaction"));
                }
                byte[] output = baos.toByteArray();
                datagramPacket = new DatagramPacket(output, output.length);
                multicastSocket.send(datagramPacket);
                outputStream.close();
                baos.close();
                inputStream.close();
                bais.close();
                //socket.close();
            }

            multicastSocket.leaveGroup(requestAddress);
            //serverSocket.close();
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
        Block block = new Block(miner.getBlockChain().getMostRecentHash());
        while (true) {
            Transaction transaction = pendingTransactions.peek();
            if (transaction != null) {
                block.addVerifiedTransaction(transaction);
            }
            if (block.isFull()) {
                boolean hashFound = false;
                while (!hashFound) {
                    hashFound = block.computeNewHash();
                }
                propose(block);
                Block acceptedBlock = learn();
                if (acceptedBlock == null) {
                    miner.updateBlockChain();
                    acceptedBlock = miner.getBlockChain().getMostRecentBlock();
                }
                pruneQueue(acceptedBlock);
                block = new Block(acceptedBlock.getProofOfWork());
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
     *
     * */
    private Block learn() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(learnPort);
            multicastSocket.joinGroup(learnAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);

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
            return acceptedBlock;

        } catch (SocketTimeoutException ste) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     *
     * */
    private void pruneQueue(Block blockAccepted) {
        for (Transaction transaction : blockAccepted.getTransactions()) {
            pendingTransactions.remove(transaction);
        }
    }

}
