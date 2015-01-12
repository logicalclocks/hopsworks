package se.kth.bbc.lims;

/**
 * Constants class to facilitate deployment on different servers.
 * TODO: move this to  configuration file to be read!
 *
 * @author stig
 */
public class Constants {

    //Local path to the hiway jar
    public static final String HIWAY_JAR_PATH = "/srv/hiway/hiway-core-0.2.0-SNAPSHOT.jar";
    
    //User under which yarn is run
    public static final String DEFAULT_YARN_USER = "glassfish";
    
    //Relative output path (within hdfs study folder) which to write cuneiform output to
    public static final String CUNEIFORM_DEFAULT_OUTPUT_PATH = "Cuneiform/Output/";
    
    //Default configuration locations
    public static final String DEFAULT_HADOOP_CONF_DIR = "/srv/hadoop/etc/hadoop/";
    public static final String DEFAULT_YARN_CONF_DIR = "/srv/hadoop/etc/hadoop/";
}
