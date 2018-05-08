package proposers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Michael on 5/2/2018.
 */
public class UpdateManagerInitializer {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
        Thread updateManager = new UpdateManager();
        updateManager.start();
        updateManager.join();
    }
}
