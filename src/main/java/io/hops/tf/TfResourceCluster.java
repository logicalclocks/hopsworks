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
public class TfResourceCluster {

  private int numCpus;
  private int numGpus;

  public TfResourceCluster() {
  }

  public TfResourceCluster(int numCpus, int numGpus) {
    this.numCpus = numCpus;
    this.numGpus = numGpus;
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
  
}
