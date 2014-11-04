package se.kth.bbc.lims;

/**
 * Constants class to facilitate deployment on different servers.
 * @author stig
 */
public class Constants {
    
    public static final String server = "LOCAL";
    //public static final String server = "SNURRAN";
    
    public static final String UPLOAD_DIR = server.equals("LOCAL")?"/home/stig/tst":"/tmp";
}
