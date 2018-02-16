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
