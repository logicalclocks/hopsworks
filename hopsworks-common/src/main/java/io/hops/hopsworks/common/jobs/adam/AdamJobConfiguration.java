/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
