package se.kth.bbc.jobs.adam;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;

/**
 *
 * @author stig
 */
@XmlRootElement
public class AdamJobConfiguration extends SparkJobConfiguration {

  private AdamCommandDTO selectedCommand;

  protected static final String KEY_COMMAND = "command";

  public AdamJobConfiguration() {
    super();
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

  @Override
  public DatabaseJsonObject getReducedJsonObject() {
    if(selectedCommand == null){
      throw new NullPointerException("Null command in AdamJobConfiguration.");
    }
    DatabaseJsonObject obj = super.getReducedJsonObject();
    obj.set(KEY_TYPE, JobType.ADAM.name());
    obj.set(KEY_COMMAND, selectedCommand.getReducedJsonObject());
    return obj;
  }

  @Override
  public void updateFromJson(DatabaseJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    AdamCommandDTO command;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.ADAM) {
        throw new IllegalArgumentException("JobType must be ADAM.");
      }
      DatabaseJsonObject jsonCommand = json.getJsonObject(KEY_COMMAND);
      command = new AdamCommandDTO();
      command.updateFromJson(jsonCommand);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into AdamJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure that the type will get recognized correctly.
    json.set(KEY_TYPE, JobType.SPARK.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.selectedCommand = command;
  }

}
