package io.hops.hopsworks.common.jobs.yarn;

/**
 *
 * @author stig
 */
public abstract class YarnSetupCommand {

  public abstract void execute(YarnRunner r);
}
