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
  private String hostIp="";
  private String secret="";

  public JupyterDTO() {
  }

  public JupyterDTO(int port, String token, long pid, String secret) {
    this.port = port;
    this.token = token;
    this.pid = pid;
    this.secret = secret;
    try {
      this.hostIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterDTO.class.getName()).log(Level.SEVERE, null, ex);
    }
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

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
  
}
