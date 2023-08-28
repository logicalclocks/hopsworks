/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.utils;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestFeatureStoreInputValidation {
  
  private FeaturestoreInputValidation featurestoreInputValidation;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    featurestoreInputValidation = new FeaturestoreInputValidation();
  }
  
  @Test
  public void testVerifyUserInputFeatureGroup() throws Exception {
    FeaturegroupDTO featuregroupDTO = new FeaturegroupDTO(1, "featurestore", 1, "1wrong_name", 1, "online_topic_name", false);
    // upper case
    featuregroupDTO.setName("UPPER_CASE");
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
  
    // starts with numeric
    featuregroupDTO.setName("12_numeric");
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
  
    // too long
    featuregroupDTO.setName(StringUtils.repeat("a", 260));
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
  
    // empty
    featuregroupDTO.setName("");
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
  }
  
  @Test
  public void testVerifyDescription() throws Exception {
    FeaturegroupDTO featuregroupDTO = new FeaturegroupDTO(1, "featurestore", 1, "wrong_name", 1, "online_topic_name", false);
    
    // description is null
    featuregroupDTO.setDescription(null);
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
    
    // description is empty
    featuregroupDTO.setDescription("");
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
    
    // description is too long
    featuregroupDTO.setDescription(StringUtils.repeat("a", 260));
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.verifyUserInput(featuregroupDTO);
  }
  
  @Test
  public void testNameValidation() throws FeaturestoreException {
    // upper case
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.nameValidation("UPPER_CASE");
    
    // starts with numeric
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.nameValidation("12_numeric");
  
    // too long
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.nameValidation(StringUtils.repeat("a", 64));
  
    // empty
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.nameValidation("");
  }
  
  @Test
  public void testDescriptionValidation() throws Exception {
    // description is too long
    thrown.expect(FeaturestoreException.class);
    featurestoreInputValidation.descriptionValidation("irrelevant_name", StringUtils.repeat("a", 260));
  }
}
