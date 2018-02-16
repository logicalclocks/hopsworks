/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.jobs.adam;

import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;

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
  public JobType getType() {
    return JobType.ADAM;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    if (selectedCommand == null) {
      throw new NullPointerException("Null command in AdamJobConfiguration.");
    }
    MutableJsonObject obj = super.getReducedJsonObject();
    obj.set(KEY_TYPE, JobType.ADAM.name());
    obj.set(KEY_COMMAND, selectedCommand.getReducedJsonObject());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
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
      MutableJsonObject jsonCommand = json.getJsonObject(KEY_COMMAND);
      command = new AdamCommandDTO();
      command.updateFromJson(jsonCommand);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into AdamJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure 
    //that the type will get recognized correctly.
    json.set(KEY_TYPE, JobType.SPARK.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.selectedCommand = command;
  }

}
