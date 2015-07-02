package se.kth.bbc.jobs.adam;

import se.kth.bbc.jobs.spark.SparkJobConfiguration;

/**
 *
 * @author stig
 */
public class AdamJobConfiguration extends SparkJobConfiguration {
  
  private AdamCommandDTO selectedCommand;

  public AdamJobConfiguration() {
  }

  public AdamJobConfiguration(AdamCommandDTO selectedCommand) {
    this.selectedCommand = selectedCommand;
  }

  public AdamCommandDTO getSelectedCommand() {
    return selectedCommand;
  }

  public void setSelectedCommand(AdamCommandDTO selectedCommand) {
    this.selectedCommand = selectedCommand;
  }
  
}
