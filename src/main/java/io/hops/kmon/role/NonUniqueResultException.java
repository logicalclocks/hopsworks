/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kmon.role;

/**
 *
 * @author jdowling
 */
public class NonUniqueResultException extends RuntimeException {

  public NonUniqueResultException() {
  }
  
  public NonUniqueResultException(String msg) {
    super(msg);
  }
  
  
}
