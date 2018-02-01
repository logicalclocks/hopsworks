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
package io.hops.hopsworks.common.jobs.jobhistory;

import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;

public enum JobFinalStatus {

  UNDEFINED("Undefined"),
  SUCCEEDED("Succeeded"),
  FAILED("Failed"),
  KILLED("Killed");

  private final String readable;

  private JobFinalStatus(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

  public static JobFinalStatus getJobFinalStatus(
          FinalApplicationStatus yarnFinalStatus) {
    switch (yarnFinalStatus) {
      case UNDEFINED:
        return JobFinalStatus.UNDEFINED;
      case SUCCEEDED:
        return JobFinalStatus.SUCCEEDED;
      case FAILED:
        return JobFinalStatus.FAILED;
      case KILLED:
        return JobFinalStatus.KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant"); // can never happen
    }
  }

}
