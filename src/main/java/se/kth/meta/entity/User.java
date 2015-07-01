package se.kth.meta.entity;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 *
 * @author vangelis
 */
public class User implements HttpSessionBindingListener {

  private int id;
  private String username;
  private String password;
  private String mail;

  private int num;

  public User(int id, String username, String password, String email) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.mail = email;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getEmail() {
    return this.mail;
  }

  public void setNum(int num) {
    this.num = num;
  }

  public int getNum() {
    return this.num;
  }

  public int getId() {
    return this.id;
  }

  @Override
  public void valueBound(HttpSessionBindingEvent event) {
    //System.out.println("USER " + username + " WAS BOUND TO THE SESSION");
  }

  @Override
  public void valueUnbound(HttpSessionBindingEvent event) {
    //System.out.println("USER " + username + " WAS UNBOUND FROM THE SESSION");
  }
}
