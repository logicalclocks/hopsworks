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

package io.hops.hopsworks.common.jobs.erasureCode;

import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

@XmlRootElement
public class ErasureCodeJobConfiguration extends JobConfiguration {

  private String filePath;

  protected final static String KEY_TYPE = "type";

  public ErasureCodeJobConfiguration() {
    super();
  }

  public ErasureCodeJobConfiguration(String filePath) {
    this();
    this.filePath = filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getFilePath() {
    return this.filePath;
  }

  @Override
  public JobType getType() {
    return JobType.ERASURE_CODING;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();

    //additional key
    obj.set(KEY_TYPE, JobType.ERASURE_CODING.toString());

    return obj;
  }
}
