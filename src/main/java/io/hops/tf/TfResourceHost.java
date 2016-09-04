/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.tf;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@XmlRootElement
public class TfResourceHost {

  private int numCpus;
  private int numGpus;
  private String host;

  public TfResourceHost() {
  }

  public TfResourceHost(int numCpus, int numGpus, String host) {
    this.numCpus = numCpus;
    this.numGpus = numGpus;
    this.host = host;
  }

  public int getNumCpus() {
    return numCpus;
  }

  public int getNumGpus() {
    return numGpus;
  }

  public void setNumCpus(int numCpus) {
    this.numCpus = numCpus;
  }

  public void setNumGpus(int numGpus) {
    this.numGpus = numGpus;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }
  
}
