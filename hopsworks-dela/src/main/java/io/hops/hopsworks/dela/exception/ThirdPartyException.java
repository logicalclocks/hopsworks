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

package io.hops.hopsworks.dela.exception;

import io.hops.hopsworks.common.exception.AppException;

public class ThirdPartyException extends AppException {

  private Source source;
  private String sourceDetails;
  
  public ThirdPartyException(int status, String msg, Source source, String sourceDetails) {
    super(status, msg);
    this.source = source;
    this.sourceDetails = sourceDetails;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  public String getSourceDetails() {
    return sourceDetails;
  }

  public void setSourceDetails(String sourceDetails) {
    this.sourceDetails = sourceDetails;
  }
  
  public static enum Source {
    LOCAL, SETTINGS, MYSQL, HDFS, KAFKA, DELA, REMOTE_DELA, HOPS_SITE
  }
  
  public static ThirdPartyException from(AppException ex, Source source, String sourceDetails) {
    return new ThirdPartyException(ex.getStatus(), ex.getMessage(), source, sourceDetails);
  }
  
  public static enum Error {
    CLUSTER_NOT_REGISTERED("cluster not registered"),
    HEAVY_PING("heavy ping required"),
    USER_NOT_REGISTERED("user not registered"), 
    DATASET_EXISTS("dataset exists"),
    DATASET_DOES_NOT_EXIST("dataset does not exist");
    
    
    private final String msg;
    
    Error(String msg) {
      this.msg = msg;
    }
    
    public boolean is(String msg) {
      return this.msg.equals(msg);
    }

    @Override
    public String toString() {
      return msg;
    }
  }
}
