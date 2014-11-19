package se.kth.bbc.yarn;

import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.lims.MessagesController;

/**
 *
 * @author stig
 */
public class AppMasterStarter {
    
    private static final Logger logger = Logger.getLogger(AppMasterStarter.class.getName());
    
    public static void start() {
        try {
            Client client = Client.getInitializedClient("/home/stig/cuneiform/hadoop-2.4.0/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.4.0.jar", null);
            Thread b = new Thread(client);
            b.start();
        } catch (Exception e) {
            MessagesController.addErrorMessage("Error", "An error occured while running the Yarn client.");
        }
    }
}
