package se.kth.bbc.lims;

/**
 * Constants class to facilitate deployment on different servers.
 * TODO: move this to  configuration file to be read!
 *
 * @author stig
 */
public class Constants {

    public static final String server = "LOCAL";
    //public static final String server = "SNURRAN";

    public static final String UPLOAD_DIR = server.equals("LOCAL") ? "/home/stig/tst" : "/tmp";
    
    public static final String LOCAL_APPMASTER_DIR = server.equals("LOCAL") ? "/home/stig/tst/appMaster" : "/tmp/appMaster";
    public static final String LOCAL_EXTRA_DIR = server.equals("LOCAL") ? "/home/stig/tst/extraFiles" : "/tmp/extraFiles";
    
    public static final String JOB_UPLOAD_DIR = server.equals("LOCAL") ? "/home/stig/tst/jobs" : "/tmp/jobs";

    public static final String FLINK_CONF_DIR = server.equals("LOCAL") ? "/home/stig/projects/flink-yarn-0.7.0-incubating/conf/.yarn-properties" : "/home/glassfish/stig/flink-0.8-incubating-SNAPSHOT/conf/.yarn-properties";

    public static final String HIWAY_JAR_PATH = "LOCAL".equals(server)? "/srv/hiway-0.2.0-SNAPSHOT/hiway-core-0.2.0-SNAPSHOT.jar" : "/home/glassfish/stig/hiway-core-0.2.0-SNAPSHOT.jar";
    
    public static final String YARN_USER = "LOCAL".equals(server)? "stig":"glassfish";
    
    public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Cuneiform/Output/";
    
    public static final String DEFAULT_CONF_PATH = "/srv/hadoop/etc/hadoop/";
}
