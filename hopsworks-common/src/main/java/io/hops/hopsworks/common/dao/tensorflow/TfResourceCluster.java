package io.hops.hopsworks.common.dao.tensorflow;

import javax.xml.bind.annotation.XmlRootElement;

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
