/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks;

import io.hops.hopsworks.common.exception.RESTCodes.CAErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.DatasetErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.DelaErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.GenericErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.JobErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.KafkaErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.MetadataErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.ProjectErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.RESTErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.RequestErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.SecurityErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.ServiceErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.TfServingErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.UserErrorCode;
import io.hops.hopsworks.common.exception.RESTCodes.ZeppelinErrorCode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class TestRESTCodes {
  
  private List<RESTErrorCode> values = new ArrayList<>();
  
  @Before
  public void getRESTErrorCodes() {
    values.addAll(Arrays.asList(CAErrorCode.values()));
    values.addAll(Arrays.asList(DatasetErrorCode.values()));
    values.addAll(Arrays.asList(DelaErrorCode.values()));
    values.addAll(Arrays.asList(DelaCSRErrorCode.values()));
    values.addAll(Arrays.asList(TfServingErrorCode.values()));
    values.addAll(Arrays.asList(ProjectErrorCode.values()));
    values.addAll(Arrays.asList(SecurityErrorCode.values()));
    values.addAll(Arrays.asList(GenericErrorCode.values()));
    values.addAll(Arrays.asList(ServiceErrorCode.values()));
    values.addAll(Arrays.asList(RequestErrorCode.values()));
    values.addAll(Arrays.asList(KafkaErrorCode.values()));
    values.addAll(Arrays.asList(JobErrorCode.values()));
    values.addAll(Arrays.asList(MetadataErrorCode.values()));
    values.addAll(Arrays.asList(UserErrorCode.values()));
    values.addAll(Arrays.asList(ZeppelinErrorCode.values()));
  
  }
  
  /**
   * Detect that no two RESTErrorCode entries have the same errorCode value.
   */
  @Test
  public void detectDuplicateErrorCodes() {
    
    
    //Find duplicate error codes
    for (int i = 0; i < values.size(); i++) {
      for (int j = i+1; j < values.size(); j++) {
        assertFalse("errorCode-1: " +values.get(i).toString()+ ", errorCode-2:"+ values.get(j).toString(),
          values.get(i).getCode().equals(values.get(j).getCode()));
      }
      
    }
  }
  
  /**
   * Validates the enums only contain error codes within the range assigned to them.
   */
  @Test
  public void validateErrorCodes() {
    
    for (RESTErrorCode errorCode : values) {
      assertTrue(errorCode.toString(), errorCode.getCode() >= errorCode.getRange());
      assertTrue(errorCode.toString(), errorCode.getCode() < errorCode.getRange() + 10000);
    }
    
  }
  
  
}
