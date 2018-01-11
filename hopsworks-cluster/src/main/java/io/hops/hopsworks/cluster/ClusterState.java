package io.hops.hopsworks.cluster;

import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
public class ClusterState {
  private final static Logger LOG = Logger.getLogger(ClusterState.class.getName());
  
  //TODO Alex - make it read this from the variables tables
  public static final boolean bypassActivationLink = true;

  public boolean bypassActivationLink() {
    return bypassActivationLink;
  }
}
