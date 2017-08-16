package io.hops.hopsworks.common.dao.jupyter.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement

public class JupyterDTO {

  private int port=0;
  private String token="";
  private long pid=0;
  private int driverCores=1;
  private String driverMemory="500M";
  private int numExecutors=1;
  private int executorCores=1;
  private String executorMemory="500M";
  private int gpus=0;
  private int numParamServers=0;
  private boolean tensorflow=false;
  private String archives="";
  private String jars="";
  private String files="";
  private String pyFiles="";
  private String hostIp="";

  public JupyterDTO() {
  }

  public JupyterDTO(int port, String token, long pid, int driverCores,
          String driverMemory, int numExecutors, int executorCores, String executorMemory,
          int gpus, String archives, String jars, String files, String pyFiles,
          int numParameterServers, boolean tensorflow) {
    this.port = port;
    this.token = token;
    this.pid = pid;
    this.driverCores = driverCores;
    this.driverMemory = driverMemory;
    this.numExecutors = numExecutors;
    this.executorCores = executorCores;
    this.executorMemory = executorMemory;
    this.pid = pid;
    this.gpus = gpus;
    this.archives = archives;
    this.jars = jars;
    this.files = files;
    this.pyFiles = pyFiles;
    try {
      this.hostIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterDTO.class.getName()).log(Level.SEVERE, null, ex);
    }
    this.numParamServers = numParameterServers;
    this.tensorflow = tensorflow;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String host) {
    this.hostIp = host;
  }
  
  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public int getDriverCores() {
    return driverCores;
  }

  public String getDriverMemory() {
    return driverMemory;
  }

  public int getNumExecutors() {
    return numExecutors;
  }

  public void setNumExecutors(int numExecutors) {
    this.numExecutors = numExecutors;
  }
  
  public int getExecutorCores() {
    return executorCores;
  }

  public String getExecutorMemory() {
    return executorMemory;
  }

  public void setDriverCores(int driverCores) {
    this.driverCores = driverCores;
  }

  public void setDriverMemory(String driverMemory) {
    this.driverMemory = driverMemory;
  }

  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }

  public void setExecutorMemory(String executorMemory) {
    this.executorMemory = executorMemory;
  }

  public int getGpus() {
    return gpus;
  }

  public void setGpus(int gpus) {
    this.gpus = gpus;
  }

  
  public String getArchives() {
    return archives;
  }

  public void setArchives(String archives) {
    this.archives = archives;
  }

  public String getJars() {
    return jars;
  }

  public void setJars(String jars) {
    this.jars = jars;
  }

  
  public String getFiles() {
    return files;
  }

  public void setFiles(String files) {
    this.files = files;
  }

  public String getPyFiles() {
    return pyFiles;
  }

  public void setPyFiles(String pyFiles) {
    this.pyFiles = pyFiles;
  }

  public int getNumParamServers() {
    return numParamServers;
  }

  public void setNumParamServers(int numParamServers) {
    this.numParamServers = numParamServers;
  }

  public boolean isTensorflow() {
    return tensorflow;
  }

  public void setTensorflow(boolean tensorflow) {
    this.tensorflow = tensorflow;
  }

}
