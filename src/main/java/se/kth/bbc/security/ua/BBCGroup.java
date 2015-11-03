/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public enum BBCGroup {

  BBC_ADMIN(1001),
  BBC_RESEARCHER(1002),
  BBC_GUEST(1003),
  AUDITOR(1004),
  SYS_ADMIN(1005);

  private final int value;

  private BBCGroup(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
