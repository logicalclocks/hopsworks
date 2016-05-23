/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.zeppelin.user;


public class AuthenticationInfo {
  String user;
  String ticket;

  public AuthenticationInfo() {}

  /***
   *
   * @param user
   * @param ticket
   */
  public AuthenticationInfo(String user, String ticket) {
    this.user = user;
    this.ticket = ticket;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getTicket() {
    return ticket;
  }

  public void setTicket(String ticket) {
    this.ticket = ticket;
  }
}
