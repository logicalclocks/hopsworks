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

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTCodes.RESTErrorCode;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class TestRESTCodes {
  
  private HashMap<String, List<RESTErrorCode>> values = new HashMap<>();
  private HashMap<String, Integer> ranges = new HashMap<>();
  
  @Before
  public void getRESTErrorCodes() {
    RESTCodes restCodes = new RESTCodes();
    Class<?>[] classes = restCodes.getClass().getClasses();
    Method method;
    for (Class enumClass : classes) {
      try {
        method = enumClass.getMethod("values");
        RESTErrorCode[] obj = (RESTErrorCode[]) method.invoke(null);
        values.put(enumClass.getName(), Arrays.asList(obj));
        ranges.put(enumClass.getName(), obj[0].getRange());
      } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      }
    }
  }
  
  @Test
  public void testDuplicateErrorCodeRange() {
    Set<String> keySet = ranges.keySet();
    for (String key : keySet) {
      for (String key1 : keySet) {
        if (key.equals(key1)) {
          continue;
        }
        assertFalse("errorCode-1: " + key + ", errorCode-2: "+ key1, ranges.get(key).equals(ranges.get(key1)));
      }
    }
  }
  
  /**
   * Detect that no two RESTErrorCode entries have the same errorCode value.
   */
  @Test
  public void detectDuplicateErrorCodes() {
    Set<String> keySet = ranges.keySet();
    List<RESTErrorCode> restErrorCodes;
    for (String key : keySet) {
      restErrorCodes = values.get(key);
      //Find duplicate error codes
      for (int i = 0; i < restErrorCodes.size(); i++) {
        for (int j = i + 1; j < restErrorCodes.size(); j++) {
          assertFalse(key +
            " errorCode-1: " + restErrorCodes.get(i).toString() + ", errorCode-2: " + restErrorCodes.get(j).toString(),
            restErrorCodes.get(i).getCode().equals(restErrorCodes.get(j).getCode()));
        }
      
      }
    }
  
  }
  
  
  /**
   * Validates the enums only contain error codes within the range assigned to them.
   */
  @Test
  public void validateErrorCodes() {
    Set<String> keySet = ranges.keySet();
    List<RESTErrorCode> restErrorCodes;
    for (String key : keySet) {
      restErrorCodes = values.get(key);
      for (RESTErrorCode errorCode : restErrorCodes) {
        if (!(errorCode instanceof RESTCodes.SchemaRegistryErrorCode)) {
          assertTrue(errorCode.toString(), errorCode.getCode() >= errorCode.getRange());
          assertTrue(errorCode.toString(), errorCode.getCode() < errorCode.getRange() + 10000);
        }
      }
    }
  
  }
  
  
}
