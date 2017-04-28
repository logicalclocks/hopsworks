package io.hops.hopsworks.common.dao.jupyter.config;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement

public class JupyterDTO {

  private int port;
  private String token;
  private long pid;

  public JupyterDTO() {
  }

  public JupyterDTO(int port, String token, long pid) {
    this.port = port;
    this.token = token;
    this.pid = pid;
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
  
}
