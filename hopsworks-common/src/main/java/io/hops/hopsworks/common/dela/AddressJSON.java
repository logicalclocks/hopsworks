package io.hops.hopsworks.common.dela;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AddressJSON {

  private String ip;
  private int port;
  private String id;

  public AddressJSON() {

  }

  public AddressJSON(String ip, int port, String id) {
    this.ip = ip;
    this.port = port;
    this.id = id;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  public String getId() {
    return id;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "AddressJSON{" + "ip=" + ip + ", port=" + port + ", id=" + id + '}';
  }

}
