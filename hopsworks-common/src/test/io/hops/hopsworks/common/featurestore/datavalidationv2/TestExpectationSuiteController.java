/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.datavalidationv2;

import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationIngestionPolicy;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_DATA_ASSET_TYPE;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_SUITE_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_GE_CLOUD_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestExpectationSuiteController {

  private ExpectationSuiteController expectationSuiteController = new ExpectationSuiteController();
  private List<String> featureNames;
  private ExpectationSuiteDTO expectationSuiteDTO;

  @Before
  public void setup() {
    expectationSuiteDTO = new ExpectationSuiteDTO();
    expectationSuiteDTO.setValidationIngestionPolicy(ValidationIngestionPolicy.STRICT);
    expectationSuiteDTO.setRunValidation(true);
    expectationSuiteDTO.setExpectationSuiteName("default");
    expectationSuiteDTO.setGeCloudId("blue");
    expectationSuiteDTO.setDataAssetType("DATASET");
    expectationSuiteDTO.setMeta(
      "{\"randomKey\": \"randomValue\"}"
    );

    ArrayList<ExpectationDTO> expectations = new ArrayList<>();
    
    expectations.add(makeValidExpectationDTO());
    expectations.add(makeValidExpectationDTO());
    expectationSuiteDTO.setExpectations(expectations);

    featureNames = new ArrayList<String>() {
      {
        add("featureA");
        add("featureB");
      }
    };
  }

  private ExpectationDTO makeValidExpectationDTO() {
    ExpectationDTO dto = new ExpectationDTO();
    dto.setExpectationType("expect_column_max_to_be_between");
    dto.setMeta("{\"expectationId\": 12}");
    dto.setKwargs("{\"min_value\": 0, \"max_value\": 1}");

    return dto;
  }

  // Expectation Suite input validation

  @Test
  public void testVerifyExpectationSuiteMeta() {
     // Null
    expectationSuiteDTO.setMeta(null);
    FeaturestoreException nullInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to null input error: ", 202,
      nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

    // Long input
    String longInput = ("{\"longInput\": \""
      + StringUtils.repeat("A", MAX_CHARACTERS_IN_EXPECTATION_SUITE_META + 10) +"\"}");
    expectationSuiteDTO.setMeta(longInput);
    FeaturestoreException longInputException = assertThrows(
      FeaturestoreException.class,
      () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to exceed max character error", 200,
      longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());

    // Invalid Json
    String notAJsonInput = "I am not a Json";
    expectationSuiteDTO.setMeta(notAJsonInput);
    FeaturestoreException notAJsonException = assertThrows(
      FeaturestoreException.class,
      () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
    );
    assertEquals("Rest code error corresponding to json parse failure: ", 201,
      notAJsonException.getErrorCode().getCode() - notAJsonException.getErrorCode().getRange());
  }

    @Test
    public void testVerifyExpectationSuiteName() {
      // Null
      expectationSuiteDTO.setExpectationSuiteName(null);
      FeaturestoreException nullInputException = assertThrows(
        FeaturestoreException.class,
        () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
      );
      assertEquals("Rest code error corresponding to null input error: ", 202,
        nullInputException.getErrorCode().getCode() - nullInputException.getErrorCode().getRange());

      // Long input
      String longInput = StringUtils.repeat("A", MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME + 10);
      expectationSuiteDTO.setExpectationSuiteName(longInput);
      FeaturestoreException longInputException = assertThrows(
        FeaturestoreException.class,
        () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
      );
      assertEquals("Rest code error corresponding to exceed max character error: ", 200,
        longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());
    }

    @Test
    public void testVerifyExpectationGeCloudId() {
      // Long input
      String longInput = StringUtils.repeat("A", MAX_CHARACTERS_IN_GE_CLOUD_ID + 10);
      expectationSuiteDTO.setGeCloudId(longInput);
      FeaturestoreException longInputException = assertThrows(
        FeaturestoreException.class,
        () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
      );
      assertEquals("Rest code error corresponding to exceed max character error: ", 200,
        longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());
    }

    @Test
    public void testVerifyExpectationDataAssetType() {
      // Long input
      String longInput = StringUtils.repeat("A", MAX_CHARACTERS_IN_DATA_ASSET_TYPE + 10);
      expectationSuiteDTO.setDataAssetType(longInput);
      FeaturestoreException longInputException = assertThrows(
        FeaturestoreException.class,
        () -> expectationSuiteController.verifyExpectationSuite(expectationSuiteDTO, featureNames)
      );
      assertEquals("Rest code error corresponding to exceed max character error: ", 200,
        longInputException.getErrorCode().getCode() - longInputException.getErrorCode().getRange());
    }

  }
