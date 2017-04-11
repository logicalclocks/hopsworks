package io.hops.hopsworks.api.zeppelin.util;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LivyMsg {

  private int from;
  private Session[] sessions;
  private int total;

  public LivyMsg() {

  }

  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  public Session[] getSessions() {
    return sessions;
  }

  public void setSessions(Session[] sessions) {
    this.sessions = sessions;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  @XmlRootElement
  public static class Session {

    private int id;
    private String kind;
    private String owner;
    private String proxyUser;
    private String state;

    public Session() {
    }

    public Session(int id, String owner) {
      this.id = id;
      this.owner = owner;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getKind() {
      return kind;
    }

    public void setKind(String kind) {
      this.kind = kind;
    }

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getProxyUser() {
      return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
      this.proxyUser = proxyUser;
    }

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

  }
}
