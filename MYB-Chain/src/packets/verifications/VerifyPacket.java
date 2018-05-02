package packets.verifications;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Created by Michael on 4/18/2018.
 */
public class VerifyPacket extends Packet implements Comparable<VerifyPacket>{
    private final RSAPublicKey publicKey;
    private final BigInteger chainLength;
    private final Block block;
    private final byte[] encryptedData;


    public VerifyPacket(RSAPublicKey rsaPublicKey, BigInteger blockChainLength, Block blockValue, byte[] dataEncrypted) {
        super(PacketTypes.SINGLE_VERIFICATION);
        this.publicKey = rsaPublicKey;
        this.chainLength = blockChainLength;
        this.block = blockValue;
        this.encryptedData = dataEncrypted;

    }


    public static byte[] encryptPacketData(RSAPrivateKey rsaPrivateKey, BigInteger blockChainLength, Block blockValue)
            throws IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);
        return cipher.doFinal(getUnencryptedData(blockChainLength, blockValue));
    }


    public static byte[] getUnencryptedData(BigInteger blockChainLength, Block blockValue) throws IOException {
        byte[] lengthData = blockChainLength.toByteArray();
        byte[] blockData = blockValue.getBlockBytes();
        byte[] packetData = new byte[lengthData.length + blockData.length];
        int offset = 0;
        for (int i = 0; i < lengthData.length; i++) {
            packetData[offset + i] = lengthData[i];
        }
        offset += lengthData.length;
        for (int i = 0; i < blockData.length; i++) {
            packetData[offset + i] = blockData[i];
        }
        return packetData;
    }


    public RSAPublicKey getPublicKey() {
        return publicKey;
    }


    public BigInteger getChainLength() {
        return chainLength;
    }


    public Block getBlock() {
        return block;
    }


    public byte[] getEncryptedData() {
        return Arrays.copyOf(encryptedData, encryptedData.length);
    }


    public boolean isHonest() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] unencryptedData = getUnencryptedData(chainLength, block);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);

        if (decryptedData.length != unencryptedData.length) {
            return false;
        }

        for (int i = 0; i < unencryptedData.length; i++) {
            if (decryptedData[i] != unencryptedData[i]) {
                return false;
            }
        }

        return true;
    }


    public int compareTo(VerifyPacket otherPacket) {
        if (chainLength.compareTo(otherPacket.getChainLength()) != 0) {
            return chainLength.compareTo(otherPacket.getChainLength());
        }

        byte[] thisProofOfWork = block.getProofOfWork();
        byte[] otherProofOfWork = otherPacket.getBlock().getProofOfWork();

        for (int i = 0; i < thisProofOfWork.length; i++) {
            if (thisProofOfWork[i] != 0 && otherProofOfWork[i] != 0) {
                break;
            }else if (thisProofOfWork[i] == 0 && otherProofOfWork[i] != 0) {
                return 1;
            }else if (otherProofOfWork[i] == 0 && thisProofOfWork[i] != 0) {
                return -1;
            }
        }
        return 0;
    }

}
