/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CharonDTO implements Serializable {

  private String charonPath;
  private String hdfsPath;
  private String string;
  private String permissions;
  private String granteeId;

  public CharonDTO(String charonPath, String hdfsPath) {
    this.charonPath = charonPath;
    this.hdfsPath = hdfsPath;
  }

  public CharonDTO(String string, String permissions, String granteeId) {
    this.string = string;
    this.permissions = permissions;
    this.granteeId = granteeId;
  }

  public CharonDTO(String string) {
    this.string = string;
  }

  public CharonDTO() {
  }

  public String getCharonPath() {
    return charonPath;
  }

  public String getHdfsPath() {
    return hdfsPath;
  }

  public String getPermissions() {
    return permissions;
  }

  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }

  public String getGranteeId() {
    return granteeId;
  }

  public void setGranteeId(String granteeId) {
    this.granteeId = granteeId;
  }

  public String getString() {

    return string;
  }

  public void setCharonPath(String charonPath) {
    this.charonPath = charonPath;
  }

  public void setString(String string) {
    this.string = string;
  }

  public void setHdfsPath(String hdfsPath) {
    this.hdfsPath = hdfsPath;
  }

  @Override
  public String toString() {
    return "Path: " + charonPath + " ; type: " + hdfsPath;
  }
  
}
