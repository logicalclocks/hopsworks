package se.kth.bbc.jobs.spark;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
public class SparkController {
  
  
  public void startJob(){
    YarnRunner.Builder builder = new YarnRunner.Builder(Constants.SPARK_AM_MAIN);
    String appJarPath = "";
    String sparkJarPath = "";
    String userClass="";
    String userJar="";
    String userArgs = "";
    Map<String,String> extraFiles = new HashMap<>();

    //Spark staging directory
    String stagingPath = File.separator + "user" + File.separator + Utils.
            getYarnUser() + File.separator + Constants.SPARK_STAGING_DIR
            + File.separator + YarnRunner.APPID_PLACEHOLDER;

    builder.localResourcesBasePath(stagingPath);
    
    //Add app and spark jar
    //TODO: check if you can remove these?
    builder.addLocalResource(Constants.SPARK_LOCRSC_SPARK_JAR, sparkJarPath);
    builder.addLocalResource(Constants.SPARK_LOCRSC_APP_JAR, appJarPath);
    //TODO: check if env variables need to be added (see ClientDistributedCacheManager.scala)
    
    //Add extra files to local resources, as key: use filename
    for(Map.Entry<String,String> k:extraFiles.entrySet()){
      builder.addLocalResource(k.getKey(), k.getValue());
    }
    
    //TODO: add to classpath: spark jar, user specified jars, user specified jar, extra classes from conf file
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", Utils.getYarnUser());
    //TODO: add local resources to env
    //TODO: add env vars from sparkconf to path
    
    //TODO add java options from spark config (or not...)
    
    builder.amArgs("--class "+userClass+" --jar "+userJar+" --arg "+userArgs);
    //TODO: add options: --executor-memory, --executor-cors, --num-executors
    
    //TODO: set app name
    //TODO: set queue
    //TODO: set application type (?)
    //And that should be it!
    //TODO: make SparkJob with this YarnRunner
    //TODO: run SparkJob
  }
}
