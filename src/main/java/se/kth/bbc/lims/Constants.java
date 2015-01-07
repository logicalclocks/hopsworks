package se.kth.bbc.lims;

/**
 * Constants class to facilitate deployment on different servers.
 * TODO: move this to  configuration file to be read!
 *
 * @author stig
 */
public class Constants {

    //public static final String server = "LOCAL";
    public static final String server = "SNURRAN";

    //Staging folder for files to be uploaded in
    public static final String UPLOAD_DIR = server.equals("LOCAL") ? "/home/stig/tst" : "/tmp";
    
    //Staging folder for upload of files used in job execution
    public static final String JOB_UPLOAD_DIR = server.equals("LOCAL") ? "/home/stig/tst/jobs" : "/tmp/jobs";

    //Local path to the hiway jar
    public static final String HIWAY_JAR_PATH = "LOCAL".equals(server)? "/srv/hiway-0.2.0-SNAPSHOT/hiway-core-0.2.0-SNAPSHOT.jar" : "/home/glassfish/stig/hiway-core-0.2.0-SNAPSHOT.jar";
    
    //User under which yarn is run
    public static final String YARN_USER = "LOCAL".equals(server)? "stig":"glassfish";
    
    //Relative output path (within hdfs study folder) which to write cuneiform output to
    public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Cuneiform/Output/";
    
    //Default configuration locations
    public static final String DEFAULT_HADOOP_CONF_DIR = "/srv/hadoop/etc/hadoop/";
    public static final String DEFAULT_YARN_CONF_DIR = "/srv/hadoop/etc/hadoop/";
}
