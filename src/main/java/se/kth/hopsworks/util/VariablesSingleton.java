/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hopsworks.util;

import java.io.File;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import se.kth.bbc.lims.Constants;


@Startup
@Singleton
public class VariablesSingleton {

  @EJB
  private VariablesFacade variables;
  

  @PostConstruct
  public void init() {
    initializeVariablesFromDB();
  }

  /**
   * This method sets global variables from values stored in the database.
   */
  public void initializeVariablesFromDB() {
    Constants.DEFAULT_HDFS_SUPERUSER = setUserVar(Constants.VARIABLE_HDFS_SUPERUSER, Constants.DEFAULT_HDFS_SUPERUSER);
    Constants.DEFAULT_YARN_SUPERUSER = setUserVar(Constants.VARIABLE_YARN_SUPERUSER, Constants.DEFAULT_YARN_SUPERUSER);
    Constants.DEFAULT_SPARK_USER = setUserVar(Constants.VARIABLE_SPARK_USER, Constants.DEFAULT_SPARK_USER);
    Constants.DEFAULT_SPARK_DIR = setDirVar(Constants.VARIABLE_SPARK_DIR, Constants.DEFAULT_SPARK_DIR);
    Constants.DEFAULT_ZEPPELIN_DIR = setDirVar(Constants.VARIABLE_ZEPPELIN_DIR, Constants.DEFAULT_ZEPPELIN_DIR);
    Constants.DEFAULT_FLINK_DIR = setDirVar(Constants.VARIABLE_FLINK_DIR, Constants.DEFAULT_FLINK_DIR);
    Constants.DEFAULT_MYSQL_DIR = setDirVar(Constants.VARIABLE_MYSQL_DIR, Constants.DEFAULT_MYSQL_DIR);
    Constants.DEFAULT_NDB_DIR = setDirVar(Constants.VARIABLE_NDB_DIR, Constants.DEFAULT_NDB_DIR);
    Constants.DEFAULT_ELASTIC_IP = setIpVar(Constants.VARIABLE_ELASTIC_IP, Constants.DEFAULT_ELASTIC_IP);
  }

  private String setUserVar(String varName, String defaultValue) {
    Variables userName = variables.findById(varName);
    if (userName != null && userName.getValue() != null && (userName.getValue().isEmpty()==false)) {
      return userName.getValue();
    }    
    return defaultValue;
  }  
  private String setDirVar(String varName, String defaultValue) {
    Variables dirName = variables.findById(varName);
    if (dirName != null && dirName.getValue() != null && (new File(dirName.getValue()).isDirectory())) {
      return dirName.getValue();
    }    
    return defaultValue;
  }  
  private String setIpVar(String varName, String defaultValue) {
    Variables ip = variables.findById(varName);
    if (ip != null && ip.getValue() != null && Ip.validIp(ip.getValue())) {
      return ip.getValue();
    }    
    return defaultValue;
  }
}