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

package io.hops.hopsworks.common.featurestore.transformationfunction;

import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestTransformationFunctionController {
  
  private Project project;
  private Users user;
  private Featurestore fs;
  private TransformationFunctionController transformationFunctionController;
  private TransformationFunctionFacade transformationFunctionFacade;
  
  @Before
  public void setup() {
    project = Mockito.mock(Project.class);
    user = Mockito.mock(Users.class);
    fs = new Featurestore();
    transformationFunctionFacade = Mockito.mock(TransformationFunctionFacade.class);
    transformationFunctionController = new TransformationFunctionController(transformationFunctionFacade);
  }
  
  @Test
  public void testDelete_builtin_exception() {
    List<Optional<TransformationFunction>> fctList =
      FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_NAMES.stream()
        .map(name -> Optional.of(new TransformationFunction(name, 1)))
        .collect(Collectors.toList());
    
    Mockito.when(transformationFunctionFacade.findById(1)).thenReturn(fctList.get(0), fctList.get(1),
      fctList.get(2), fctList.get(3));
  
    Assert.assertThrows(FeaturestoreException.class, () -> transformationFunctionController.delete(project, fs, user,
      1));
    Assert.assertThrows(FeaturestoreException.class, () -> transformationFunctionController.delete(project, fs, user,
      1));
    Assert.assertThrows(FeaturestoreException.class, () -> transformationFunctionController.delete(project, fs, user,
      1));
    Assert.assertThrows(FeaturestoreException.class, () -> transformationFunctionController.delete(project, fs, user,
      1));
  }
  
}
