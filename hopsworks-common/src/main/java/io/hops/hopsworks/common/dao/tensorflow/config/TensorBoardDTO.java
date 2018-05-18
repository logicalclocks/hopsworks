package io.hops.hopsworks.common.dao.tensorflow.config;

import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TensorBoardDTO {

  private int port=0;
  private BigInteger pid=BigInteger.valueOf(1);
  private String hostIp="";
  private int exitValue=-1;

  public TensorBoardDTO(BigInteger pid, int port, int exitValue) {
    this.port = port;
    this.pid = pid;
    this.exitValue = exitValue;
    try {
      this.hostIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterDTO.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public int getExitValue() {
    return exitValue;
  }

  public void setExitValue(int exitValue) {
    this.exitValue = exitValue;
  }
}
