/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kth.bbc.jobs.jobhistory;

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

  public static JobFinalStatus getJobFinalStatus(FinalApplicationStatus
      yarnFinalStatus) {
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