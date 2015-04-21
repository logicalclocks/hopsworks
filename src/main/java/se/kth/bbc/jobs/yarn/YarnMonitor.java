package se.kth.bbc.jobs.yarn;

import java.io.Closeable;
import java.io.IOException;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

/**
 *
 * @author stig
 */
public final class YarnMonitor implements Closeable {

  private final YarnClient yarnClient;
  private final ApplicationId appId;

  public YarnMonitor(ApplicationId id, YarnClient yarnClient) {
    this.appId = id;
    this.yarnClient = yarnClient;
  }

  public YarnMonitor start() {
    yarnClient.start();
    return this;
  }

  public void stop() {
    yarnClient.stop();
  }

  public boolean isStarted() {
    return yarnClient.isInState(Service.STATE.STARTED);
  }

  public boolean isStopped() {
    return yarnClient.isInState(Service.STATE.STOPPED);
  }

  //---------------------------------------------------------------------------        
  //--------------------------- STATUS QUERIES --------------------------------
  //---------------------------------------------------------------------------
  public YarnApplicationState getApplicationState() throws YarnException,
          IOException {
    return yarnClient.getApplicationReport(appId).getYarnApplicationState();
  }

  //---------------------------------------------------------------------------        
  //------------------------- YARNCLIENT UTILS --------------------------------
  //---------------------------------------------------------------------------
  @Override
  public void close() {
    stop();
  }

  public void cancelJob() throws YarnException, IOException {
    yarnClient.killApplication(appId);
  }
}
