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
